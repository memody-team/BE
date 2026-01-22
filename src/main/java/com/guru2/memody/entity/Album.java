package com.guru2.memody.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "album")
@NoArgsConstructor
@Getter @Setter
@AllArgsConstructor
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long albumId;
    private String title;
    private String thumbnailUrl;
    private String artist;

    @ManyToMany
    @JoinTable(
            name = "included_songs",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "music_id")
    )
    private List<Music> includedSongs;

    private Long itunesId;
}
