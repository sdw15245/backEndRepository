package com.sweep.project.utill;

import com.sweep.project.route.BoardingInfo;
import com.sweep.project.route.PathSearchType;
import com.sweep.project.route.TrafficResponse;
import com.sweep.project.route.redis.RouteRedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

@Component
@RequiredArgsConstructor
@Aspect
public class RedisOnBoardInfoAop {

    private final RouteRedisService routeRedisService;

    //구분기준 type,start 좌표,end좌표,hhmm 값
    @Around("@annotation(onBoardInfoAop)")
    public Object geoLocationCache(ProceedingJoinPoint joinPoint,OnBoardInfoAop onBoardInfoAop)
            throws Throwable{

        Object[] args = joinPoint.getArgs();
        PathSearchType type        = (PathSearchType) args[0];
        LocalDateTime arrivalTime  = (LocalDateTime)  args[1];
        // 좌표는 현재 HTTP 요청 파라미터에서 꺼냄 (메서드 시그니처에 불필요)
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
        Optional<List<BoardingInfo>> cached =
                routeRedisService.findBoarding(type,timeHHmm,dayCode, startLat, startLon, endLat, endLon);
        if (cached.isPresent()) {
            return cached.get();
        }
        List<BoardingInfo> result = (List<BoardingInfo>) joinPoint.proceed();
        // Lua 스크립트로 저장
        routeRedisService.saveBoardingIfAbsent(type,timeHHmm,dayCode,startLat, startLon, endLat, endLon, result);
        return result;
    }
}
