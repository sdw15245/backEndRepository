package com.sweep.project.route.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 경로 검색 결과 스냅샷.
 * Member 와의 연관은 {@link Alarm} 을 통해 맺어진다.
 * 좌표는 소수점 4자리 반올림 후 저장한다 (Redis 키와 동일).
 */
@Entity
@Table(name = "route")
@Getter
@NoArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PathSearchType type;

    /** 경도(lon), 소수점 4자리 반올림 */
    private double startX;
    /** 위도(lat), 소수점 4자리 반올림 */
    private double startY;
    private double endX;
    private double endY;

    /** ODsay API 단일 경로 JSON (TrafficResponse) */
    @Column(columnDefinition = "jsonb")
    private String routeData;

    /** 총 소요 시간 (분) — routeData.totalTime 을 별도 컬럼으로 캐싱 */
    private Integer totalTime;

    private LocalDateTime createDate;

    @Builder
    public Route(PathSearchType type,
                 double startX, double startY,
                 double endX, double endY,
                 String routeData,
                 Integer totalTime) {
        this.type = type;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.routeData = routeData;
        this.totalTime = totalTime;
        this.createDate = LocalDateTime.now();
    }
}
