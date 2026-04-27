package com.sweep.project.route.subway;

import com.sweep.project.route.*;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.domain.WalkSegment;
import com.sweep.project.route.dto.RequestRouteDto;
import com.sweep.project.route.mixed.MixedBoardingInfo;
import com.sweep.project.route.mixed.SegmentBoardingInfo;
import com.sweep.project.util.GeoLocationCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sweep.project.route.domain.PathSearchType.PATH_TYPE_SUBWAY;
import static com.sweep.project.route.TrafficType.TRAFFIC_TYPE_HUMAN;
import static com.sweep.project.route.TrafficType.TRAFFIC_TYPE_SUBWAY;

@Slf4j
@Service
public class SubwayOdsayService extends AbstractRouteSearch {

    private static final int SUBWAY_MOVING_TYPE  = 1; // subwayPathSchedule API: movingType=1
    private static final int WALK_MOVING_TYPE    = 2; // subwayPathSchedule API: movingType=2 (환승도보)
    private static final int PATH_TYPE_SHORTEST  = 1; // 최단 시간 경로

    @Override
    public boolean checkType(PathSearchType pathSearchType) {
        return pathSearchType == PATH_TYPE_SUBWAY;
    }

    @Override
    @GeoLocationCache//-->다른 service에도 getroutes를 적용, 파라미터도 추후에 변경할 예정.
    public List<SubwayRoute> getRoutes(PathSearchType type,double startLat,double startLon,double endLat,double endLon) {
        OdsayRouteResponse response = callRouteApi(PATH_TYPE_SUBWAY.pathType,startLat,startLon,endLat,endLon);
        return parseSubwayRoutes(response);
    }

    @Override
    public MixedBoardingInfo getBoardingInfo(LocalDateTime desiredArrivalTime, TrafficResponse route) {
        SubwayRoute subwayRoute = (SubwayRoute) route;

        // 최초 출발 시각 = desiredArrivalTime - totalTime
        LocalDateTime currentDateTime = desiredArrivalTime.minusMinutes(subwayRoute.getTotalTime());
        LocalTime recommendedDepartureTime = currentDateTime.toLocalTime();

        int dayCode = switch (desiredArrivalTime.getDayOfWeek()) {
            case SATURDAY -> 2;
            case SUNDAY   -> 3;
            default       -> 1;
        };

        List<SegmentBoardingInfo> segmentBoardingInfos = new ArrayList<>();
        boolean firstSubwayFound = false;
        int pendingWalkMinutes = 0;
        LocalTime pendingPlatformArrival = null;

        for (RouteSegment seg : subwayRoute.getSegments()) {

            if (seg instanceof WalkSegment walkSeg) {
                // 도보 구간: 소요 시간만큼 currentDateTime 진행
                pendingWalkMinutes = walkSeg.getSectionTime();
                currentDateTime = currentDateTime.plusMinutes(walkSeg.getSectionTime());
                pendingPlatformArrival = currentDateTime.toLocalTime();

            } else if (seg instanceof SubwayRoute.SubwaySegment subwaySeg) {
                // 탑승역 도착 시각(= currentDateTime)을 출발 기준으로 조회
                String timeHHmm = currentDateTime.format(DateTimeFormatter.ofPattern("HHmm"));

                SubwayPathScheduleResponse scheduleResponse =
                        callScheduleApi(subwaySeg.getStartID(), subwaySeg.getEndId(), dayCode, timeHHmm);

                // 응답에서 지하철 subPath 추출 (departureTime·arrivalTime 사용)
                SubwayPathScheduleResponse.SubPath subwaySubPath = findSubwaySubPath(scheduleResponse);

                boolean isTransferPoint = firstSubwayFound;
                firstSubwayFound = true;

                SegmentBoardingInfo segInfo = buildSingleSegmentInfo(
                        subwaySubPath, subwaySeg, isTransferPoint,
                        pendingPlatformArrival, pendingWalkMinutes);

                if (segInfo != null) {
                    segmentBoardingInfos.add(segInfo);
                }

                // arrivalTime 기준으로 currentDateTime 갱신 (없으면 sectionTime 폴백)
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

                pendingWalkMinutes = 0;
                pendingPlatformArrival = null;
            }
        }

        if (segmentBoardingInfos.isEmpty()) {
            throw new IllegalStateException("스케줄 응답에서 지하철 구간 정보를 파싱할 수 없습니다.");
        }

        return new MixedBoardingInfo(recommendedDepartureTime, segmentBoardingInfos);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /** 스케줄 응답에서 pathType=1 경로의 첫 번째 지하철 subPath를 반환한다. */
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

    /**
     * 이미 추출된 subwaySubPath를 이용해 SegmentBoardingInfo를 생성한다.
     * departureTime → latestBoardingTime, arrivalTime은 호출부에서 currentDateTime 갱신에 사용된다.
     */
    private SegmentBoardingInfo buildSingleSegmentInfo(
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
            String endStation = subwaySubPath.getEndName() != null
                    ? subwaySubPath.getEndName()
                    : subwaySeg.getEndStation();
            trains.add(new SubwayBoardingInfo.TrainSchedule(latestBoardingTime, endStation, 0, 0));
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

    private List<SubwayRoute> parseSubwayRoutes(OdsayRouteResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getPath() == null) {
            return new ArrayList<>();
        }
        return response.getResult().getPath().stream()
                .filter(path -> path.getPathType() == PATH_TYPE_SUBWAY.pathType)
                .map(this::toSubwayRoute)
                .collect(Collectors.toList());
    }

    private SubwayRoute toSubwayRoute(OdsayRouteResponse.Path path) {
        OdsayRouteResponse.Info info = path.getInfo();
        List<RouteSegment> segments = new ArrayList<>();

        if (path.getSubPath() != null) {
            for (OdsayRouteResponse.SubPath subPath : path.getSubPath()) {
                if (subPath.getTrafficType() == TRAFFIC_TYPE_HUMAN.trafficNumber) {
                    segments.add(new WalkSegment(subPath.getDistance(), subPath.getSectionTime()));
                } else if (subPath.getTrafficType() == TRAFFIC_TYPE_SUBWAY.trafficNumber
                        && subPath.getLane() != null) {
                    for (OdsayRouteResponse.Lane lane : subPath.getLane()) {
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
                    }
                }
            }
        }

        int transferCount = Math.max(0, info.getBusTransitCount() + info.getSubwayTransitCount() - 1);
        return new SubwayRoute(
                info.getTotalTime(),
                info.getPayment(),
                transferCount,
                info.getSubwayTransitCount(),
                info.getTotalWalk(),
                segments,
                info.getMapObj() != null ? info.getMapObj() : ""
        );
    }

    /**
     * ODsay subwayPathSchedule API를 호출한다.
     *
     * @param sid       출발역 ID (첫 번째 지하철 구간 startID)
     * @param eid       도착역 ID (마지막 지하철 구간 endId)
     * @param dayCode   1=평일, 2=토요일, 3=일요일/공휴일
     * @param timeHHmm  도착 희망 시각 (HHmm 형식, MODE=2 도착 기준)
     */
    private SubwayPathScheduleResponse callScheduleApi(int sid, int eid, int dayCode, String timeHHmm) {
        String url = UriComponentsBuilder.fromHttpUrl(SCHEDULE_SEARCH_URL)
                .queryParam("apiKey", odsayKey)
                .queryParam("lang", 0)
                .queryParam("SID", sid)
                .queryParam("EID", eid)
                .queryParam("MODE", 1)       // 1 = 출발 시각 기준 검색
                .queryParam("DAY", dayCode)
                .queryParam("TIME", timeHHmm)
                .toUriString();
        log.info("subway path schedule url: {}", url);
        return restTemplate.getForObject(url, SubwayPathScheduleResponse.class);
    }

    /**
     * "HH:mm:ss" 형식 문자열을 LocalTime으로 변환한다.
     * 24시 이상(익일 열차)은 mod 24 처리한다.
     */
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
}
