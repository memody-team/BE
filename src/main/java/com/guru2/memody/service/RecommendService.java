package com.guru2.memody.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.guru2.memody.Exception.UserNotFoundException;
import com.guru2.memody.dto.*;
import com.guru2.memody.entity.*;
import com.guru2.memody.entity.Record;
import com.guru2.memody.repository.*;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {
    private final UserRepository userRepository;
    private final RecommendMusicRepository recommendRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AlbumRepository albumRepository;
    private final RecommendAlbumRepository recommendAlbumRepository;
    private final RecommendArtistRepository recommendArtistRepository;
    private final ArtistRepository artistRepository;
    private final RecommendMusicRepository recommendMusicRepository;

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    // I/O 바운드 작업(네트워크 호출)을 위한 스레드 풀 설정 (동시 요청 20개까지 처리)
    private final ExecutorService apiExecutor = Executors.newFixedThreadPool(20);

    private final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/";
    private final GenrePreferenceRepository genrePreferenceRepository;
    private final ArtistPreferenceRepository artistPreferenceRepository;
    private final MusicLikeRepository musicLikeRepository;
    private final RecordRepository recordRepository;
    private final ITunesService itunesService;
    private final MusicRepository musicRepository;
    private final ObjectMapper objectMapper;

    @Getter @Setter @NoArgsConstructor
    private static class GeminiResponseDto {
        private int total;
        private List<GeminiRecommendationDto> recommendations;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class GeminiRecommendationDto {
        private String title;
        private String artist;
        private String country;
    }

    private List<String> userInfoToPrompt(User user){
        List<String> userInfo = new ArrayList<>();
        List<String> genreNames = genrePreferenceRepository.findAllByUser(user)
                .stream()
                .map(g -> g.getGenre().getGenreName())
                .toList();

        List<String> artistNames = artistPreferenceRepository.findAllByUser(user)
                .stream()
                .map(a -> a.getArtist().getArtistName())
                .toList();

        List<String> musicNames = musicLikeRepository
                .findAllByUserOrderByLikeDateDesc(user, Limit.of(3))
                .stream()
                .map(like -> like.getMusic().getTitle() + " - (trackId) " + like.getMusic().getItunesId())
                .toList();

        List<String> recordContents = recordRepository
                .findAllByUserOrderByRecordTimeDesc(user, Limit.of(3))
                .stream()
                .map(record ->
                        "노래 제목: " + record.getRecordMusic().getTitle()
                                + ", trackId: " + record.getRecordMusic().getItunesId()
                                + ", 기록한 이유: " + record.getText()
                )
                .toList();
        userInfo.add(genreNames.toString());
        userInfo.add(artistNames.toString());
        userInfo.add(musicNames.toString());
        userInfo.add(recordContents.toString());
        return userInfo;
    }

    private String createPromptWithUserInfo(User user, String purpose){
        List<String> userInfo = userInfoToPrompt(user);

        String prompt = """
                아래는 음악 기록 및 추천 서비스 유저의 음악 선호 정보입니다.
                좋아요 누른 음악과 기록한 음악은 각각 0개 이상 3개 이하의 가장 최근 데이터입니다. \n
                기록한 음악은 기록한 당시 사용자가 입력한 음악에 대한 감상이 포함되어 있습니다.
                
                추천 기준: 
                1. 유저가 '좋아요' 하거나 '기록한' 음악과 유사한 분위기를 최우선으로 찾으세요.
                2. 해당 데이터가 부족하면 선호 장르/가수 기반으로 확장하세요.
                3. 노래의 분위기(Vibe)가 유저의 취향과 맞아야 합니다.
                4. **추천 결과의 80퍼센트는 반드시 한국 음악(K-Pop, 인디 등)이어야 합니다.**
                
                [기존 유저 정보]
                좋아하는 음악 장르: %s
                좋아하는 분위기의 가수: %s
                좋아요 누른 음악: %s
                기록한 음악(감상 포함): %s
                
                응답 규칙(매우 중요):
                - 출력 형식은 다음과 같습니다.
                
                {
                  "total": 숫자,
                  "recommendations": [
                    {
                      "title": "%s",
                      "artist": "가수명",
                      "country": "KR or OTHER"
                    }
                  ]
                }
                
                규칙:
                - total은 recommendations 배열 길이와 동일해야 합니다.
                - %s 최소 5개, 최대 10개입니다.
                - 전체 추천 곡 중 80퍼센트 이상은 country가 "KR"이어야 합니다.
                - title과 artist는 iTunes Search API에서 검색 가능한 정확한 명칭을 사용하세요.
                - 다른 텍스트는 절대 포함하지 마세요.
            
                """.formatted(
                userInfo.get(0).isEmpty() ? "없음" : String.join(", ", userInfo.get(0)),
                userInfo.get(1).isEmpty() ? "없음" : String.join(", ", userInfo.get(1)),
                userInfo.get(2).isEmpty() ? "없음" : String.join(", ", userInfo.get(2)),
                userInfo.get(3).isEmpty() ? "없음" : String.join(" | ", userInfo.get(3)),
                purpose,
                purpose.equals("곡 제목") ? "추천 곡은" : "앨범 개수는"
        );

        log.info(prompt);

        return  prompt;
    }

    private String createArtistPromptWithUserInfo(User user){
        List<String> userInfo = userInfoToPrompt(user);

        String prompt = """
                아래는 음악 기록 및 추천 서비스 유저의 음악 선호 정보입니다.
                좋아요 누른 음악과 기록한 음악은 각각 0개 이상 3개 이하의 가장 최근 데이터입니다. \n
                기록한 음악은 기록한 당시 사용자가 입력한 음악에 대한 감상이 포함되어 있습니다.
                
                추천 기준: 
                1. 유저가 '좋아요' 하거나 '기록한' 음악과 유사한 분위기를 최우선으로 찾으세요.
                2. 해당 데이터가 부족하면 선호 장르/가수 기반으로 확장하세요.
                3. 노래의 분위기(Vibe)가 유저의 취향과 맞아야 합니다.
                4. **추천 결과는 모두 동일 가수의 곡이어야 합니다.**
                
                [기존 유저 정보]
                좋아하는 음악 장르: %s
                좋아하는 분위기의 가수: %s
                좋아요 누른 음악: %s
                기록한 음악(감상 포함): %s
                
                응답 규칙(매우 중요):
                - 출력 형식은 다음과 같습니다.
                
                {
                  "total": 숫자,
                  "recommendations": [
                    {
                      "title": "곡 제목",
                      "artist": "가수명",
                      "country": "KR or OTHER"
                    }
                  ]
                }
                
                규칙:
                - total은 recommendations 배열 길이와 동일해야 합니다.
                - 곡 개수는 최소 5개, 최대 10개입니다.
                - 전체 추천 곡 중 80퍼센트 이상은 country가 "KR"이어야 합니다.
                - title과 artist는 iTunes Search API에서 검색 가능한 정확한 명칭을 사용하세요.
                - 다른 텍스트는 절대 포함하지 마세요.
            
                """.formatted(
                userInfo.get(0).isEmpty() ? "없음" : String.join(", ", userInfo.get(0)),
                userInfo.get(1).isEmpty() ? "없음" : String.join(", ", userInfo.get(1)),
                userInfo.get(2).isEmpty() ? "없음" : String.join(", ", userInfo.get(2)),
                userInfo.get(3).isEmpty() ? "없음" : String.join(" | ", userInfo.get(3))
        );

        log.info(prompt);

        return  prompt;
    }

    private String createPromptWithOnboarding(User user, RecommendRequestDto recommend) {

        List<String> userInfo = userInfoToPrompt(user);

        String prompt = """
                아래는 음악 기록 및 추천 서비스 유저의 음악 선호 정보와, 현재 유저가 듣고 싶어하는 음악의 정보입니다.
                좋아요 누른 음악과 기록한 음악은 각각 0개 이상 3개 이하의 가장 최근 데이터입니다. \n
                기록한 음악은 기록한 당시 사용자가 입력한 음악에 대한 감상이 포함되어 있습니다.
                
                추천 기준: 
                1. 유저의 현재 정보를 기반으로 어울리는 곡을 최우선으로 찾으세요.
                2. 유저가 '좋아요' 하거나 '기록한' 음악과 유사한 분위기를 기반으로 확장하세요.
                3. 해당 데이터가 부족하면 선호 장르/가수 기반으로 확장하세요.
                4. 노래의 분위기(Vibe)가 유저의 취향과 맞아야 합니다.
                5. **추천 결과의 80퍼센트는 반드시 한국 음악(K-Pop, 인디 등)이어야 합니다.**
                6. 추천 곡은 최소 5개, 최대 10개입니다.
                
                [기존 유저 정보]
                좋아하는 음악 장르: %s
                좋아하는 분위기의 가수: %s
                좋아요 누른 음악: %s
                기록한 음악(감상 포함): %s
                
                [유저의 현재 정보]
                음악을 듣고 싶은 상황: %s
                기분: %s
                듣고 싶은 아티스트: %s
                듣고 싶은 음악 장르: %s
                
                응답 규칙(매우 중요):
                - 출력 형식은 다음과 같습니다.
                
                {
                  "total": 숫자,
                  "recommendations": [
                    {
                      "title": "곡 제목",
                      "artist": "가수명",
                      "country": "KR or OTHER"
                    }
                  ]
                }
                
                규칙:
                - total은 recommendations 배열 길이와 동일해야 합니다.
                - 추천 곡은 최소 5개, 최대 10개입니다.
                - 전체 추천 곡 중 80퍼센트 이상은 country가 "KR"이어야 합니다.
                - title과 artist는 iTunes Search API에서 검색 가능한 정확한 명칭을 사용하세요.
                - 다른 텍스트는 절대 포함하지 마세요.
            
                """.formatted(
                userInfo.get(0).isEmpty() ? "없음" : String.join(", ", userInfo.get(0)),
                userInfo.get(1).isEmpty() ? "없음" : String.join(", ", userInfo.get(1)),
                userInfo.get(2).isEmpty() ? "없음" : String.join(", ", userInfo.get(2)),
                userInfo.get(3).isEmpty() ? "없음" : String.join(" | ", userInfo.get(3)),
                recommend.getMoment(),
                recommend.getMood(),
                recommend.getArtistNames(),
                recommend.getGenreNames()
        );

        log.info(prompt);

        return  prompt;
    }

    private String callGemini(String prompt){
        Client client = Client.builder().apiKey(geminiApiKey).build();
        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-3-flash-preview",
                        prompt,
                        null);
        log.info(response.toString());

        return response.text();
    }

    private List<GeminiRecommendationDto> parseGeminiResponse(String geminiText) {
        try {
            String cleanedText = geminiText.replaceAll("```json", "").replaceAll("```", "").trim();

            GeminiResponseDto responseDto = objectMapper.readValue(cleanedText, GeminiResponseDto.class);

            if (responseDto.getRecommendations() == null || responseDto.getRecommendations().isEmpty()) {
                throw new IllegalStateException("추천 결과가 비어있습니다.");
            }

            return responseDto.getRecommendations();

        } catch (JsonProcessingException e) {
            log.error("Gemini 응답 파싱 실패: {}", geminiText, e);
            throw new IllegalArgumentException("AI 응답 형식이 올바르지 않습니다.");
        }
    }



    private Music fetchMusicLogic(String title, String artistName) {
        try {
            String musicResponse = itunesService.searchTrackWithClearInfo(title, artistName);
            JsonNode root = objectMapper.readTree(musicResponse);
            JsonNode results = root.path("results");

            if (results.isEmpty()) return null;

            JsonNode trackNode = results.get(0);
            Long trackId = trackNode.path("trackId").asLong();

            // DB 조회 혹은 생성 (동시성 이슈 방지를 위해 여기서 save)
            Music music = musicRepository.findMusicByItunesId(trackId).orElse(new Music());

            synchronized (this) { // save 시 중복 방지를 위한 간단한 동기화 (선택 사항)
                music.setItunesId(trackId);
                music.setTitle(trackNode.path("trackName").asText());
                music.setArtist(trackNode.path("artistName").asText());
                music.setAppleMusicUrl(trackNode.path("trackViewUrl").asText());
                music.setThumbnailUrl(trackNode.path("artworkUrl100").asText());
                musicRepository.save(music);
            }
            return music;
        } catch (Exception e) {
            log.error("iTunes 곡 검색 실패: {} - {}", title, artistName, e);
            return null;
        }
    }

    /**
     * 앨범 및 수록곡 검색 로직 (비동기 호출용)
     */
    private Album fetchAlbumLogic(String title, String artistName) {
        try {
            String albumResponse = itunesService.searchAlbumWithClearInfo(title, artistName);
            JsonNode root = objectMapper.readTree(albumResponse);
            JsonNode results = root.path("results");

            if (results.isEmpty()) return null;

            JsonNode albumNode = results.get(0);
            Long albumItunesId = albumNode.path("collectionId").asLong();

            Album album = albumRepository.findByItunesId(albumItunesId).orElse(new Album());
            album.setItunesId(albumItunesId);
            album.setTitle(albumNode.path("collectionName").asText());
            album.setArtist(albumNode.path("artistName").asText());
            album.setThumbnailUrl(albumNode.path("artworkUrl100").asText());
            albumRepository.save(album);

            // 앨범 수록곡 검색
            String musicResponse = itunesService.lookupTracksByAlbumId(albumItunesId);
            JsonNode trackResults = objectMapper.readTree(musicResponse).path("results");
            List<Music> currentAlbumTracks = new ArrayList<>();

            for (JsonNode trackNode : trackResults) {
                if ("track".equals(trackNode.path("wrapperType").asText())) {
                    Long trackId = trackNode.path("trackId").asLong();
                    Music music = musicRepository.findMusicByItunesId(trackId).orElse(new Music());
                    music.setItunesId(trackId);
                    music.setTitle(trackNode.path("trackName").asText());
                    music.setArtist(trackNode.path("artistName").asText());
                    music.setAppleMusicUrl(trackNode.path("trackViewUrl").asText());
                    music.setThumbnailUrl(trackNode.path("artworkUrl100").asText());
                    musicRepository.save(music);
                    currentAlbumTracks.add(music);
                }
            }
            album.setIncludedSongs(currentAlbumTracks);
            return album;
        } catch (Exception e) {
            log.error("iTunes 앨범 검색 실패: {} - {}", title, artistName, e);
            return null;
        }
    }

    // =================================================================================
    // [서비스 메서드] CompletableFuture 적용
    // =================================================================================

    @Transactional
    public List<MusicListResponseDto> getRecommendTrackByOnboarding(Long userId, RecommendRequestDto recommendRequestDto) throws JsonProcessingException {
        User user = userRepository.findUserByUserId(userId).orElseThrow(UserNotFoundException::new);

        // 기존 추천 내역 삭제 및 초기화
        recommendRepository.findByUser(user).ifPresent(recommendRepository::delete);

        RecommendMusic recommend = new RecommendMusic();
        recommend.setUser(user);
        recommend.setMomentType(Moment.valueOf(recommendRequestDto.getMoment()));
        recommend.setMoodType(Mood.valueOf(recommendRequestDto.getMood()));
        recommend.setCreateTime(LocalDateTime.now());

        // 1. Gemini 호출
        // (주의: createPromptWithOnboarding 메서드는 기존 코드에 있는 내용을 사용해야 합니다)
        String prompt = createPromptWithOnboarding(user, recommendRequestDto);
        String response = callGemini(prompt);
        List<GeminiRecommendationDto> recommendedItems = parseGeminiResponse(response);

        // 2. 병렬 처리 (기존 for문 대체)
        List<CompletableFuture<Music>> futures = recommendedItems.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> fetchMusicLogic(item.getTitle(), item.getArtist()), apiExecutor))
                .toList();

        List<Music> musicList = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        recommend.setRecommendMusics(musicList);
        recommendRepository.save(recommend);

        return musicList.stream()
                .map(m -> new MusicListResponseDto(m.getMusicId(), m.getTitle(), m.getArtist(), m.getThumbnailUrl()))
                .toList();
    }

    @Transactional
    public List<MusicListResponseDto> getRecommendTrackByUserInfo(Long userId){
        User user = userRepository.findUserByUserId(userId).orElseThrow(UserNotFoundException::new);
        RecommendMusic recommend = recommendRepository.findByUser(user).orElseGet(RecommendMusic::new);

        // 오늘 날짜 캐시 확인
        if (recommend.getRecommendMusics() != null && !recommend.getRecommendMusics().isEmpty()
                && recommend.getCreateTime().toLocalDate().isEqual(LocalDate.now())) {
            return recommend.getRecommendMusics().stream()
                    .map(m -> new MusicListResponseDto(m.getMusicId(), m.getTitle(), m.getArtist(), m.getThumbnailUrl()))
                    .toList();
        }

        recommend.setUser(user);
        recommend.setCreateTime(LocalDateTime.now());

        // 1. Gemini 호출
        String prompt = createPromptWithUserInfo(user, "곡 제목");
        String response = callGemini(prompt);
        List<GeminiRecommendationDto> recommendedItems = parseGeminiResponse(response);

        // 2. 병렬 처리
        List<CompletableFuture<Music>> futures = recommendedItems.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> fetchMusicLogic(item.getTitle(), item.getArtist()), apiExecutor))
                .toList();

        List<Music> musicList = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        recommend.setRecommendMusics(musicList);
        recommendRepository.save(recommend);

        return musicList.stream()
                .map(m -> new MusicListResponseDto(m.getMusicId(), m.getTitle(), m.getArtist(), m.getThumbnailUrl()))
                .toList();
    }

    @Transactional
    public RecommendAlbum getRecommendAlbumByUserInfo(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(UserNotFoundException::new);
        RecommendAlbum recommendAlbum = recommendAlbumRepository.findByUser(user).orElseGet(RecommendAlbum::new);
        recommendAlbum.setUser(user);

        if (recommendAlbum.getAlbums() != null && !recommendAlbum.getAlbums().isEmpty()
                && recommendAlbum.getCreateTime().toLocalDate().isEqual(LocalDate.now())) {
            return recommendAlbum;
        }

        // 1. Gemini 호출
        String prompt = createPromptWithUserInfo(user, "앨범 명");
        String response = callGemini(prompt);
        List<GeminiRecommendationDto> recommendedItems = parseGeminiResponse(response);

        // 2. 병렬 처리 (앨범 검색 + 수록곡 검색을 묶어서 병렬화)
        List<CompletableFuture<Album>> futures = recommendedItems.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> fetchAlbumLogic(item.getTitle(), item.getArtist()), apiExecutor))
                .toList();

        List<Album> albumList = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        recommendAlbum.setAlbums(albumList);
        recommendAlbum.setCreateTime(LocalDateTime.now());
        recommendAlbumRepository.save(recommendAlbum);

        return recommendAlbum;
    }

    @Transactional
    public RecommendArtist getRecommendArtistByUserInfo(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(UserNotFoundException::new);
        RecommendArtist recommendArtist = recommendArtistRepository.findByUser(user)
                .orElseGet(() -> {
                    RecommendArtist newRa = new RecommendArtist();
                    newRa.setUser(user);
                    return newRa;
                });

        if (recommendArtist.getArtist() != null
                && recommendArtist.getCreateTime() != null
                && recommendArtist.getCreateTime().toLocalDate().isEqual(LocalDate.now())) {
            return recommendArtist;
        }

        // 1. Gemini 호출
        String prompt = createArtistPromptWithUserInfo(user);
        String response = callGemini(prompt);
        List<GeminiRecommendationDto> recommendedItems = parseGeminiResponse(response);

        if (recommendedItems.isEmpty()) throw new RuntimeException("AI 추천 결과가 없습니다.");

        // 2. 아티스트 정보 저장 (단건이므로 동기 처리)
        String artistName = recommendedItems.get(0).getArtist();
        Artist artist = null;
        try {
            String artistJson = itunesService.searchArtistWithItunes(artistName);
            JsonNode artistResults = objectMapper.readTree(artistJson).path("results");
            if (artistResults.size() > 0) {
                JsonNode artistNode = artistResults.get(0);
                Long itunesArtistId = artistNode.path("artistId").asLong();
                artist = artistRepository.findByItunesArtistId(itunesArtistId).orElse(new Artist());
                artist.setItunesArtistId(itunesArtistId);
                artist.setArtistName(artistNode.path("artistName").asText());
                artistRepository.save(artist);
            }
        } catch (Exception e) {
            log.error("가수 정보 처리 중 오류: {}", artistName, e);
        }
        recommendArtist.setArtist(artist); // 아티스트 설정 추가

        // 3. 곡 목록 병렬 처리
        List<CompletableFuture<Music>> futures = recommendedItems.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> fetchMusicLogic(item.getTitle(), item.getArtist()), apiExecutor))
                .toList();

        List<Music> musicList = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        recommendArtist.setRecommendedItems(musicList);
        recommendArtist.setCreateTime(LocalDateTime.now());
        recommendArtistRepository.save(recommendArtist);

        return recommendArtist;
    }

    @Transactional
    public HomeDto getHome(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(UserNotFoundException::new);
        RecommendMusic recommendMusic = recommendMusicRepository.findByUser(user).orElse(null);

        List<Record> records = recordRepository.findAllByUserOrderByRecordTimeDesc(user);
        RecommendAlbum recommendAlbum = getRecommendAlbumByUserInfo(userId);
        RecommendArtist recommendArtist = getRecommendArtistByUserInfo(userId);

        List<MusicCardDto> todays = new ArrayList<>();
        List<MusicCardWithRegionDto> recorded = new ArrayList<>();
        List<MusicCardDto> albums = new ArrayList<>();
        List<MusicCardDto> artists = new ArrayList<>();

        List<Music> todayList;
        try{
            todayList = recommendMusic.getRecommendMusics();
        } catch (Exception e){
            todayList = null;
        }
        List<Album> albumList = recommendAlbum.getAlbums();
        List<Music> artistList = recommendArtist.getRecommendedItems();


        if (todayList != null && !todayList.isEmpty()
                && recommendMusic.getCreateTime().toLocalDate().isEqual(LocalDate.now())) {

            for (Music music : todayList) {
                MusicCardDto dto = new MusicCardDto();
                dto.setTitle(music.getTitle());
                dto.setArtist(music.getArtist());
                dto.setThumbnailUrl(music.getThumbnailUrl());
                dto.setMusicId(music.getMusicId());
                todays.add(dto);
            }
        }

        int count = 0;
        for(Record record : records) {
            if(count > 12){
                break;
            }
            MusicCardWithRegionDto musicCardWithRegionDto = new MusicCardWithRegionDto();
            musicCardWithRegionDto.setMusicId(record.getRecordMusic().getMusicId());
            musicCardWithRegionDto.setTitle(record.getRecordMusic().getTitle());
            musicCardWithRegionDto.setArtist(record.getRecordMusic().getArtist());
            String[] parts = record.getRecordLocation().split(" ");
            musicCardWithRegionDto.setRegion(parts[1]);
            musicCardWithRegionDto.setThumbnailUrl(record.getRecordMusic().getThumbnailUrl());
            recorded.add(musicCardWithRegionDto);
            count++;
        }


        for(Album album : albumList) {
            MusicCardDto musicCardDto = new MusicCardDto();
            musicCardDto.setArtist(album.getArtist());
            musicCardDto.setTitle(album.getTitle());
            musicCardDto.setMusicId(album.getAlbumId());
            musicCardDto.setThumbnailUrl(album.getThumbnailUrl());
            albums.add(musicCardDto);
        }

        for(Music artist : artistList) {
            MusicCardDto musicCardDto = new MusicCardDto();
            musicCardDto.setMusicId(artist.getMusicId());
            musicCardDto.setArtist(artist.getArtist());
            musicCardDto.setTitle(artist.getTitle());
            musicCardDto.setThumbnailUrl(artist.getThumbnailUrl());
            artists.add(musicCardDto);
        }
        HomeDto homeDto = new HomeDto();
        homeDto.setTodayRecommends(todays);
        homeDto.setSavedMusic(recorded);
        homeDto.setAlbumRecommends(albums);
        homeDto.setArtistName(artists.get(artists.size() - 1).getArtist());
        homeDto.setArtistRecommends(artists);

        return homeDto;

    }
}
