package com.sweep.project.route.mixed;

import com.sweep.project.route.*;
import com.sweep.project.route.bus.BusRoute;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.domain.WalkSegment;
import com.sweep.project.route.subway.SubwayBoardingInfo;
import com.sweep.project.route.subway.SubwayPathScheduleResponse;
import com.sweep.project.route.subway.SubwayRoute;
import com.sweep.project.util.GeoLocationCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.sweep.project.route.domain.PathSearchType.PATH_TYPE_ANYONE;
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
    private static final int SUBWAY_MOVING_TYPE = 1;
    private static final int PATH_TYPE_SHORTEST = 1;

    @Override
    public boolean checkType(PathSearchType pathSearchType) {
        return pathSearchType == PATH_TYPE_ANYONE;
    }

    @GeoLocationCache
    @Override
    public List<MixedRoute> getRoutes(PathSearchType type,double startLat,double startLon,double endLat,double endLon) {
        OdsayRouteResponse response = callRouteApi(PATH_TYPE_ANYONE.pathType,startLat,startLon,endLat,endLon);

        return parseMixedRoutes(response);
    }

    /**
     * 복합 경로의 모든 교통 수단 구간(첫 탑승 + 환승 지점)에 대해 탑승 정보를 계산한다.
     * <p>
     * SubwayOdsayService와 동일하게 desiredArrivalTime - totalTime 을 출발 기준점으로 삼고,
     * 지하철 구간은 subwayPathSchedule API(SID+EID+DAY+TIME)로 실제 열차 시각을 조회하여
     * currentDateTime 을 갱신하고, 버스 구간은 currentDateTime 을 latestBoardingTime으로 사용한다.
     *
     * @param route MixedRoute 타입이어야 한다.
     */
    @Override
    public MixedBoardingInfo getBoardingInfo(LocalDateTime desiredArrivalTime, TrafficResponse route) {
        MixedRoute mixedRoute = (MixedRoute) route;

        int dayCode = switch (desiredArrivalTime.getDayOfWeek()) {
            case SATURDAY -> 2;
            case SUNDAY   -> 3;
            default       -> 1;
        };

        LocalDateTime currentDateTime = desiredArrivalTime.minusMinutes(mixedRoute.getTotalTime());
        LocalTime recommendedDepartureTime = currentDateTime.toLocalTime();

        List<SegmentBoardingInfo> segmentBoardingInfos = new ArrayList<>();
        boolean firstTransitFound = false;
        int pendingWalkMinutes = 0;
        LocalTime pendingPlatformArrival = null;

        for (RouteSegment segment : mixedRoute.getSegments()) {
            if (segment instanceof WalkSegment walk) {
                pendingWalkMinutes = walk.getSectionTime();
                currentDateTime = currentDateTime.plusMinutes(walk.getSectionTime());
                if (firstTransitFound) {
                    pendingPlatformArrival = currentDateTime.toLocalTime();
                }
                continue;
            }

            boolean isTransferPoint = firstTransitFound;
            firstTransitFound = true;

            if (segment instanceof SubwayRoute.SubwaySegment subwaySeg) {
                String timeHHmm = currentDateTime.format(DateTimeFormatter.ofPattern("HHmm"));
                sleepQuietly(150);
                SubwayPathScheduleResponse scheduleResponse =
                        callPathScheduleApi(subwaySeg.getStartID(), subwaySeg.getEndId(), dayCode, timeHHmm);

                SubwayPathScheduleResponse.SubPath subwaySubPath = findSubwaySubPath(scheduleResponse);

                SegmentBoardingInfo segInfo = buildSubwaySegmentInfo(
                        subwaySubPath, subwaySeg, isTransferPoint,
                        pendingPlatformArrival, pendingWalkMinutes);

                if (segInfo != null) {
                    segmentBoardingInfos.add(segInfo);
                }

                if (subwaySubPath != null) {
                    LocalTime arrivalTime = parseHHmmss(subwaySubPath.getArrivalTime());
                    if (arrivalTime != null) {
                        LocalDateTime next = currentDateTime.toLocalDate().atTime(arrivalTime);
                        if (!next.isAfter(currentDateTime)) next = next.plusDays(1);
                        currentDateTime = next;
                    } else {
                        currentDateTime = currentDateTime.plusMinutes(subwaySeg.getSectionTime());
                    }
                } else {
                    currentDateTime = currentDateTime.plusMinutes(subwaySeg.getSectionTime());
                }

            } else if (segment instanceof BusRoute.BusSegment busSegment) {
                segmentBoardingInfos.add(new SegmentBoardingInfo(
                        TRAFFIC_TYPE_BUS.trafficNumber,
                        busSegment.getStartStop(),
                        busSegment.getBusNo(),
                        busSegment.getLocalBusId(),
                        busSegment.getBusProviderCode(),
                        busSegment.getLocalBusStationId(),
                        busSegment.getStationProviderCode(),
                        busSegment.getStartStopOrder(),
                        isTransferPoint,
                        currentDateTime.toLocalTime(),
                        new ArrayList<>(),
                        null,
                        null,
                        pendingWalkMinutes
                ));
                currentDateTime = currentDateTime.plusMinutes(busSegment.getSectionTime());
            }

            pendingWalkMinutes = 0;
            pendingPlatformArrival = null;
        }

        if (segmentBoardingInfos.isEmpty()) {
            throw new IllegalArgumentException("해당 경로에 교통 수단 구간이 존재하지 않습니다.");
        }

        return new MixedBoardingInfo(recommendedDepartureTime, segmentBoardingInfos);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private SubwayPathScheduleResponse callPathScheduleApi(int sid, int eid, int dayCode, String timeHHmm) {
        String url = UriComponentsBuilder.fromHttpUrl(SCHEDULE_SEARCH_URL)
                .queryParam("apiKey", odsayKey)
                .queryParam("lang", 0)
                .queryParam("SID", sid)
                .queryParam("EID", eid)
                .queryParam("MODE", 1)
                .queryParam("DAY", dayCode)
                .queryParam("TIME", timeHHmm)
                .toUriString();
        log.info("subway path schedule url: {}", url);
        return restTemplate.getForObject(url, SubwayPathScheduleResponse.class);
    }

    private SubwayPathScheduleResponse.SubPath findSubwaySubPath(SubwayPathScheduleResponse scheduleResponse) {
        if (scheduleResponse == null
                || scheduleResponse.getResult() == null
                || scheduleResponse.getResult().getPath() == null
                || scheduleResponse.getResult().getPath().isEmpty()) {
            return null;
        }
        SubwayPathScheduleResponse.Path path = scheduleResponse.getResult().getPath().stream()
                .filter(p -> p.getPathType() == PATH_TYPE_SHORTEST)
                .findFirst()
                .orElse(scheduleResponse.getResult().getPath().get(0));
        if (path.getSubPath() == null) return null;
        return path.getSubPath().stream()
                .filter(sp -> sp.getMovingType() == SUBWAY_MOVING_TYPE)
                .findFirst()
                .orElse(null);
    }

    private SegmentBoardingInfo buildSubwaySegmentInfo(
            SubwayPathScheduleResponse.SubPath subwaySubPath,
            SubwayRoute.SubwaySegment subwaySeg,
            boolean isTransferPoint,
            LocalTime pendingPlatformArrival,
            int pendingWalkMinutes) {

        if (subwaySubPath == null) {
            log.warn("지하철 subPath 없음: SID={}, EID={}", subwaySeg.getStartID(), subwaySeg.getEndId());
            return null;
        }

        LocalTime latestBoardingTime = parseHHmmss(subwaySubPath.getDepartureTime());
        String lineName = (subwaySubPath.getLaneName() != null && !subwaySubPath.getLaneName().isBlank())
                ? subwaySubPath.getLaneName()
                : subwaySeg.getLineName();
        String startStation = subwaySubPath.getStartName() != null
                ? subwaySubPath.getStartName()
                : subwaySeg.getStartStation();

        List<SubwayBoardingInfo.TrainSchedule> trains = new ArrayList<>();
        if (latestBoardingTime != null) {
            trains.add(new SubwayBoardingInfo.TrainSchedule(
                    latestBoardingTime,
                    subwaySubPath.getWayName(),
                    "Y".equals(subwaySubPath.getIsExpressLane()) ? 1 : 0,
                    0
            ));
        }

        return new SegmentBoardingInfo(
                TRAFFIC_TYPE_SUBWAY.trafficNumber,
                startStation,
                lineName,
                null, 0, null, 0, 0,
                isTransferPoint,
                latestBoardingTime,
                trains,
                new ArrayList<>(),
                isTransferPoint ? pendingPlatformArrival : null,
                pendingWalkMinutes
        );
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private LocalTime parseHHmmss(String hhmmss) {
        if (hhmmss == null || hhmmss.isBlank()) return null;
        try {
            String[] parts = hhmmss.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            return LocalTime.of(h % 24, m);
        } catch (Exception e) {
            log.warn("HH:mm:ss 파싱 실패: {}", hhmmss);
            return null;
        }
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
                            0, // ord는 BIS API에서 별도 조회
                            subPath.getStartLocalStationID(),
                            subPath.getStartStationProviderCode(),
                            lane.getBusLocalBlID(),
                            lane.getBusProviderCode()
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
                segments,
                info.getMapObj() != null ? info.getMapObj() : ""
        );
    }

}
