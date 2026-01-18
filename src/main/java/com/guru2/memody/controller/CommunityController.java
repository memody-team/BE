package com.guru2.memody.controller;

import com.guru2.memody.config.CustomUserDetails;
import com.guru2.memody.dto.CommunityResponseDto;
import com.guru2.memody.dto.LikeResponseDto;
import com.guru2.memody.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/community")
@Controller
public class CommunityController {
    private final RecordService recordService;

    @GetMapping
    public ResponseEntity<List<CommunityResponseDto>> getCommunity(@AuthenticationPrincipal CustomUserDetails user) {
        List<CommunityResponseDto> response = recordService.getCommunity();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{recordId}/like")
    public ResponseEntity<LikeResponseDto> likeRecord(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long recordId) {
        Long userId = user.getUserId();
        LikeResponseDto likeResponseDto = recordService.likeRecord(userId, recordId);
        return ResponseEntity.ok(likeResponseDto);
    }
}
