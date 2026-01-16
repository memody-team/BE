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
                "팝(Pop)", "K-Pop", "힙합/랩", "R&B/소울", "인디/얼터너티브", "록/메탈", "일렉트로닉/EDM", "재즈", "클래식", "OST/영화음악", "로파이/칠", "트로트"
        );

        for (String g : genres) {
            Genre genre = new Genre();
            genre.setGenreName(g);
            genreRepository.save(genre);
        }
    }
}

