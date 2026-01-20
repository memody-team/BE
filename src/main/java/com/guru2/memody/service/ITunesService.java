package com.guru2.memody.service;

import com.guru2.memody.Exception.UserNotFoundException;
import com.guru2.memody.dto.MusicListResponseDto;
import com.guru2.memody.entity.MusicLike;
import com.guru2.memody.entity.User;
import com.guru2.memody.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ITunesService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String APPLE_BASE_URL = "https://itunes.apple.com/search";
    private static final String APPLE_IMAGE_URL = "https://itunes.apple.com/lookup";

    private final UserRepository userRepository;
    private final MusicLikeRepository musicLikeRepository;

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

    public String searchArtistWithItunes(String search) {
        String uri = UriComponentsBuilder.fromUriString(APPLE_BASE_URL)
                .queryParam("term", search)
                .queryParam("country", "KR")
                .queryParam("media", "music")
                .queryParam("lang", "ko_kr")
                .queryParam("entity", "musicArtist")
                .queryParam("limit", 9)
                .toUriString();
        return restTemplate.getForObject(uri, String.class);
    }

    public String searchArtistImageWithItunes(Long id) {
        String uri = UriComponentsBuilder.fromUriString(APPLE_IMAGE_URL)
                .queryParam("id", id)
                .queryParam("country", "KR")
                .queryParam("entity", "song")
                .queryParam("attribute", "artistTerm")
                .queryParam("limit", 1)
                .toUriString();

        return restTemplate.getForObject(uri, String.class);
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
