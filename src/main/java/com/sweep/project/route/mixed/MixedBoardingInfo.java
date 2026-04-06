package com.sweep.project.route.mixed;

import com.sweep.project.route.BoardingInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * 복합(버스+지하철) 경로 전체 탑승 정보.
 * <p>
 * segmentBoardingInfos 의 첫 번째 항목(transferPoint=false)이 최초 탑승 정보이고,
 * 나머지 항목(transferPoint=true)이 환승 지점별 탑승 정보이다.
 */
@Data
@AllArgsConstructor
public class MixedBoardingInfo implements BoardingInfo {

    /**
     * 출발지에서 출발해야 하는 권장 시각.
     * = desiredArrivalTime - totalTime
     */
    private LocalTime recommendedDepartureTime;

    /**
     * 교통 수단 구간별 탑승 정보 목록.
     * index 0: 첫 번째 탑승 정보 (transferPoint=false)
     * index 1+: 환승 지점 탑승 정보 (transferPoint=true)
     */
    private List<SegmentBoardingInfo> segmentBoardingInfos;
}
