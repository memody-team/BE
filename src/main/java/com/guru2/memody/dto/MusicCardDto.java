package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MusicCardDto {
    private Long musicId;
    private String thumbnailUrl;
    private String title;
    private String artist;
}
