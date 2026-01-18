package com.guru2.memody.repository;

import com.guru2.memody.entity.Record;
import com.guru2.memody.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long> {
    List<Record> findAllByUser(User user);

    List<Record> findAllByUserNot(User user);

    List<Record> findAllByOrderByRecordTimeDesc();

    List<Record> findAllByUserOrderByRecordTimeDesc(User user);
}
