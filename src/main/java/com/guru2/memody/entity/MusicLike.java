package com.guru2.memody.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "music_like")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class MusicLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long musicLikeId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "music_id")
    private Music music;

    private LocalDateTime likeDate;
}
