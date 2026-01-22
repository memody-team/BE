package com.guru2.memody.controller;

import com.guru2.memody.config.CustomUserDetails;
import com.guru2.memody.dto.HomeDto;
import com.guru2.memody.service.RecommendService;
import com.guru2.memody.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/home")
@Controller
public class HomeController {
    private final RecommendService recommendService;
    @GetMapping
    public ResponseEntity<HomeDto> home(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getUserId();
        HomeDto response = recommendService.getHome(userId);
        return ResponseEntity.ok(response);
    }
}
