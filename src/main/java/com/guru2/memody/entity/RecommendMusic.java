package com.guru2.memody.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="recommend_music")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendMusic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendMusicId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createTime;

    private Enum MomentType;
    private Enum MoodType;

    @ManyToMany
    @JoinTable(
            name = "recommend_musics",
            joinColumns = @JoinColumn(name = "recommend_id"),
            inverseJoinColumns = @JoinColumn(name = "music_id")
    )
    private List<Music> recommendMusics;
}
