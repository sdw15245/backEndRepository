package com.sweep.project.route.mixed;

import com.sweep.project.route.bus.BusBoardingInfo;
import com.sweep.project.route.subway.SubwayBoardingInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * 복합 경로에서 하나의 교통 수단 구간(첫 탑승 또는 환승 지점)에 대한 탑승 정보.
 * <p>
 * trafficType=1(지하철)이면 availableTrains 에, trafficType=2(버스)이면 arrivingBuses 에 값이 채워진다.
 */
@Data
@AllArgsConstructor
public class SegmentBoardingInfo {

    /**
     * 교통 수단 유형.
     * 1: 지하철, 2: 버스
     */
    private int trafficType;

    /** 탑승 역 또는 정류장 이름 */
    private String stopOrStation;

    /** 버스 번호 또는 지하철 노선명 */
    private String transportId;

    /** 각 지역 버스노선 ID (BIS 제공지역 버스 구간인 경우에만 존재) */
    private String localBusId;
    /** 버스노선 BIS 코드 */
    private int busProviderCode;

    /** 각 지역 정류장 ID (BIS 제공지역 버스 구간인 경우에만 존재) */
    private String localStationId;
    /** 정류장 BIS 코드 */
    private int stationProviderCode;
    /** 탑승 정류소의 노선 내 순번 (버스 도착 API ord 파라미터) */
    private int startStopOrder;

    /**
     * 환승 지점 여부.
     * false = 첫 번째 탑승, true = 환승 지점
     */
    private boolean transferPoint;

    /**
     * 이 지점에서 늦어도 이 시각에 탑승해야 목적지 도착 시각을 맞출 수 있음.
     */
    private LocalTime latestBoardingTime;

    /** 지하철 구간인 경우 이용 가능한 열차 목록 (최대 3편성) */
    private List<SubwayBoardingInfo.TrainSchedule> availableTrains;

    /** 버스 구간인 경우 곧 도착하는 버스 목록 (최대 2대) */
    private List<BusBoardingInfo.ArrivingBus> arrivingBuses;

    /**
     * 환승 지점에서 환승 열차가 이 역에 도착하는 시각.
     * subwayPathSchedule API의 startArriveTime 기반.
     * 첫 탑승 구간이거나 버스 구간인 경우 null.
     */
    private LocalTime trainArrivalTime;

    /**
     * 환승 도보 이동 소요 시간 (분).
     * 이전 구간 하차 후 이 구간 탑승역까지 걸어가는 시간.
     * 첫 탑승 구간이거나 도보 환승이 없는 경우 0.
     */
    private int transferWalkMinutes;
}
