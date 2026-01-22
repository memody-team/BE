package com.guru2.memody.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "recommend_album")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendAlbum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendAlbumId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "recommend_albums",
            joinColumns = @JoinColumn(name = "recommend_album_id"),
            inverseJoinColumns = @JoinColumn(name = "album_id")
    )
    private List<Album> albums;

    private LocalDateTime createTime;
}
