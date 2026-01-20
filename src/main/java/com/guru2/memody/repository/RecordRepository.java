package com.guru2.memody.repository;

import com.guru2.memody.entity.Music;
import com.guru2.memody.entity.Record;
import com.guru2.memody.entity.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long> {
    List<Record> findAllByUser(User user);

    List<Record> findAllByUserNot(User user);

    List<Record> findAllByOrderByRecordTimeDesc();

    List<Record> findAllByUserOrderByRecordTimeDesc(User user);
    List<Record> findAllByUserOrderByRecordTimeDesc(User user, Limit limit);

    List<Record> findAllByRecordMusic(Music recordMusic);
}
