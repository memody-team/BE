package com.guru2.memody.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "`like`")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "record_id")
    private Record record;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isLiked = true;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime likeDate = LocalDateTime.now();

}
