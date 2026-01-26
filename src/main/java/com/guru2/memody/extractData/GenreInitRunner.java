package com.guru2.memody.extractData;

import com.guru2.memody.entity.Genre;
import com.guru2.memody.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("init")
@Component
@RequiredArgsConstructor
public class GenreInitRunner implements CommandLineRunner {

    private final GenreRepository genreRepository;

    @Override
    public void run(String... args) {
        if (genreRepository.count() > 0) return;

        List<String> genres = List.of(
                "POP", "KPOP", "HIPHOP", "RNB", "INDIE", "ROCK", "ELECTRONIC", "JAZZ", "CLASSIC", "OST", "LOFI", "TROT"
        );

        for (String g : genres) {
            Genre genre = new Genre();
            genre.setGenreName(g);
            genreRepository.save(genre);
        }
    }
}

