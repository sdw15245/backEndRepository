package com.sweep.project.route.mixed;

import com.sweep.project.route.RouteSegment;
import com.sweep.project.route.TrafficResponse;
import lombok.Data;

import java.util.List;

/**
 * ODsay SearchPathType=0 응답에서 pathType=3(버스+지하철 혼합) 경로를 파싱한 모델.
 * segments 에 WalkSegment / SubwaySegment / BusSegment 가 순서대로 들어있다.
 */
@Data
public class MixedRoute implements TrafficResponse {

    /** DB Route ID */
    private Long routeId;

    /** 총 소요 시간 (분) */
    private int totalTime;
    /** 요금 (원) */
    private int payment;
    /** 환승 횟수 */
    private int transferCount;
    /** 버스 탑승 횟수 */
    private int busTransitCount;
    /** 지하철 탑승 횟수 */
    private int subwayTransitCount;
    /** 총 도보 거리 (미터) */
    private int totalWalk;
    /** 구간 목록 – trafficType으로 WalkSegment(3)/SubwaySegment(1)/BusSegment(2) 구분 */
    private List<RouteSegment> segments;
    /** 노선별 그래픽 데이터 흭득시 첨부해야 하는값*/
    private String mapObj;

    public MixedRoute(int totalTime, int payment, int transferCount,
                      int busTransitCount, int subwayTransitCount, int totalWalk,
                      List<RouteSegment> segments, String mapObj) {
        this.totalTime = totalTime;
        this.payment = payment;
        this.transferCount = transferCount;
        this.busTransitCount = busTransitCount;
        this.subwayTransitCount = subwayTransitCount;
        this.totalWalk = totalWalk;
        this.segments = segments;
        this.mapObj = mapObj;
    }
}
