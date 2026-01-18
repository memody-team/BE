package com.guru2.memody.controller;

import com.guru2.memody.config.CustomUserDetails;
import com.guru2.memody.dto.MusicListResponseDto;
import com.guru2.memody.dto.MyRecordResponseDto;
import com.guru2.memody.dto.RecordDetailDto;
import com.guru2.memody.repository.RecordRepository;
import com.guru2.memody.service.MusicService;
import com.guru2.memody.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/library")
@Controller
public class LibraryController {
    private final MusicService musicService;
    private final RecordService recordService;

    @GetMapping("/like")
    public ResponseEntity<List<MusicListResponseDto>> getLikedMusicList(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getUserId();
        List<MusicListResponseDto> response = musicService.getLikedMusicList(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pin")
    public ResponseEntity<List<MyRecordResponseDto>> getMyRecordList(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getUserId();
        List<MyRecordResponseDto> response = recordService.getMyRecordList(userId);
        return ResponseEntity.ok(response);
    }

}
