package com.sweep.project.route.domain;

import com.sweep.project.route.RouteSegment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 도보 구간 정보 (trafficType = 3)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalkSegment implements RouteSegment {

    /** 도보 거리 (미터) */
    private int distance;
    /** 도보 소요 시간 (분) */
    private int sectionTime;

    @Override
    public int getTrafficType() {
        return 3;
    }
}
