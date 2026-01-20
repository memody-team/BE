package com.guru2.memody.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru2.memody.Exception.UserNotFoundException;
import com.guru2.memody.dto.MusicListResponseDto;
import com.guru2.memody.dto.RecommendRequestDto;
import com.guru2.memody.entity.*;
import com.guru2.memody.entity.Record;
import com.guru2.memody.repository.*;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendService {
    private final UserRepository userRepository;
    private final RecommendRepository recommendRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
    private final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private final GenrePreferenceRepository genrePreferenceRepository;
    private final ArtistPreferenceRepository artistPreferenceRepository;
    private final MusicLikeRepository musicLikeRepository;
    private final RecordRepository recordRepository;

    private String userInfoToPrompt(User user) {
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
                .map(like -> like.getMusic().getTitle())
                .toList();

        List<String> recordContents = recordRepository
                .findAllByUserOrderByRecordTimeDesc(user, Limit.of(3))
                .stream()
                .map(record ->
                        "노래 제목: " + record.getRecordMusic().getTitle()
                                + ", 기록한 이유: " + record.getText()
                )
                .toList();

        String prompt = """
                아래는 음악 기록 및 추천 서비스 유저의 음악 선호 정보입니다.
                좋아요 누른 음악과 기록한 음악은 각각 0개 이상 3개 이하의 가장 최근 데이터입니다. \n
                기록한 음악은 기록한 당시 사용자가 입력한 음악에 대한 감상이 포함되어 있습니다.
                
                추천 기준: 
                1. 좋아요 누른 음악과 기록한 음악이 하나라도 있다면 이를 최우선으로 기반하여 추천합니다. 
                2. 둘 다 없다면 좋아하는 음악 장르, 가수를 기반으로 노래를 추천합니다. \n\n
                
                좋아하는 음악 장르: %s
                좋아하는 분위기의 가수: %s
                좋아요 누른 음악: %s
                기록한 음악(감상 포함): %s
                
                응답 규칙(매우 중요):
                - iTunes Search API에서 사용되는 trackId(Long 숫자)만 사용하세요.
                - 추천 곡은 최소 5개, 최대 20개입니다.
                - 출력 형식은 다음과 같습니다.
                
                [곡 개수를 표현하는 두 자리 숫자],[trackId],[trackId],...,[trackId]
                
                - 곡 개수는 두 자리 숫자로 표시합니다.(예, 5 -> 05, 18 -> 18)
                - 쉼표(,) 외의 다른 구분자는 사용하지 마세요. 공백문자( )도 사용하지 마세요. 
                - trackId 리스트 제외 다른 텍스트, 설명, 줄바꿈을 추가하지 마세요.
                - 응답 예시 : 
                04,1440833092,1440833091,1440833094,14408330926
                """.formatted(
                genreNames.isEmpty() ? "없음" : String.join(", ", genreNames),
                artistNames.isEmpty() ? "없음" : String.join(", ", artistNames),
                musicNames.isEmpty() ? "없음" : String.join(", ", musicNames),
                recordContents.isEmpty() ? "없음" : String.join(" | ", recordContents)
        );;

        return  prompt;
    }

    private String callGemini(String prompt){
        String uri = UriComponentsBuilder.fromUriString(GEMINI_BASE_URL)
                .queryParam("key", GEMINI_API_KEY)
                .build().toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                    "contents" : [
                    {
                        "parts" : [
                        { "text" : "%s" }
                        ]
                    }
                    ]
                }
                """.formatted(prompt);

        HttpEntity<String> request = new HttpEntity<String>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

        return response.getBody();
    }

    private String extractGeminiText(String response) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        return root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText()
                .trim();
    }

    private List<Long> parseTrackIds(String geminiText) {

        // 혹시 모를 공백 제거
        geminiText = geminiText.replaceAll("\\s", "");

        String[] tokens = geminiText.split(",");

        if (tokens.length < 2) {
            throw new IllegalArgumentException("Gemini 응답 형식 오류");
        }

        // 첫 번째 값 = 곡 개수
        int declaredCount;
        try {
            declaredCount = Integer.parseInt(tokens[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("곡 개수 파싱 실패");
        }

        List<Long> trackIds = new ArrayList<>();

        for (int i = 1; i < tokens.length; i++) {
            try {
                trackIds.add(Long.parseLong(tokens[i]));
            } catch (NumberFormatException e) {
                // 숫자 아닌 값은 무시
            }
        }

        // 곡 개수 검증
        if (trackIds.size() < 5 || trackIds.size() > 20) {
            throw new IllegalStateException("추천 곡 개수 범위 오류");
        }

        return trackIds;
    }



    public List<MusicListResponseDto> getRecommendByOnboarding(Long userId, RecommendRequestDto recommendRequestDto) throws JsonProcessingException {
        User user = userRepository.findUserByUserId(userId).orElseThrow(UserNotFoundException::new);
        if(recommendRepository.findByUser(user).isPresent()) {
            recommendRepository.delete(recommendRepository.findByUser(user).get());
        }
        Recommend recommend = new Recommend();
        recommend.setUser(user);
        recommend.setMomentType(Moment.valueOf(recommendRequestDto.getMoment()));
        recommend.setMoodType(Mood.valueOf(recommendRequestDto.getMood()));
        recommend.setCreateTime(LocalDateTime.now());

        String prompt = userInfoToPrompt(user);
        String response = callGemini(prompt);
        String text = extractGeminiText(response);
        List<Long> trackIds = parseTrackIds(text);

        for(Long trackId : trackIds) {

        }

    }
}
