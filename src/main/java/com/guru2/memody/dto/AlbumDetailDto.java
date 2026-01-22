package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter @Getter
public class AlbumDetailDto {
    private String albumName;
    private String artistName;
    private String thumbnailUrl;
    private List<MusicListResponseDto> musicList;
}
