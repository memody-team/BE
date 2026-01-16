package com.guru2.memody.service;

import com.guru2.memody.Exception.RegionWrongException;
import com.guru2.memody.Exception.UserAlreadyExistsException;
import com.guru2.memody.Exception.UserNameAlreadyExistsException;
import com.guru2.memody.config.JwtTokenProvider;
import com.guru2.memody.dto.LoginRequestDto;
import com.guru2.memody.dto.SignUpDto;
import com.guru2.memody.dto.SignUpResponseDto;
import com.guru2.memody.entity.Region;
import com.guru2.memody.entity.User;
import com.guru2.memody.repository.RegionRepository;
import com.guru2.memody.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegionRepository regionRepository;

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public SignUpResponseDto signup(SignUpDto signUpDto) {
        if(userRepository.findUserByEmail(signUpDto.getEmail()).isPresent()){
            throw new UserAlreadyExistsException();
        }
        if(userRepository.findUserByName(signUpDto.getName()).isPresent()){
            throw new UserNameAlreadyExistsException();
        }

        String encodedPassword = passwordEncoder.encode(signUpDto.getPassword());

        User user = new User();
        user.setEmail(signUpDto.getEmail());
        user.setPassword(encodedPassword);
        user.setName(signUpDto.getName());
        userRepository.save(user);

        SignUpResponseDto signUpResponseDto = new SignUpResponseDto();
        signUpResponseDto.setUserId(user.getUserId());

        return signUpResponseDto;
    }

    public String login(LoginRequestDto loginRequestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateJwtToken(loginRequestDto.getEmail());
        return token;

    }

    public String updateRegion(Long userId, String region) {
        Region reg = regionRepository.findFirstByFullNameContaining(region);
        if(reg == null) {
            throw new RegionWrongException("Region not found");
        }
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                () -> new UserAlreadyExistsException()
        );

        user.setLocation(reg);
        return "Patch Region Successful: " + reg.getFullName();

    }


}
