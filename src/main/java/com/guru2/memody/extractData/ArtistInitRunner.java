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
                "아이유", "NewJeans", "세븐틴", "(여자)아이들", "Taylor Swift", "Billie Eilish", "Ariana Grande", "The Weekend", "실리카겔", "백예린", "AKMU", "Lauv", "YOASOBI", "Olivia Rodrigo", "IVE", "ZICO"
        );

        for (String a : artists) {
            Artist artist = new Artist();
            artist.setArtistName(a);
            artistRepository.save(artist);
        }
    }
}
