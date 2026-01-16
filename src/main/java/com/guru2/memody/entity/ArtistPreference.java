package com.guru2.memody.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "artist_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long artistPreferenceId;

    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
