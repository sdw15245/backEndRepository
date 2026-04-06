package com.sweep.project.route.mixed;

import com.sweep.project.route.*;
import com.sweep.project.route.bus.BusArrivalResponse;
import com.sweep.project.route.bus.BusBoardingInfo;
import com.sweep.project.route.bus.BusRoute;
import com.sweep.project.route.subway.SubwayBoardingInfo;
import com.sweep.project.route.subway.SubwayRoute;
import com.sweep.project.route.subway.SubwayScheduleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.sweep.project.route.PathSearchType.PATH_TYPE_ANYONE;
import static com.sweep.project.route.TrafficType.*;

/**
 * SearchPathType=0(전체) 조회 후 pathType=3(버스+지하철 혼합) 경로를 처리하는 전략.
 * <p>
 * 첫 번째 탑승 구간뿐만 아니라 환승 지점마다 해당 버스/지하철 도착 정보를 조회하여
 * {@link MixedBoardingInfo}에 담아 반환한다.
 * 공공데이터포털(서울 버스 API) 호출은 이 서비스에서만 수행한다.
 */
@Service
@Slf4j
public class MixedRouteService extends AbstractRouteSearch {

    private static final int PATH_TYPE_MIXED = 3; // ODsay pathType=3: 버스+지하철 혼합
    private static final int MAX_TRAIN_RESULTS = 3;

    @Override
    public boolean checkType(PathSearchType pathSearchType) {
        return pathSearchType == PATH_TYPE_ANYONE;
    }

    @Override
    public List<MixedRoute> getRoutes(PathSearchType pathSearchType) {
        OdsayRouteResponse response = callRouteApi(PATH_TYPE_ANYONE.pathType);
        return parseMixedRoutes(response);
    }

    /**
     * 복합 경로의 모든 교통 수단 구간(첫 탑승 + 환승 지점)에 대해 탑승 정보를 계산한다.
     *
     * <pre>
     * 각 구간의 최대 탑승 시각 계산:
     *   latestBoardingTime(i) = desiredArrivalTime - (totalTime - elapsedTimeBeforeSegment(i))
     *   elapsedTimeBeforeSegment(i) = 출발부터 해당 구간 탑승 지점까지의 누적 소요 시간
     * </pre>
     *
     * @param route MixedRoute 타입이어야 한다.
     */
    @Override
    public MixedBoardingInfo getBoardingInfo(LocalDateTime desiredArrivalTime, TrafficResponse route) {
        MixedRoute mixedRoute = (MixedRoute) route;
        int totalTime = mixedRoute.getTotalTime();

        LocalTime recommendedDepartureTime = desiredArrivalTime.minusMinutes(totalTime).toLocalTime();

        List<SegmentBoardingInfo> segmentBoardingInfos = new ArrayList<>();
        boolean firstTransitFound = false;
        int elapsedTime = 0; // 출발부터 현재 구간 시작까지 누적 소요 시간

        for (RouteSegment segment : mixedRoute.getSegments()) {
            if (segment instanceof WalkSegment) {
                elapsedTime += segment.getSectionTime();
                continue;
            }

            boolean isTransferPoint = firstTransitFound;
            firstTransitFound = true;

            // 이 구간 탑승 지점에서의 최대 탑승 시각
            int remainingFromHere = totalTime - elapsedTime;
            LocalTime latestBoardingTime = desiredArrivalTime.minusMinutes(remainingFromHere).toLocalTime();

            SegmentBoardingInfo segInfo = buildSegmentBoardingInfo(
                    segment, isTransferPoint, latestBoardingTime, desiredArrivalTime);

            if (segInfo != null) {
                segmentBoardingInfos.add(segInfo);
            }

            elapsedTime += segment.getSectionTime();
        }

        if (segmentBoardingInfos.isEmpty()) {
            throw new IllegalArgumentException("해당 경로에 교통 수단 구간이 존재하지 않습니다.");
        }

        return new MixedBoardingInfo(recommendedDepartureTime, segmentBoardingInfos);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private SegmentBoardingInfo buildSegmentBoardingInfo(
            RouteSegment segment,
            boolean isTransferPoint,
            LocalTime latestBoardingTime,
            LocalDateTime desiredArrivalTime) {

        if (segment instanceof SubwayRoute.SubwaySegment subway) {
            SubwayScheduleResponse scheduleResponse = callScheduleApi(
                    subway.getStartID(), subway.getWayCode());
            List<SubwayBoardingInfo.TrainSchedule> trains = filterAvailableTrains(
                    scheduleResponse, desiredArrivalTime.getDayOfWeek(),
                    subway.getWayCode(), latestBoardingTime, isTransferPoint);

            return new SegmentBoardingInfo(
                    TRAFFIC_TYPE_SUBWAY.trafficNumber,
                    subway.getStartStation(),
                    subway.getLineName(),
                    isTransferPoint,
                    latestBoardingTime,
                    trains,
                    new ArrayList<>(),
                    null,  // trainArrivalTime: 역별 시간표 기반이라 별도 열차 도착 시각 없음
                    0      // transferWalkMinutes: 혼합 경로의 지하철 구간은 별도 집계 없음
            );

        } else if (segment instanceof BusRoute.BusSegment bus) {
            BusArrivalResponse arrivalResponse = callBusArrivalApi(
                    bus.getLocalBusStationId(),
                    bus.getLocalBusId(),
                    bus.getStartStopOrder());
            List<BusBoardingInfo.ArrivingBus> arrivingBuses = parseArrivingBuses(arrivalResponse);

            return new SegmentBoardingInfo(
                    TRAFFIC_TYPE_BUS.trafficNumber,
                    bus.getStartStop(),
                    bus.getBusNo(),
                    isTransferPoint,
                    latestBoardingTime,
                    new ArrayList<>(),
                    arrivingBuses,
                    null,  // trainArrivalTime: 버스 구간에는 해당 없음
                    0      // transferWalkMinutes: 혼합 경로의 버스 구간은 별도 집계 없음
            );
        }

        return null;
    }

    private List<MixedRoute> parseMixedRoutes(OdsayRouteResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getPath() == null) {
            log.info("복합응답 없음");
            return new ArrayList<>();
        }
        return response.getResult().getPath().stream()
                .filter(path -> path.getPathType() == PATH_TYPE_MIXED)
                .map(this::toMixedRoute)
                .collect(Collectors.toList());
    }

    private MixedRoute toMixedRoute(OdsayRouteResponse.Path path) {
        OdsayRouteResponse.Info info = path.getInfo();
        List<RouteSegment> segments = new ArrayList<>();

        if (path.getSubPath() != null) {
            for (OdsayRouteResponse.SubPath subPath : path.getSubPath()) {
                if (subPath.getTrafficType() == TRAFFIC_TYPE_HUMAN.trafficNumber) {
                    segments.add(new WalkSegment(subPath.getDistance(), subPath.getSectionTime()));

                } else if (subPath.getTrafficType() == TRAFFIC_TYPE_SUBWAY.trafficNumber
                        && subPath.getLane() != null) {
                    // 지하철 구간 – 첫 번째 lane 정보만 사용
                    OdsayRouteResponse.Lane lane = subPath.getLane().get(0);
                    segments.add(new SubwayRoute.SubwaySegment(
                            lane.getName(),
                            lane.getSubwayCode(),
                            subPath.getStartName(),
                            subPath.getEndName(),
                            subPath.getStationCount(),
                            subPath.getSectionTime(),
                            subPath.getDistance(),
                            subPath.getStartID(),
                            subPath.getEndID(),
                            subPath.getWayCode()
                    ));

                } else if (subPath.getTrafficType() == TRAFFIC_TYPE_BUS.trafficNumber
                        && subPath.getLane() != null) {
                    // 버스 구간 – 첫 번째 lane 정보만 사용
                    OdsayRouteResponse.Lane lane = subPath.getLane().get(0);
                    segments.add(new BusRoute.BusSegment(
                            lane.getBusNo(),
                            lane.getType(),
                            subPath.getStartName(),
                            subPath.getEndName(),
                            subPath.getStationCount(),
                            subPath.getSectionTime(),
                            subPath.getDistance(),
                            lane.getBusID(),
                            subPath.getStartID(),
                            subPath.getStartExNo(),
                            subPath.getStartLocalStationID(),
                            lane.getBusLocalBlID()
                    ));
                }
            }
        }

        int transferCount = Math.max(0, info.getBusTransitCount() + info.getSubwayTransitCount() - 1);
        return new MixedRoute(
                info.getTotalTime(),
                info.getPayment(),
                transferCount,
                info.getBusTransitCount(),
                info.getSubwayTransitCount(),
                info.getTotalWalk(),
                segments
        );
    }

    private SubwayScheduleResponse callScheduleApi(int stationID, int wayCode) {
        String url = UriComponentsBuilder.fromHttpUrl(SCHEDULE_SEARCH_URL)
                .queryParam("apiKey", odsayKey)
                .queryParam("stationID", stationID)
                .queryParam("wayCode", wayCode)
                .toUriString();
        log.info("subway schedule url: {}", url);
        return restTemplate.getForObject(url, SubwayScheduleResponse.class);
    }

    private BusArrivalResponse callBusArrivalApi(String stationID, String busRouteId, int ord) {
        String url = UriComponentsBuilder.fromHttpUrl(BUS_ARRIVAL_URL)
                .queryParam("serviceKey", seoulBusApiKey)
                .queryParam("stId", stationID)
                .queryParam("busRouteId", busRouteId)
                .queryParam("ord", ord)
                .queryParam("resultType", "json")
                .toUriString();
        log.info("bus arrival url: {}", url);
        return restTemplate.getForObject(url, BusArrivalResponse.class);
    }

    /**
     * @param isTransferPoint false → 마감 이전 열차 (마지막 3편), true → 도착 이후 열차 (다음 3편)
     */
    private List<SubwayBoardingInfo.TrainSchedule> filterAvailableTrains(
            SubwayScheduleResponse response,
            DayOfWeek dayOfWeek,
            int wayCode,
            LocalTime pivotTime,
            boolean isTransferPoint) {

        if (response == null || response.getResult() == null) return new ArrayList<>();

        SubwayScheduleResponse.WeekSchedule weekSchedule = switch (dayOfWeek) {
            case SATURDAY -> response.getResult().getSaturdaySchedule();
            case SUNDAY   -> response.getResult().getHolidaySchedule();
            default       -> response.getResult().getWeekdaySchedule();
        };
        if (weekSchedule == null) return new ArrayList<>();

        List<SubwayScheduleResponse.TrainTime> trainTimes =
                (wayCode == 1) ? weekSchedule.getUp() : weekSchedule.getDown();
        if (trainTimes == null) return new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        List<SubwayBoardingInfo.TrainSchedule> parsed = trainTimes.stream()
                .filter(t -> t.getDepartureTime() != null)
                .map(t -> new SubwayBoardingInfo.TrainSchedule(
                        LocalTime.parse(t.getDepartureTime(), fmt),
                        t.getEndStationName(),
                        t.getSubwayClass(),
                        t.getFirstLastFlag()
                ))
                .collect(Collectors.toList());

        if (isTransferPoint) {
            return parsed.stream()
                    .filter(t -> !t.getDepartureTime().isBefore(pivotTime))
                    .sorted(Comparator.comparing(SubwayBoardingInfo.TrainSchedule::getDepartureTime))
                    .limit(MAX_TRAIN_RESULTS)
                    .collect(Collectors.toList());
        } else {
            return parsed.stream()
                    .filter(t -> !t.getDepartureTime().isAfter(pivotTime))
                    .sorted(Comparator.comparing(SubwayBoardingInfo.TrainSchedule::getDepartureTime).reversed())
                    .limit(MAX_TRAIN_RESULTS)
                    .sorted(Comparator.comparing(SubwayBoardingInfo.TrainSchedule::getDepartureTime))
                    .collect(Collectors.toList());
        }
    }

    private List<BusBoardingInfo.ArrivingBus> parseArrivingBuses(BusArrivalResponse response) {
        if (response == null
                || response.getServiceResult() == null
                || response.getServiceResult().getMsgBody() == null
                || response.getServiceResult().getMsgBody().getItemList() == null
                || response.getServiceResult().getMsgBody().getItemList().isEmpty()) {
            return new ArrayList<>();
        }

        BusArrivalResponse.Item item = response.getServiceResult().getMsgBody().getItemList().get(0);
        List<BusBoardingInfo.ArrivingBus> result = new ArrayList<>();

        if (isValidArrival(item.getArrmsg1())) {
            result.add(new BusBoardingInfo.ArrivingBus(
                    item.getPlainNo1(), item.getArrmsg1(),
                    parseIntSafe(item.getTraTime1()), parseIntSafe(item.getRerdieNum1()),
                    item.getPrvSttn1(), parseIntSafe(item.getCongestdeg1()),
                    "1".equals(item.getIsLast1()),
                    parseDoubleSafe(item.getGpsX1()), parseDoubleSafe(item.getGpsY1())
            ));
        }
        if (isValidArrival(item.getArrmsg2())) {
            result.add(new BusBoardingInfo.ArrivingBus(
                    item.getPlainNo2(), item.getArrmsg2(),
                    parseIntSafe(item.getTraTime2()), parseIntSafe(item.getRerdieNum2()),
                    item.getPrvSttn2(), parseIntSafe(item.getCongestdeg2()),
                    "1".equals(item.getIsLast2()),
                    parseDoubleSafe(item.getGpsX2()), parseDoubleSafe(item.getGpsY2())
            ));
        }
        return result;
    }

    private boolean isValidArrival(String arrmsg) {
        return arrmsg != null && !arrmsg.isBlank()
                && !arrmsg.contains("운행종료") && !arrmsg.contains("출발대기");
    }

    private int parseIntSafe(String value) {
        if (value == null || value.isBlank()) return 0;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private double parseDoubleSafe(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try { return Double.parseDouble(value.trim()); } catch (NumberFormatException e) { return 0.0; }
    }
}
