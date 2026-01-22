package com.guru2.memody.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.guru2.memody.Exception.UserNotFoundException;
import com.guru2.memody.dto.MusicListResponseDto;
import com.guru2.memody.dto.RecommendRequestDto;
import com.guru2.memody.entity.*;
import com.guru2.memody.entity.Record;
import com.guru2.memody.repository.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {
    private final UserRepository userRepository;
    private final RecommendRepository recommendRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${GEMINI_API_KEY}") // properties에 넣은 값
    private String geminiApiKey;
    private final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
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

    private String createPromptWithUserInfo(User user){
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
                5. 추천 곡은 최소 5개, 최대 10개입니다.
                
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
                - 추천 곡은 최소 5개, 최대 10개입니다.
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



    public List<MusicListResponseDto> getRecommendByOnboarding(Long userId, RecommendRequestDto recommendRequestDto) throws JsonProcessingException {
        User user = userRepository.findUserByUserId(userId).orElseThrow(UserNotFoundException::new);
        recommendRepository.findByUser(user)
                .ifPresent(recommendRepository::delete);
        Recommend recommend = new Recommend();
        recommend.setUser(user);
        recommend.setMomentType(Moment.valueOf(recommendRequestDto.getMoment()));
        recommend.setMoodType(Mood.valueOf(recommendRequestDto.getMood()));
        recommend.setCreateTime(LocalDateTime.now());

        String prompt = createPromptWithOnboarding(user, recommendRequestDto);
        String response = callGemini(prompt);
        List<GeminiRecommendationDto> recommendedItems = parseGeminiResponse(response);

        List<MusicListResponseDto> trackList = new ArrayList<>();
        List<Music> musicList = new ArrayList<>();

        for(GeminiRecommendationDto item : recommendedItems) {
            try{
                String musicResponse = itunesService.searchTrackWithClearInfo(item.getTitle(), item.getArtist());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(musicResponse);

                JsonNode results = root.path("results");
                for (JsonNode trackNode : results) {
                    Music music = null;
                    music = musicRepository.findMusicByItunesId(trackNode.path("trackId").asLong()).orElse(
                            new Music()
                    );

                    music.setItunesId(trackNode.path("trackId").asLong());
                    music.setTitle(trackNode.path("trackName").asText());
                    music.setArtist(trackNode.path("artistName").asText());
                    music.setAppleMusicUrl(trackNode.path("trackViewUrl").asText());
                    music.setThumbnailUrl(trackNode.path("artworkUrl100").asText());
                    musicList.add(music);
                    musicRepository.save(music);

                    trackList.add(new MusicListResponseDto(
                            music.getMusicId(),
                            trackNode.path("trackName").asText(),
                            trackNode.path("artistName").asText(),
                            trackNode.path("artworkUrl100").asText()
                    ));
                }
            } catch (Exception e) {
                log.error("iTunes 검색 중 오류 발생: {} - {}", item.getTitle(), item.getArtist(), e);
            }

            recommend.setRecommendMusic(musicList);
            recommendRepository.save(recommend);
        }

        return trackList;
    }

    public List<MusicListResponseDto> getRecommendByUserInfo(Long userId){
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );

        Recommend recommend = recommendRepository.findByUser(user).orElseGet(
                Recommend::new
        );
        List<MusicListResponseDto> trackList = new ArrayList<>();
        if (recommend.getRecommendMusic() != null && recommend.getCreateTime().toLocalDate().isEqual(LocalDate.now())) {
            for (Music music : recommend.getRecommendMusic()) {
                MusicListResponseDto musicListResponseDto = new MusicListResponseDto(music.getMusicId(), music.getTitle(), music.getArtist(), music.getThumbnailUrl());
                trackList.add(musicListResponseDto);
            }
            return trackList;
        }

        recommend.setUser(user);
        recommend.setMomentType(null);
        recommend.setMoodType(null);
        recommend.setCreateTime(LocalDateTime.now());

        String prompt = createPromptWithUserInfo(user);
        String response = callGemini(prompt);
        List<GeminiRecommendationDto> recommendedItems = parseGeminiResponse(response);

        List<Music> musicList = new ArrayList<>();

        for(GeminiRecommendationDto item : recommendedItems) {
            try{
                String musicResponse = itunesService.searchTrackWithClearInfo(item.getTitle(), item.getArtist());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(musicResponse);

                JsonNode results = root.path("results");
                for (JsonNode trackNode : results) {
                    Music music = null;
                    music = musicRepository.findMusicByItunesId(trackNode.path("trackId").asLong()).orElse(
                            new Music()
                    );

                    music.setItunesId(trackNode.path("trackId").asLong());
                    music.setTitle(trackNode.path("trackName").asText());
                    music.setArtist(trackNode.path("artistName").asText());
                    music.setAppleMusicUrl(trackNode.path("trackViewUrl").asText());
                    music.setThumbnailUrl(trackNode.path("artworkUrl100").asText());
                    musicList.add(music);
                    musicRepository.save(music);

                    trackList.add(new MusicListResponseDto(
                            music.getMusicId(),
                            trackNode.path("trackName").asText(),
                            trackNode.path("artistName").asText(),
                            trackNode.path("artworkUrl100").asText()
                    ));
                }
            } catch (Exception e) {
                log.error("iTunes 검색 중 오류 발생: {} - {}", item.getTitle(), item.getArtist(), e);
            }

            recommend.setRecommendMusic(musicList);
            recommendRepository.save(recommend);
        }

        return trackList;
    }
}
