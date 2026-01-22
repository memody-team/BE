package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ArtistRecommendDto {
    private String artistName;
    private List<MusicListResponseDto> musicList;
}
