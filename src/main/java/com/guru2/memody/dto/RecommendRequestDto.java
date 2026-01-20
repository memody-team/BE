package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class RecommendRequestDto {
    private String moment;
    private String mood;
    private List<Long> artistIds;
    private List<String> genreNames;
}
