package com.guru2.memody.repository;

import com.guru2.memody.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByItunesId(Long itunesId);
}
