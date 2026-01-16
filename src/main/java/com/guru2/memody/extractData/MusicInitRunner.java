package com.guru2.memody.extractData;

import com.guru2.memody.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("init")
public class MusicInitRunner implements CommandLineRunner {

    private final MusicService musicService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Music Init Started");
        musicService.initMusics();
        System.out.println("Music Init Finished");
    }

}
