package com.guru2.memody.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "genre_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenrePreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long genrePreferenceId;

    @ManyToOne
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
