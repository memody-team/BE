package com.guru2.memody.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@AllArgsConstructor
public class CommunityGroupDto {
    private Long userId;
    private String nickname;
//    private String userProfileUrl;

    private List<CommunityResponseDto> records;
}