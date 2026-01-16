package com.guru2.memody.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "record")
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "musicId")
    private Music recordMusic;

    private String text;

    private LocalDateTime recordTime;
    private String recordLocation;

    private String latitude;
    private String longitude;
}
