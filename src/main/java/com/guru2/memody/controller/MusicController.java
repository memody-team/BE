package com.guru2.memody.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.guru2.memody.config.CustomUserDetails;
import com.guru2.memody.dto.*;
import com.guru2.memody.service.MusicService;
import com.guru2.memody.service.RecommendService;
import com.guru2.memody.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/music")
@Controller
public class MusicController {

    private final MusicService musicService;
    private final RecommendService recommendService;
    private final RecordService recordService;

    @GetMapping("/search")
    public ResponseEntity<List<MusicListResponseDto>> searchTrack(@RequestParam String search) throws JsonProcessingException {
        List<MusicListResponseDto> response = musicService.searchTrack(search);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/record", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecordPinResponseDto> recordTrack(@AuthenticationPrincipal CustomUserDetails user,
                                                            @RequestPart("request") MusicRecordDto request,
                                                            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws JsonProcessingException {
        Long userId = user.getUserId();
        RecordPinResponseDto response = musicService.recordTrack(userId, request, images);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{musicId}/like")
    public ResponseEntity<LikeResponseDto> likeTrack(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long musicId){
        Long userId = user.getUserId();
        LikeResponseDto response = musicService.likeTrack(userId, musicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/artist")
    public ResponseEntity<List<ArtistResponseDto>> getArtist(@AuthenticationPrincipal CustomUserDetails user, @RequestParam String search) throws JsonProcessingException {
        List<ArtistResponseDto> response = musicService.getArtistList(search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{musicId}")
    public ResponseEntity<MusicDetailDto> getMusic(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long musicId){
        Long userId = user.getUserId();
        MusicDetailDto response = musicService.getMusicDetail(userId, musicId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recommend")
    public ResponseEntity<List<MusicListResponseDto>> getRecommend(@AuthenticationPrincipal CustomUserDetails user,
                                                                   @RequestBody RecommendRequestDto recommendRequestDto) throws JsonProcessingException {
        Long userId = user.getUserId();
        List<MusicListResponseDto> response = recommendService.getRecommendTrackByOnboarding(userId, recommendRequestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    public ResponseEntity<List<MusicListResponseDto>> getTodayRecommend(@AuthenticationPrincipal CustomUserDetails user) throws JsonProcessingException {
        Long userId = user.getUserId();
        List<MusicListResponseDto> response = recommendService.getRecommendTrackByUserInfo(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pin")
    public ResponseEntity<List<PinnedListDto>> getLatentPin(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getUserId();
        List<PinnedListDto> response = recordService.getLatentPin(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<AlbumDetailDto> getTodayAlbum(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long albumId) {
        Long userId = user.getUserId();
        AlbumDetailDto response = musicService.getAlbumDetail(userId, albumId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today/artist")
    public ResponseEntity<ArtistRecommendDto> getTodayArtist(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getUserId();
        ArtistRecommendDto response = musicService.getArtistRecommendDetail(userId);
        return ResponseEntity.ok(response);
    }
}
