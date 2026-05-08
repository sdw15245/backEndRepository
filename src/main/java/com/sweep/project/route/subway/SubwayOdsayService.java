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
        log.info("최초 출발 시각:{}",currentDateTime);
        LocalTime recommendedDepartureTime = currentDateTime.toLocalTime();
        log.info("권장 출발 시각:{}",recommendedDepartureTime);

        int dayCode = switch (desiredArrivalTime.getDayOfWeek()) {
            case SATURDAY -> 2;
            case SUNDAY   -> 3;
            default       -> 1;
        };

        List<SegmentBoardingInfo> segmentBoardingInfos = new ArrayList<>();
        boolean firstSubwayFound = false;
        int pendingWalkMinutes = 0;
        LocalTime pendingPlatformArrival = null;

        List<RouteSegment> segments = subwayRoute.getSegments();
        log.info("=== getBoardingInfo 시작 === segments 총 개수:{}", segments.size());
        for (int i = 0; i < segments.size(); i++) {
            RouteSegment seg = segments.get(i);
            log.info("[Segment {}] type={}", i, seg.getClass().getSimpleName());
        }

        for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
            RouteSegment seg = segments.get(segIdx);

            if (seg instanceof WalkSegment walkSeg) {
                log.info("[Segment {}] WalkSegment - distance={}m, sectionTime={}분, 처리 후 currentDateTime={}",
                        segIdx, walkSeg.getDistance(), walkSeg.getSectionTime(),
                        currentDateTime.plusMinutes(walkSeg.getSectionTime()));
                // 도보 구간: 소요 시간만큼 currentDateTime 진행
                pendingWalkMinutes = walkSeg.getSectionTime();
                currentDateTime = currentDateTime.plusMinutes(walkSeg.getSectionTime());
                pendingPlatformArrival = currentDateTime.toLocalTime();

            } else if (seg instanceof SubwayRoute.SubwaySegment subwaySeg) {
                log.info("[Segment {}] SubwaySegment - 노선={}, subwayCode={}, 출발역={}(ID:{}), 도착역={}(ID:{}), 역수={}, 소요={}분, 거리={}m, wayCode={}",
                        segIdx, subwaySeg.getLineName(), subwaySeg.getSubwayCode(),
                        subwaySeg.getStartStation(), subwaySeg.getStartID(),
                        subwaySeg.getEndStation(), subwaySeg.getEndId(),
                        subwaySeg.getStationCount(), subwaySeg.getSectionTime(),
                        subwaySeg.getDistance(), subwaySeg.getWayCode());

                // 탑승역 도착 시각(= currentDateTime)을 출발 기준으로 조회
                String timeHHmm = currentDateTime.format(DateTimeFormatter.ofPattern("HHmm"));

                sleepQuietly(150);
                SubwayPathScheduleResponse scheduleResponse =
                        callScheduleApi(subwaySeg.getStartID(), subwaySeg.getEndId(), dayCode, timeHHmm);

                logScheduleResponse(scheduleResponse, segIdx);
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
            log.warn("스케줄 응답에서 지하철 구간 정보를 파싱할 수 없어 빈 결과를 반환합니다.");
            return new MixedBoardingInfo(recommendedDepartureTime, new ArrayList<>());
        }

        return new MixedBoardingInfo(recommendedDepartureTime, segmentBoardingInfos);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void logScheduleResponse(SubwayPathScheduleResponse scheduleResponse, int segIdx) {
        if (scheduleResponse == null || scheduleResponse.getResult() == null) {
            log.warn("[Segment {}] scheduleResponse 또는 result가 null", segIdx);
            return;
        }
        SubwayPathScheduleResponse.Result result = scheduleResponse.getResult();
        log.info("[Segment {}] ScheduleResponse - notificationCode={}, notificationMessage={}",
                segIdx, result.getNotificationCode(), result.getNotificationMessage());

        List<SubwayPathScheduleResponse.Path> paths = result.getPath();
        if (paths == null || paths.isEmpty()) {
            log.warn("[Segment {}] path 목록이 비어있음", segIdx);
            return;
        }
        log.info("[Segment {}] path 개수={}", segIdx, paths.size());

        for (int pi = 0; pi < paths.size(); pi++) {
            SubwayPathScheduleResponse.Path path = paths.get(pi);
            log.info("[Segment {}][Path {}] pathType={} (1=최단시간, 2=최소환승)", segIdx, pi, path.getPathType());

            SubwayPathScheduleResponse.Info info = path.getInfo();
            if (info != null) {
                log.info("[Segment {}][Path {}] Info - day={}, totalTime={}분, subwayTravelTime={}분, exchangeWalkTime={}분, " +
                                "첫출발역={}, 최종도착역={}, departureTime={}, arrivalTime={}, transferCount={}, cardFare={}원, stationCount={}",
                        segIdx, pi,
                        info.getDay(), info.getTotalTime(), info.getSubwayTravelTime(), info.getExchangeWalkTime(),
                        info.getFirstStartStationName(), info.getLastEndStationName(),
                        info.getDepartureTime(), info.getArrivalTime(),
                        info.getTransferCount(), info.getCardFare(), info.getStationCount());
            }

            List<SubwayPathScheduleResponse.SubPath> subPaths = path.getSubPath();
            if (subPaths == null || subPaths.isEmpty()) {
                log.warn("[Segment {}][Path {}] subPath 목록이 비어있음", segIdx, pi);
                continue;
            }
            log.info("[Segment {}][Path {}] subPath 개수={}", segIdx, pi, subPaths.size());

            for (int spi = 0; spi < subPaths.size(); spi++) {
                SubwayPathScheduleResponse.SubPath sp = subPaths.get(spi);
                String movingTypeName = switch (sp.getMovingType()) {
                    case SUBWAY_MOVING_TYPE -> "지하철";
                    case WALK_MOVING_TYPE   -> "환승도보";
                    default                 -> "기타(" + sp.getMovingType() + ")";
                };
                log.info("[Segment {}][Path {}][SubPath {}] movingType={}({}), 노선={}, " +
                                "startName={}(ID:{}), endName={}(ID:{}), sectionTime={}분, stopStationCount={}, " +
                                "departureTime={}, arrivalTime={}, wayName={}, isExpressLane={}, wayCode={}",
                        segIdx, pi, spi,
                        sp.getMovingType(), movingTypeName, sp.getLaneName(),
                        sp.getStartName(), sp.getStartID(),
                        sp.getEndName(), sp.getEndID(),
                        sp.getSectionTime(), sp.getStopStationCount(),
                        sp.getDepartureTime(), sp.getArrivalTime(),
                        sp.getWayName(), sp.getIsExpressLane(), sp.getWayCode());

                if (sp.getPrevTrain() != null) {
                    SubwayPathScheduleResponse.PrevNextTrain prev = sp.getPrevTrain();
                    log.info("[Segment {}][Path {}][SubPath {}] prevTrain - 노선={}, departureTime={}, wayName={}, isExpress={}",
                            segIdx, pi, spi, prev.getLaneName(), prev.getDepartureTime(), prev.getWayName(), prev.getIsExpressLane());
                }
                if (sp.getNextTrain() != null) {
                    SubwayPathScheduleResponse.PrevNextTrain next = sp.getNextTrain();
                    log.info("[Segment {}][Path {}][SubPath {}] nextTrain - 노선={}, departureTime={}, wayName={}, isExpress={}",
                            segIdx, pi, spi, next.getLaneName(), next.getDepartureTime(), next.getWayName(), next.getIsExpressLane());
                }
            }
        }
    }

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
            trains.add(new SubwayBoardingInfo.TrainSchedule(latestBoardingTime,
                    subwaySubPath.getWayName(), subwaySubPath.getIsExpressLane().equals("Y") ? 1:0, 0));
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
        log.info("time:{} sid:{} eid{}",timeHHmm,sid,eid);

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

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
