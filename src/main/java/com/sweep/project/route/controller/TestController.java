package com.sweep.project.route.controller;

import com.sweep.project.route.bus.BusArrivalInfo;
import com.sweep.project.route.bus.BusArrivalService;
import com.sweep.project.route.*;
import com.sweep.project.route.domain.ApiResponse;
import com.sweep.project.route.domain.PathSearchType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final TrafficRouteStragy trafficRouteStragy;
    private final BusArrivalService busArrivalService;

    /**
     * 버스 도착 정보 조회.
     * GET /route/bus/arrival?stId=&busRouteId=&ord=&providerCode=
     *
     * providerCode: 2 = 경기도, 4 = 서울 (기본값)
     * ord가 0이면 BIS API를 통해 자동 조회한다.
     */
    @GetMapping("/bus/arrival")
    public BusArrivalInfo getBusArrival(
            @RequestParam String stId,
            @RequestParam String busRouteId,
            @RequestParam(defaultValue = "0") int ord,
            @RequestParam(defaultValue = "4") int providerCode) {
        return busArrivalService.getBusArrival(stId, busRouteId, ord, providerCode);
    }

    /**
     * 탑승 정보 조회.
     * GET /route/boarding?type=PATH_TYPE_SUBWAY&arrivalTime=2024-06-01T09:00:00
     */
    @GetMapping("/boarding")
    public ApiResponse getBoardingInfo(
            @RequestParam PathSearchType type,
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double endLat,
            @RequestParam double endLon,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime) {
        List<? extends TrafficResponse> routes = trafficRouteStragy.getRoutes(type, startLat, startLon, endLat, endLon);
        if (routes.isEmpty()) {
            return new ApiResponse(null, null);
        }
        List<BoardingInfo> boardingInfos = trafficRouteStragy.getBoardingInfo(type, arrivalTime, routes);
        return new ApiResponse(routes, boardingInfos);
    }
}
