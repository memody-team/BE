package com.guru2.memody.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="recommend")
public class Recommend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;



    private LocalDateTime createTime;
}
