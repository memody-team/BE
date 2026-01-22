package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HomeDto {
    private List<MusicCardDto> todayRecommends;
    private List<MusicCardWithRegionDto> savedMusic;
    private List<MusicCardDto> albumRecommends;
    private String artistName;
    private List<MusicCardDto> artistRecommends;
}
