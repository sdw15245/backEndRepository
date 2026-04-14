package com.sweep.project.util;

import com.sweep.project.route.BoardingInfo;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.redis.RouteRedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Aspect
@Slf4j
public class RedisOnBoardInfoAop {

    private final RouteRedisService routeRedisService;

    @Around("@annotation(onBoardInfoAop)")
    public Object geoLocationCache(ProceedingJoinPoint joinPoint, OnBoardInfoAop onBoardInfoAop)
            throws Throwable {

        Object[] args = joinPoint.getArgs();
        PathSearchType type       = (PathSearchType) args[0];
        LocalDateTime arrivalTime = (LocalDateTime)  args[1];

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();

        double startLat = Double.parseDouble(request.getParameter("startLat"));
        double startLon = Double.parseDouble(request.getParameter("startLon"));
        double endLat   = Double.parseDouble(request.getParameter("endLat"));
        double endLon   = Double.parseDouble(request.getParameter("endLon"));

        String timeHHmm = arrivalTime.format(DateTimeFormatter.ofPattern("HHmm"));
        int dayCode = switch (arrivalTime.getDayOfWeek()) {
            case SATURDAY -> 2;
            case SUNDAY   -> 3;
            default       -> 1;
        };

        // 1. boarding Hash 캐시 조회
        Optional<List<BoardingInfo>> cached =
                routeRedisService.findBoarding(type, timeHHmm, dayCode, startLat, startLon, endLat, endLon);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. 캐시 미스 → API 호출
        @SuppressWarnings("unchecked")
        List<BoardingInfo> result = (List<BoardingInfo>) joinPoint.proceed();

        // 3. route Hash 에서 routeId 목록 조회 (boarding subkey 로 사용)
        List<Long> routeIds = routeRedisService.getRouteIds(type, startLat, startLon, endLat, endLon);
        if (routeIds.isEmpty()) {
            log.warn("[BoardingCache] route Hash 에 routeId 없음 — boarding 캐싱 생략 type={}", type);
            return result;
        }

        // 4. boarding Hash 에 저장 (field = routeId, route 와 동일한 순서·ID)
        routeRedisService.saveBoardingIfAbsent(
                type, timeHHmm, dayCode, startLat, startLon, endLat, endLon, result, routeIds);

        return result;
    }
}
