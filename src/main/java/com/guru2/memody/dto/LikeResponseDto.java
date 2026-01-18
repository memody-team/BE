package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LikeResponseDto {
    private boolean like;
    private Integer likeCount;
    private Enum likeType;
}
