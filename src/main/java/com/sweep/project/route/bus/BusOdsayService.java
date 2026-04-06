package com.sweep.project.route.bus;

import com.sweep.project.route.*;
import com.sweep.project.route.mixed.MixedBoardingInfo;
import com.sweep.project.route.mixed.SegmentBoardingInfo;
import com.sweep.project.route.subway.SubwayRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sweep.project.route.PathSearchType.PATH_TYPE_BUS;
import static com.sweep.project.route.TrafficType.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusOdsayService extends AbstractRouteSearch {


    @Override
    public boolean checkType(PathSearchType pathSearchType) {
        return pathSearchType == PATH_TYPE_BUS;
    }

    @Override
    public List<BusRoute> getRoutes(PathSearchType pathSearchType) {
        log.info("start");
        OdsayRouteResponse response = callRouteApi(PATH_TYPE_BUS.pathType);
        log.info("response:{}",response);
        return parseBusRoutes(response);
    }

    /**
     * {@inheritDoc}
     *
     * <pre>
     * 계산 방식:
     *   timeToFirstStop      = 최초 버스 탑승 구간 이전 도보 소요 시간의 합
     *   latestBoardingTime   = desiredArrivalTime - (totalTime - timeToFirstStop)
     *   recommendedDeparture = desiredArrivalTime - totalTime
     * </pre>
     *
     * @param route BusRoute 타입이어야 한다.
     */
    /*
    *     public MixedBoardingInfo getBoardingInfo(LocalDateTime desiredArrivalTime, TrafficResponse route) {
        BusRoute busRoute = (BusRoute) route;
        boolean firstFound = false;
        int timeToFirstStop = 0;

        List<SegmentBoardingInfo> segmentBoardingInfos = new ArrayList<>();

        int totalTime = busRoute.getTotalTime();
        LocalTime recommendedDepartureTime = desiredArrivalTime
                .minusMinutes(totalTime)
                .toLocalTime();


        for (RouteSegment segment : busRoute.getSegments()) {
            if (segment instanceof WalkSegment) {
                timeToFirstStop += segment.getSectionTime();
                continue;
            }
            if (!(segment instanceof BusRoute.BusSegment busSegment)) continue;

            boolean isTransferPoint = firstFound;
            firstFound = true;

            LocalTime latestBoardingTime = desiredArrivalTime
                    .minusMinutes(totalTime - timeToFirstStop)
                    .toLocalTime();

            String stationId=busSegment.getLocalBusStationId();
            String busId=busSegment.getLocalBusId();

            BusArrivalResponse arrivalResponse = callBusArrivalApi(
                    stationId,
                    busId,
                    busSegment.getStartStopOrder()
            );

            List<BusBoardingInfo.ArrivingBus> arrivingBuses = parseArrivingBuses(arrivalResponse);


            segmentBoardingInfos.add(new SegmentBoardingInfo(
                    TRAFFIC_TYPE_BUS.trafficNumber,
                    busSegment.getLocalBusStationId(),
                    busSegment.getLocalBusId(),
                    isTransferPoint,
                    latestBoardingTime,
                    new ArrayList<>(),
                    arrivingBuses

            ));
            timeToFirstStop+=busSegment.getSectionTime();
        }

        if (segmentBoardingInfos.isEmpty()) {
            throw new IllegalArgumentException("해당 경로에 버스 구간이 존재하지 않습니다.");
        }


        return new MixedBoardingInfo(recommendedDepartureTime, segmentBoardingInfos);
    }*/
    @Override
    public BusBoardingInfo getBoardingInfo(LocalDateTime desiredArrivalTime, TrafficResponse route) {
        BusRoute busRoute = (BusRoute) route;
        int timeToFirstStop = 0;
        BusRoute.BusSegment firstBusSegment = null;

        for (RouteSegment segment : busRoute.getSegments()) {
            if (segment instanceof WalkSegment) {
                timeToFirstStop += segment.getSectionTime();
            } else if (segment instanceof BusRoute.BusSegment bus) {
                firstBusSegment = bus;
                break;
            }
        }

        if (firstBusSegment == null) {
            throw new IllegalArgumentException("해당 경로에 버스 구간이 존재하지 않습니다.");
        }

        int totalTime = busRoute.getTotalTime();
        LocalTime latestBoardingTime = desiredArrivalTime
                .minusMinutes(totalTime - timeToFirstStop)
                .toLocalTime();
        LocalTime recommendedDepartureTime = desiredArrivalTime
                .minusMinutes(totalTime)
                .toLocalTime();


        String stationId=firstBusSegment.getLocalBusStationId();
        String busId=firstBusSegment.getLocalBusId();

        BusArrivalResponse arrivalResponse = callBusArrivalApi(
                stationId,
                busId,
                firstBusSegment.getStartStopOrder()
        );

        List<BusBoardingInfo.ArrivingBus> arrivingBuses = parseArrivingBuses(arrivalResponse);

        return new BusBoardingInfo(
                firstBusSegment.getStartStop(),
                firstBusSegment.getBusNo(),
                latestBoardingTime,
                recommendedDepartureTime,
                arrivingBuses
        );
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<BusRoute> parseBusRoutes(OdsayRouteResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getPath() == null) {
            return new ArrayList<>();
        }

        return response.getResult().getPath().stream()
                .filter(path -> path.getPathType() == PATH_TYPE_BUS.pathType)
                .map(this::toBusRoute)
                .collect(Collectors.toList());
    }

    private BusRoute toBusRoute(OdsayRouteResponse.Path path) {
        OdsayRouteResponse.Info info = path.getInfo();
        List<RouteSegment> segments = new ArrayList<>();

        if (path.getSubPath() != null) {
            for (OdsayRouteResponse.SubPath subPath : path.getSubPath()) {
                if (subPath.getTrafficType() == TRAFFIC_TYPE_HUMAN.trafficNumber) {
                    segments.add(new WalkSegment(subPath.getDistance(), subPath.getSectionTime()));
                } else if (subPath.getTrafficType() == TRAFFIC_TYPE_BUS.trafficNumber && subPath.getLane() != null) {
                    for (OdsayRouteResponse.Lane lane : subPath.getLane()) {
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
        }

        int transferCount = Math.max(0, info.getBusTransitCount() + info.getSubwayTransitCount() - 1);
        return new BusRoute(
                info.getTotalTime(),
                info.getPayment(),
                transferCount,
                info.getBusTransitCount(),
                info.getTotalWalk(),
                segments
        );
    }

    private BusArrivalResponse callBusArrivalApi(String stationID, String busRouteId, int ord) {
        String url = UriComponentsBuilder.fromHttpUrl(BUS_ARRIVAL_URL)
                .queryParam("serviceKey", seoulBusApiKey)
                .queryParam("stId", stationID)
                .queryParam("busRouteId", busRouteId)
                .queryParam("ord", ord)
                .queryParam("resultType", "json")
                .toUriString();

        return restTemplate.getForObject(url, BusArrivalResponse.class);
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
