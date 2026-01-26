package com.guru2.memody.extractData;

import com.guru2.memody.entity.Artist;
import com.guru2.memody.entity.Genre;
import com.guru2.memody.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("init")
@Component
@RequiredArgsConstructor
public class ArtistInitRunner implements CommandLineRunner {
    private final ArtistRepository artistRepository;

    @Override
    public void run(String... args) {
        if (artistRepository.count() > 0) return;

        List<String> artists = List.of(
                "TaylorSwift", "NewJeans", "세븐틴", "YOASOBI", "아이유", "ROSE", "백예린", "ILLIT", "카더가든", "OFFICIALHIGEDANDISM", "잔나비", "KATSEYE"
        );

        for (String a : artists) {
            Artist artist = new Artist();
            artist.setArtistName(a);
            artistRepository.save(artist);
        }
    }
}
