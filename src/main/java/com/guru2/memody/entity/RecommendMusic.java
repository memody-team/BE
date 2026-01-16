package com.guru2.memody.entity;

import jakarta.persistence.*;

@Entity
@Table(name="recommend_music")
public class RecommendMusic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendMusicId;

    @ManyToOne
    @JoinColumn(name="musicId")
    private Music music;

    @ManyToOne
    @JoinColumn(name="recommendId")
    private Recommend recommend;

}
