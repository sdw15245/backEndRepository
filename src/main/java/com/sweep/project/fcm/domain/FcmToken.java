package com.sweep.project.fcm.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, unique = true)
    private String token;

    // 토큰 정리 위한 마지막 토큰 갱신 시간
    private LocalDateTime updatedAt;

    @Builder
    public FcmToken(Long memberId, String token) {
        this.memberId = memberId;
        this.token = token;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTimestamp(){ // 토큰 갱신
        this.updatedAt = LocalDateTime.now();
    }
}
