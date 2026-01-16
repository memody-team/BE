package com.guru2.memody.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "region")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id")
    private Long regionId;

    // 행정구역 코드 (법정동 코드)
    @Column(unique = true)
    private String code;

    // ex. "서울특별시 강남구 역삼동"
    @Column(length = 30, nullable = false)
    private String fullName;

    private Double latitude;
    private Double longitude;
}

