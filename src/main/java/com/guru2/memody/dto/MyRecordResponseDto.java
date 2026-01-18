package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MyRecordResponseDto {
    private String title;
    private String artist;
    private String content;
    private String recordDate;
    private String thumbnail;
    private String spotifyUrl;
    private String iTunesUrl;
    private String regionName;
}
