package com.guru2.memody.repository;

import com.guru2.memody.entity.RecommendArtist;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendArtistRepository extends JpaRepository<RecommendArtist, Long> {
    Optional<RecommendArtist> findByUser(User user);
}
