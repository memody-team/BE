package com.guru2.memody.controller;

import com.guru2.memody.config.CustomUserDetails;
import com.guru2.memody.dto.RegionUpdateDto;
import com.guru2.memody.repository.RegionRepository;
import com.guru2.memody.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mypage")
@Controller
public class MypageController {

    private final UserService userService;

    @PatchMapping("/region")
    public ResponseEntity<String> updateRegion(@AuthenticationPrincipal CustomUserDetails user,
                                               @RequestParam RegionUpdateDto region) {
        Long userId = user.getUserId();
        String response = userService.updateRegion(userId, region);

        return ResponseEntity.ok(response);
    }
}
