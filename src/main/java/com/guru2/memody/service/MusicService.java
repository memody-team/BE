package com.guru2.memody.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru2.memody.Exception.UserNotFoundException;
import com.guru2.memody.dto.*;
import com.guru2.memody.entity.*;
import com.guru2.memody.entity.Record;
import com.guru2.memody.extractData.VWorldClient;
import com.guru2.memody.repository.MusicLikeRepository;
import com.guru2.memody.repository.MusicRepository;
import com.guru2.memody.repository.RecordRepository;
import com.guru2.memody.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final UserRepository userRepository;
    private final RecordRepository recordRepository;
    private final VWorldClient vWorldClient;
    private final MusicLikeRepository musicLikeRepository;

    @Value("${lastfm.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String APPLE_BASE_URL = "https://itunes.apple.com/search";
    private static final String LAST_FM_PATH = "https://ws.audioscrobbler.com/2.0/";
    private static final String DEEZER_BASE_URL = "https://api.deezer.com/search";


    private final MusicRepository musicRepository;
    public String searchTrackWithItunes(String search) {
        String uri = UriComponentsBuilder.fromUriString(APPLE_BASE_URL)
                .queryParam("term", search)
                .queryParam("country", "KR")
                .queryParam("media", "music")
                .queryParam("lang", "ko_kr")
                .queryParam("entity", "song")
                .queryParam("limit", 20)
                .toUriString();
        return restTemplate.getForObject(uri, String.class);
    }

    public String searchTrackWithDeezer(String search) {
        String uri = UriComponentsBuilder.fromUriString(DEEZER_BASE_URL)
                .queryParam("q", search)
                .toUriString();
        return restTemplate.getForObject(uri, String.class);
    }

    public String searchTrackWithLastfm(String search) {
        String uri = UriComponentsBuilder.fromUriString(LAST_FM_PATH)
                .queryParam("method", "track.search")
                .queryParam("track", search)
                .queryParam("api_key", apiKey)
                .queryParam("format", "json")
                .toUriString();
        return restTemplate.getForObject(uri, String.class);
    }

    @Transactional
    public List<MusicListResponseDto> searchTrack(String search) throws JsonProcessingException {
        String response = searchTrackWithItunes(search);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        JsonNode results = root.path("results");

        List<MusicListResponseDto> trackList = new ArrayList<>();

        for (JsonNode trackNode : results) {

            Music music = null;
            music = musicRepository.findMusicByAppleMusicUrl(trackNode.path("trackViewUrl").asText()).orElse(
                    new Music()
            );
            music.setTitle(trackNode.path("trackName").asText());
            music.setArtist(trackNode.path("artistName").asText());
            music.setAppleMusicUrl(trackNode.path("trackViewUrl").asText());
            music.setThumbnailUrl(trackNode.path("artworkUrl100").asText());

            musicRepository.save(music);

            trackList.add(new MusicListResponseDto(
                    music.getMusicId(),
                    trackNode.path("trackName").asText(),
                    trackNode.path("artistName").asText(),
                    trackNode.path("artworkUrl100").asText()
            ));

        }

        return trackList;
    }

    @Transactional
    public RecordPinResponseDto recordTrack(Long userId, MusicRecordDto musicRecordDto) throws JsonProcessingException {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                () -> new RuntimeException("User not found")
        );
        Music music = musicRepository.findMusicByMusicId(musicRecordDto.getMusicId());
        String spotifyUrl = getSpotifyLinkFromItunes(music.getAppleMusicUrl());
        music.setSpotifyUrl(spotifyUrl);
        musicRepository.save(music);

        RegionFullName regionFullName = vWorldClient.setRecordRegion(musicRecordDto.getLongitude(), musicRecordDto.getLatitude());
        Record record = new Record();
        record.setRecordMusic(music);
        record.setText(musicRecordDto.getContent());
        record.setLatitude(musicRecordDto.getLatitude());
        record.setLongitude(musicRecordDto.getLongitude());
        record.setRecordLocation(regionFullName.getName());
        record.setRecordTime(LocalDateTime.now());
        record.setUser(user);
        recordRepository.save(record);

        RecordPinResponseDto recordPinResponseDto = new RecordPinResponseDto();
        recordPinResponseDto.setRecordId(record.getRecordId());
        recordPinResponseDto.setThumbnailUrl(music.getThumbnailUrl());
        recordPinResponseDto.setLatitude(record.getLatitude());
        recordPinResponseDto.setLongitude(record.getLongitude());
        return recordPinResponseDto;

    }

    public String getSpotifyLinkFromItunes(String itunesUrl) throws JsonProcessingException {
        String encodedUrl = UriUtils.encode(itunesUrl, StandardCharsets.UTF_8);

        String uri = "https://api.song.link/v1-alpha.1/links?url=" + encodedUrl;

        String response = restTemplate.getForObject(uri, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        return root
                .path("linksByPlatform")
                .path("spotify")
                .path("url")
                .asText();
    }

    public LikeResponseDto likeTrack(Long userId, Long musicId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        LikeResponseDto likeResponseDto = new LikeResponseDto();

        Music music = musicRepository.findMusicByMusicId(musicId);
        musicLikeRepository.findByUserAndMusic(user, music)
                        .ifPresentOrElse(existML -> {
                            musicLikeRepository.delete(existML);
                            likeResponseDto.setLikeType(LikeType.MUSIC);
                            likeResponseDto.setLike(false);
                            likeResponseDto.setLikeCount(0);
                                },
                                ()  -> {
                                    MusicLike newMusicLike = new MusicLike();
                                    newMusicLike.setUser(user);
                                    newMusicLike.setMusic(music);
                                    newMusicLike.setLikeDate(LocalDateTime.now());
                                    musicLikeRepository.save(newMusicLike);
                                    likeResponseDto.setLikeCount(1);
                                    likeResponseDto.setLikeType(LikeType.MUSIC);
                                    likeResponseDto.setLike(true);
                        });


        return likeResponseDto;
    }

    public List<MusicListResponseDto> getLikedMusicList(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        List<MusicLike> musicLikes = musicLikeRepository.findAllByUser(user);
        List<MusicListResponseDto> musicListResponseDtos = new ArrayList<>();
        for (MusicLike musicLike : musicLikes) {
            MusicListResponseDto musicListResponseDto = new MusicListResponseDto(musicLike.getMusic().getMusicId(), musicLike.getMusic().getTitle(),
                    musicLike.getMusic().getArtist(), musicLike.getMusic().getThumbnailUrl());
            musicListResponseDtos.add(musicListResponseDto);
        }
        return musicListResponseDtos;
    }



}
