package com.guru2.memody.repository;

import com.guru2.memody.entity.Music;
import com.guru2.memody.entity.MusicLike;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicLikeRepository extends JpaRepository<MusicLike, Integer> {

    Optional<MusicLike> findByUserAndMusic(User user, Music music);

    List<MusicLike> findAllByUser(User user);
}
