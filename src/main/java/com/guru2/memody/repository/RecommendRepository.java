package com.guru2.memody.repository;

import com.guru2.memody.entity.Recommend;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendRepository extends JpaRepository<Recommend, Long> {
    Optional<Recommend> findByUser(User user);
}
