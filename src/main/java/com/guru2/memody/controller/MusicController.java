package com.guru2.memody.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.guru2.memody.config.CustomUserDetails;
import com.guru2.memody.dto.LikeResponseDto;
import com.guru2.memody.dto.MusicRecordDto;
import com.guru2.memody.dto.RecordPinResponseDto;
import com.guru2.memody.dto.MusicListResponseDto;
import com.guru2.memody.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/music")
@Controller
public class MusicController {

    private final MusicService musicService;

    @GetMapping("/search")
    public ResponseEntity<List<MusicListResponseDto>> searchTrack(@RequestParam String search) throws JsonProcessingException {
        List<MusicListResponseDto> response = musicService.searchTrack(search);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/record")
    public ResponseEntity<RecordPinResponseDto> recordTrack(@AuthenticationPrincipal CustomUserDetails user,
                                                            @RequestBody MusicRecordDto musicRecordDto) throws JsonProcessingException {
        Long userId = user.getUserId();
        RecordPinResponseDto response = musicService.recordTrack(userId, musicRecordDto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{musicId}/like")
    public ResponseEntity<LikeResponseDto> likeTrack(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long musicId){
        Long userId = user.getUserId();
        LikeResponseDto response = musicService.likeTrack(userId, musicId);
        return ResponseEntity.ok(response);
    }
}
