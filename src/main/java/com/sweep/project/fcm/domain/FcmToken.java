package com.sweep.project.fcm.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private LocalDateTime updateAt;

    @Builder
    public FcmToken(Long memberId, String token) {
        this.memberId = memberId;
        this.token = token;
        this.updateAt = LocalDateTime.now();
    }

    public void updateToken(){ // 토큰 갱신
        this.updateAt = LocalDateTime.now();
    }
}
