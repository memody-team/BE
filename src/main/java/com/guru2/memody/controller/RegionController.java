package com.guru2.memody.controller;

import com.guru2.memody.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/location")
@Controller
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/search")
    public ResponseEntity<List<String>> searchRegion(@RequestParam String region) {
        ResponseEntity<List<String>> response = regionService.searchRegions(region);
        return response;
    }

}
