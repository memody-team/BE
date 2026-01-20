package com.guru2.memody.repository;

import com.guru2.memody.entity.GenrePreference;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenrePreferenceRepository extends JpaRepository<GenrePreference, Long> {
    void deleteAllByUser(User user);

    List<GenrePreference> findAllByUser(User user);
}
