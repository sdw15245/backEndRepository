package com.sweep.project.route.subway;

import com.sweep.project.route.*;
import com.sweep.project.route.mixed.MixedBoardingInfo;
import com.sweep.project.route.mixed.SegmentBoardingInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sweep.project.route.PathSearchType.PATH_TYPE_SUBWAY;
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
    public List<SubwayRoute> getRoutes(PathSearchType pathSearchType) {
        OdsayRouteResponse response = callRouteApi(PATH_TYPE_SUBWAY.pathType);
        return parseSubwayRoutes(response);
    }

    /**
     * subwayPathSchedule API를 활용해 지하철 구간 탑승 정보를 계산한다.
     *
     * <ol>
     *   <li>route 내 첫 번째 지하철 구간의 startID(SID)와 마지막 지하철 구간의 endId(EID) 추출</li>
     *   <li>subwayPathSchedule API 호출 (MODE=2: 도착 시각 기준)</li>
     *   <li>pathType=1(최단 시간) 경로 선택</li>
     *   <li>각 subPath에서 다음 정보 추출:
     *     <ul>
     *       <li>첫 탑승역 출발 시각(startDepartTime) → latestBoardingTime</li>
     *       <li>환승 도보 소요 시간(sectionTime of walk subPath) → transferWalkMinutes</li>
     *       <li>환승 열차 도착 시각(startArriveTime) → trainArrivalTime</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param route SubwayRoute 타입이어야 한다.
     * @return MixedBoardingInfo – segmentBoardingInfos 에 구간별 탑승 정보가 순서대로 담긴다.
     */
    @Override
    public MixedBoardingInfo getBoardingInfo(LocalDateTime desiredArrivalTime, TrafficResponse route) {
        SubwayRoute subwayRoute = (SubwayRoute) route;

        // 1. 지하철 구간 목록 추출
        List<SubwayRoute.SubwaySegment> subwaySegments = subwayRoute.getSegments().stream()
                .filter(s -> s instanceof SubwayRoute.SubwaySegment)
                .map(s -> (SubwayRoute.SubwaySegment) s)
                .collect(Collectors.toList());

        if (subwaySegments.isEmpty()) {
            throw new IllegalArgumentException("해당 경로에 지하철 구간이 존재하지 않습니다.");
        }

        // 2. SID = 첫 번째 지하철 구간 출발역, EID = 마지막 지하철 구간 도착역
        int sid = subwaySegments.get(0).getStartID();
        int eid = subwaySegments.get(subwaySegments.size() - 1).getEndId();

        // 3. 요일 코드 결정 (1=평일, 2=토요일, 3=일요일/공휴일)
        int dayCode = switch (desiredArrivalTime.getDayOfWeek()) {
            case SATURDAY -> 2;
            case SUNDAY   -> 3;
            default       -> 1;
        };

        // 4. 희망 도착 시각을 HHmm 형식으로 변환
        String timeHHmm = desiredArrivalTime.format(DateTimeFormatter.ofPattern("HHmm"));

        // 5. subwayPathSchedule API 호출
        SubwayPathScheduleResponse scheduleResponse = callScheduleApi(sid, eid, dayCode, timeHHmm);

        if (scheduleResponse == null
                || scheduleResponse.getResult() == null
                || scheduleResponse.getResult().getPath() == null
                || scheduleResponse.getResult().getPath().isEmpty()) {
            throw new IllegalStateException("지하철 경로 시간표 조회에 실패했습니다. SID=" + sid + ", EID=" + eid);
        }

        // 6. 최단 시간 경로(pathType=1) 선택, 없으면 첫 번째 경로 사용
        SubwayPathScheduleResponse.Path shortestPath = scheduleResponse.getResult().getPath().stream()
                .filter(p -> p.getPathType() == PATH_TYPE_SHORTEST)
                .findFirst()
                .orElse(scheduleResponse.getResult().getPath().get(0));

        // 7. 구간별 탑승 정보 구성
        List<SegmentBoardingInfo> segmentBoardingInfos = buildSegmentInfos(shortestPath, subwaySegments);

        if (segmentBoardingInfos.isEmpty()) {
            throw new IllegalStateException("스케줄 응답에서 지하철 구간 정보를 파싱할 수 없습니다.");
        }

        // 추천 출발 시각 = info.departureTime (스케줄 기반 실제 출발 시각)
        LocalTime recommendedDepartureTime = shortestPath.getInfo() != null
                ? parseHHmmss(shortestPath.getInfo().getDepartureTime())
                : null;
        if (recommendedDepartureTime == null) {
            recommendedDepartureTime = segmentBoardingInfos.get(0).getLatestBoardingTime();
        }
        if (recommendedDepartureTime == null) {
            recommendedDepartureTime = desiredArrivalTime.minusMinutes(subwayRoute.getTotalTime()).toLocalTime();
        }

        return new MixedBoardingInfo(recommendedDepartureTime, segmentBoardingInfos);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /**
     * 스케줄 API 응답의 subPath 목록을 순회하며 지하철 구간별 SegmentBoardingInfo를 생성한다.
     *
     * <p>실제 API 응답 기준:</p>
     * <ul>
     *   <li>환승도보 구간(movingType=2): sectionTime → transferWalkMinutes,
     *       arrivalTime → 환승 플랫폼 도착 시각(trainArrivalTime으로 저장)</li>
     *   <li>지하철 구간(movingType=1):
     *     <ul>
     *       <li>departureTime (HH:mm:ss) → latestBoardingTime</li>
     *       <li>laneName → 노선명</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    private List<SegmentBoardingInfo> buildSegmentInfos(
            SubwayPathScheduleResponse.Path path,
            List<SubwayRoute.SubwaySegment> subwaySegments) {

        List<SegmentBoardingInfo> result = new ArrayList<>();
        List<SubwayPathScheduleResponse.SubPath> subPaths = path.getSubPath();
        if (subPaths == null) return result;

        boolean firstSubwayFound = false;
        int pendingWalkMinutes = 0;
        LocalTime pendingPlatformArrival = null; // 환승 플랫폼 도착 시각
        int subwayIdx = 0;

        for (SubwayPathScheduleResponse.SubPath subPath : subPaths) {

            if (subPath.getMovingType() == WALK_MOVING_TYPE) {
                // 환승 도보 구간: 이동 시간과 플랫폼 도착 시각 보관
                pendingWalkMinutes = subPath.getSectionTime();
                pendingPlatformArrival = parseHHmmss(subPath.getArrivalTime());
                continue;
            }

            if (subPath.getMovingType() != SUBWAY_MOVING_TYPE) continue;

            boolean isTransferPoint = firstSubwayFound;
            firstSubwayFound = true;

            // 탑승 시각: 열차가 이 역에서 출발하는 시각 (HH:mm:ss)
            LocalTime latestBoardingTime = parseHHmmss(subPath.getDepartureTime());

            // 환승 지점: pendingPlatformArrival = 환승 플랫폼 도착 시각 (도보 구간 arrivalTime)
            LocalTime trainArrivalTime = isTransferPoint ? pendingPlatformArrival : null;

            // 노선명: laneName 직접 필드 우선, 없으면 원본 route 구간에서 보완
            String lineName = (subPath.getLaneName() != null && !subPath.getLaneName().isBlank())
                    ? subPath.getLaneName()
                    : (subwayIdx < subwaySegments.size() ? subwaySegments.get(subwayIdx).getLineName() : "");

            String startStation = subPath.getStartName() != null ? subPath.getStartName() : "";

            // 스케줄 API 응답의 열차 한 편을 TrainSchedule 형태로 래핑
            List<SubwayBoardingInfo.TrainSchedule> trains = new ArrayList<>();
            if (latestBoardingTime != null) {
                String endStation = subPath.getEndName() != null ? subPath.getEndName() : "";
                trains.add(new SubwayBoardingInfo.TrainSchedule(latestBoardingTime, endStation, 0, 0));
            }

            result.add(new SegmentBoardingInfo(
                    TRAFFIC_TYPE_SUBWAY.trafficNumber,
                    startStation,
                    lineName,
                    isTransferPoint,
                    latestBoardingTime,
                    trains,
                    new ArrayList<>(),
                    trainArrivalTime,
                    pendingWalkMinutes
            ));

            pendingWalkMinutes = 0;
            pendingPlatformArrival = null;
            subwayIdx++;
        }

        return result;
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
                segments
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
                .queryParam("MODE", 2)       // 2 = 도착 시각 기준 검색
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
