package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class RecordDetailDto {
    private String title;
    private String artist;
    private String content;
    private String recordDate;
    private String thumbnail;
    private List<String> imageUrls;
    private String spotifyUrl;
    private String iTunesUrl;
    private Boolean liked;
    private Integer likeCount;
}
