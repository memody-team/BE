package com.guru2.memody.repository;

import com.guru2.memody.entity.ArtistPreference;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistPreferenceRepository extends JpaRepository<ArtistPreference, Long> {
    void deleteAllByUser(User user);

    List<ArtistPreference> findAllByUser(User user);
}
