package com.guru2.memody.repository;

import com.guru2.memody.entity.RecommendMusic;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendMusicRepository extends JpaRepository<RecommendMusic, Long> {
    Optional<RecommendMusic> findByUser(User user);
}
