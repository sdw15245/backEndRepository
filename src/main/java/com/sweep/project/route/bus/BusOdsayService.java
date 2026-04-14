package com.sweep.project.route.bus;

import com.sweep.project.route.*;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.domain.WalkSegment;
import com.sweep.project.route.dto.RequestRouteDto;
import com.sweep.project.route.mixed.MixedBoardingInfo;
import com.sweep.project.route.mixed.SegmentBoardingInfo;
import com.sweep.project.util.GeoLocationCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sweep.project.route.domain.PathSearchType.PATH_TYPE_BUS;
import static com.sweep.project.route.TrafficType.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusOdsayService extends AbstractRouteSearch {


    @Override
    public boolean checkType(PathSearchType pathSearchType) {
        return pathSearchType == PATH_TYPE_BUS;
    }

    @GeoLocationCache
    @Override
    public List<BusRoute> getRoutes(PathSearchType type,double startLat,double startLon,double endLat,double endLon) {
        log.info("start");
        OdsayRouteResponse response = callRouteApi(PATH_TYPE_BUS.pathType,startLat,startLon,endLat,endLon);
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
    @Override
    public MixedBoardingInfo getBoardingInfo(LocalDateTime desiredArrivalTime, TrafficResponse route) {
        BusRoute busRoute = (BusRoute) route;
        boolean firstFound = false;
        int timeElapsed = 0;
        int pendingWalkMinutes = 0;

        List<SegmentBoardingInfo> segmentBoardingInfos = new ArrayList<>();

        int totalTime = busRoute.getTotalTime();
        LocalTime recommendedDepartureTime = desiredArrivalTime
                .minusMinutes(totalTime)
                .toLocalTime();

        for (RouteSegment segment : busRoute.getSegments()) {
            if (segment instanceof WalkSegment walk) {
                timeElapsed += walk.getSectionTime();
                if (firstFound) {
                    pendingWalkMinutes += walk.getSectionTime();
                }
                continue;
            }
            if (!(segment instanceof BusRoute.BusSegment busSegment)) continue;

            boolean isTransferPoint = firstFound;
            firstFound = true;

            LocalTime latestBoardingTime = desiredArrivalTime
                    .minusMinutes(totalTime - timeElapsed)
                    .toLocalTime();

            segmentBoardingInfos.add(new SegmentBoardingInfo(
                    TRAFFIC_TYPE_BUS.trafficNumber,
                    busSegment.getStartStop(),
                    busSegment.getBusNo(),
                    isTransferPoint,
                    latestBoardingTime,
                    new ArrayList<>(),
                    null,
                    null,
                    pendingWalkMinutes
            ));

            timeElapsed += busSegment.getSectionTime();
            pendingWalkMinutes = 0;
        }

        if (segmentBoardingInfos.isEmpty()) {
            throw new IllegalArgumentException("해당 경로에 버스 구간이 존재하지 않습니다.");
        }

        return new MixedBoardingInfo(recommendedDepartureTime, segmentBoardingInfos);
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
}
