package com.guru2.memody.regionExtract;

import com.guru2.memody.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("init")
public class RegionInitRunner implements CommandLineRunner {

    private final RegionService regionService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("InitRunner 실행");
        regionService.initRegions();
        System.out.println("initRunner 종료");
    }
}
