package com.guru2.memody.repository;

import com.guru2.memody.entity.ArtistPreference;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistPreferenceRepository extends JpaRepository<ArtistPreference, Long> {
    void deleteAllByUser(User user);
}
