package com.guru2.memody.repository;

import com.guru2.memody.entity.RecommendAlbum;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendAlbumRepository extends JpaRepository<RecommendAlbum, Long> {
    Optional<RecommendAlbum> findByUser(User user);
}
