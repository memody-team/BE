package com.guru2.memody.controller;

import com.guru2.memody.config.CustomUserDetails;
import com.guru2.memody.dto.LoginRequestDto;
import com.guru2.memody.dto.SignUpDto;
import com.guru2.memody.dto.SignUpResponseDto;
import com.guru2.memody.entity.Region;
import com.guru2.memody.repository.UserRepository;
import com.guru2.memody.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Controller
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    ResponseEntity<SignUpResponseDto> signup(@RequestBody SignUpDto signUpDto){
        SignUpResponseDto signUpResponseDto = userService.signup(signUpDto);
        return ResponseEntity.ok(signUpResponseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto loginRequestDto) {
        String token = userService.login(loginRequestDto);
        return ResponseEntity.ok(token);
    }

    @PatchMapping("/region")
    public ResponseEntity<String> updateRegion(@RequestParam Long userId,
                                               @RequestParam String region) {
        String response = userService.updateRegion(userId, region);

        return ResponseEntity.ok(response);
    }
    
}
