package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityResponseDto {
    private Long recordId;
    private String userName;
    private String musicName;
    private String artistName;
    private String content;
    private String regionName;
    private String thumbnailUrl;
    private String spotifyUrl;
    private String appleMusicUrl;
    private String recordDate;
    private Integer likeCount;
    private Boolean isLiked;

    private Long userId;

}
