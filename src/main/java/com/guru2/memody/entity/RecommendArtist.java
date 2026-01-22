package com.guru2.memody.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "recommend_artist")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendArtist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendArtistId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "recommend_artist_musics",
            joinColumns = @JoinColumn(name = "recommend_artist_id"),
            inverseJoinColumns = @JoinColumn(name = "music_id")
    )
    private List<Music> recommendedItems;

    private LocalDateTime createTime;
}
