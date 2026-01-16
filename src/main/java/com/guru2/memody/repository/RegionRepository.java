package com.guru2.memody.repository;

import com.guru2.memody.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findAllByFullNameContaining(String fullName);
    Region findFirstByFullNameContaining(String fullName);
}
