package com.sweep.project.route.domain;

import com.sweep.project.alarm.service.AlarmService;
import com.sweep.project.redis.RouteRedisService;
import com.sweep.project.route.BoardingInfo;
import com.sweep.project.route.TrafficResponse;
import com.sweep.project.route.dto.RouteTicketResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RouteTicket 기반 경로 단건 조회 서비스.
 *
 * <ul>
 *   <li>route 데이터: Redis → 미스 시 DB(Route.routeData) 재캐싱</li>
 *   <li>boarding: {@link PathSearchType#PATH_TYPE_SUBWAY} 만 Redis 단순 조회 (미스 시 null)</li>
 *   <li>bus / mixed: boarding 스킵</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RouteTicketService {

    private final RouteTicketRepository routeTicketRepository;
    private final RouteRedisService     routeRedisService;
    private final AlarmService          alarmService;

    @Transactional
    public void deleteRouteTicket(Long ticketId) {
        RouteTicket ticket = routeTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓: " + ticketId));
        ticket.updateDeleted();
        alarmService.deleteAlarmsByRouteTicket(ticketId);
    }

    public RouteTicketResponse getRoute(Long ticketId, LocalDateTime arrivalTime) {
        RouteTicket ticket = routeTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓: " + ticketId));
        return buildResponse(ticket, arrivalTime);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private RouteTicketResponse buildResponse(RouteTicket ticket, LocalDateTime arrivalTime) {
        Route          route    = ticket.getRoute();
        PathSearchType pathType = route.getType();   // 이미 enum

        // Route 엔티티: startX=lon, startY=lat
        double startLat = route.getStartY(), startLon = route.getStartX();
        double endLat   = route.getEndY(),   endLon   = route.getEndX();

        // 1. route 데이터: Redis HGET → 미스 시 DB routeData 재캐싱
        TrafficResponse routeData = routeRedisService
                .findOrCacheRoute(pathType, startLat, startLon, endLat, endLon,
                        route.getId(), route.getRouteData())
                .orElse(null);

        // 2. boarding: subway + arrivalTime 있을 때만 Redis 조회
        List<BoardingInfo> boardingInfos = null;
        if (pathType == PathSearchType.PATH_TYPE_SUBWAY && arrivalTime != null) {
            boardingInfos = fetchBoardingFromRedis(pathType, arrivalTime, startLat, startLon, endLat, endLon);
        }

        return RouteTicketResponse.builder()
                .ticketId(ticket.getId())
                .routeId(route.getId())
                .type(pathType)
                .routeData(routeData)
                .boardingInfos(boardingInfos)
                .build();
    }

    private List<BoardingInfo> fetchBoardingFromRedis(PathSearchType type,
                                                       LocalDateTime arrivalTime,
                                                       double startLat, double startLon,
                                                       double endLat, double endLon) {
        String timeHHmm = arrivalTime.format(DateTimeFormatter.ofPattern("HHmm"));
        int dayCode = switch (arrivalTime.getDayOfWeek()) {
            case SATURDAY -> 2;
            case SUNDAY   -> 3;
            default       -> 1;
        };
        return routeRedisService
                .findBoarding(type, timeHHmm, dayCode, startLat, startLon, endLat, endLon)
                .orElse(null);
    }
}
