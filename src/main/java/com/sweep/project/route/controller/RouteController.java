package com.sweep.project.route.controller;

import com.sweep.project.route.bus.BusArrivalInfo;
import com.sweep.project.route.bus.BusArrivalService;
import com.sweep.project.route.*;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.domain.Route;
import com.sweep.project.route.domain.RouteDbService;
import com.sweep.project.route.domain.RouteResponse;
import com.sweep.project.redis.RouteRedisService;
import com.sweep.project.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
@Slf4j
public class RouteController {

    private final TrafficRouteStragy trafficRouteStragy;
    private final BusArrivalService busArrivalService;
    private final RouteDbService routeDbService;
    private final RouteRedisService routeRedisService;

    /**
     * 버스 도착 정보 조회.
     * GET /route/bus/arrival?stId=&busRouteId=&ord=&providerCode=
     *
     * providerCode: 2 = 경기도, 4 = 서울 (기본값)
     * ord가 0이면 BIS API를 통해 자동 조회한다.
     */
    @Operation(summary = "버스의 위치를 조회",description = "특정 노선의 버스가 특정 정류장에 대한 도착정보를 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    headers = {
                            @Header(name = "Authorization", description = "Bearer [Access JWT 토큰]",
                                    schema = @Schema(type = "string")),
                    },
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "권한이 부족합니다.",
                    content = @Content(schema = @Schema()))
    })
    @GetMapping("/bus/arrival")
    public ApiResponseUtil<BusArrivalInfo> getBusArrival(
            @Parameter(description = "버스 정류소 ID (BIS 기준)", example = "100000080", required = true)
            @RequestParam String stId,
            @Parameter(description = "버스 노선 ID (BIS 기준)", example = "100100118", required = true)
            @RequestParam String busRouteId,
            @Parameter(description = "노선 내 정류소 순번. 0이면 BIS API로 자동 조회", example = "0")
            @RequestParam(defaultValue = "0") int ord,
            @Parameter(description = "버스 정보 제공 기관 코드. 2=경기도, 4=서울(기본값)", example = "4")
            @RequestParam(defaultValue = "4") int providerCode) {
        return ApiResponseUtil.SuccessApiResponse("ok",busArrivalService.getBusArrival(stId, busRouteId, ord, providerCode));
    }

    /**
     * 탑승 정보 조회.
     * GET /route/boarding?type=PATH_TYPE_SUBWAY&arrivalTime=2024-06-01T09:00:00
     */
    @Operation(summary = "도착지-목적지 루트 조회",description = "도착지 목적지 간의 교통수단에 따른 루트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    headers = {
                            @Header(name = "Authorization", description = "Bearer [Access JWT 토큰]",
                                    schema = @Schema(type = "string")),
                    },
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "권한이 부족합니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "요청시 토큰값을 넣어주셔야됩니다.",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @GetMapping("/boarding")
    public ApiResponseUtil<RouteResponse> getBoardingInfo(
            @Parameter(description = "경로 탐색 유형. PATH_TYPE_ANYONE, PATH_TYPE_SUBWAY, PATH_TYPE_BUS", example = "PATH_TYPE_SUBWAY", required = true)
            @RequestParam PathSearchType type,
            @Parameter(description = "출발지 위도", example = "37.5665", required = true)
            @RequestParam double startLat,
            @Parameter(description = "출발지 경도", example = "126.9780", required = true)
            @RequestParam double startLon,
            @Parameter(description = "목적지 위도", example = "37.4979", required = true)
            @RequestParam double endLat,
            @Parameter(description = "목적지 경도", example = "127.0276", required = true)
            @RequestParam double endLon,
            @Parameter(description = "목적지 도착 희망 시각 (ISO 8601 형식)", example = "2024-06-01T09:00:00", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime) {
        List<? extends TrafficResponse> routes = trafficRouteStragy.getRoutes(type, startLat, startLon, endLat, endLon);
        if (routes.isEmpty()) {
            return ApiResponseUtil.SuccessApiResponse("ok", new RouteResponse(null, null));
        }
        List<BoardingInfo> boardingInfos = trafficRouteStragy.getBoardingInfo(type, arrivalTime, routes);
        return ApiResponseUtil.SuccessApiResponse("ok",new RouteResponse(routes, boardingInfos));
    }


    @Operation(summary = "알람 리스트 목록조회시에 subway타입인 지하철 시간표를 조회해서 보여줌. latestBoardingTime  이컬럼을 쓰씨면될거같습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    headers = {
                            @Header(name = "Authorization", description = "Bearer [Access JWT 토큰]",
                                    schema = @Schema(type = "string")),
                    },
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "권한이 부족합니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "요청시 토큰값을 넣어주셔야됩니다.",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @GetMapping("/detail/{id}")
    public ApiResponseUtil<List<BoardingInfo>> getSubwayBoardingInfo(
           @PathVariable(name = "id") Long id,
           @Parameter(description = "경로 탐색 유형. PATH_TYPE_ANYONE, PATH_TYPE_SUBWAY, PATH_TYPE_BUS", example = "PATH_TYPE_SUBWAY", required = true)
           @RequestParam PathSearchType type,
           @Parameter(description = "출발지 위도", example = "37.5665", required = true)
           @RequestParam double startLat,
           @Parameter(description = "출발지 경도", example = "126.9780", required = true)
           @RequestParam double startLon,
           @Parameter(description = "목적지 위도", example = "37.4979", required = true)
           @RequestParam double endLat,
           @Parameter(description = "목적지 경도", example = "127.0276", required = true)
           @RequestParam double endLon,
           @Parameter(description = "목적지 도착 희망 시각 (ISO 8601 형식)", example = "2024-06-01T09:00:00", required = true)
           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime){

        String timeHHmm = arrivalTime.format(DateTimeFormatter.ofPattern("HHmm"));
        int dayCode = switch (arrivalTime.getDayOfWeek()) {
            case SATURDAY -> 2;
            case SUNDAY   -> 3;
            default       -> 1;
        };

        // 1. 기존 boarding hash 에서 routeId 단건 조회
        Optional<BoardingInfo> cached =
                routeRedisService.findBoardingById(type, timeHHmm, dayCode, startLat, startLon, endLat, endLon, id);
        if (cached.isPresent()) {
            return ApiResponseUtil.SuccessApiResponse("ok", List.of(cached.get()));
        }

        // 2. 캐시 미스 → DB에서 TrafficResponse 조회
        List<TrafficResponse> trafficResponses = routeDbService.findById(id);
        if (trafficResponses.isEmpty()) {
            return ApiResponseUtil.SuccessApiResponse("ok", List.of());
        }

        // 3. AOP 없이 단건 boarding info 계산
        BoardingInfo boardingInfo =
                trafficRouteStragy.getBoardingInfoSingle(type, arrivalTime, trafficResponses.get(0));

        // 4. 기존 boarding hash 에 routeId 기준으로 저장 (AOP 캐시와 동일한 키 구조 공유)
        routeRedisService.saveBoardingById(
                type, timeHHmm, dayCode, startLat, startLon, endLat, endLon, id, boardingInfo);

        return ApiResponseUtil.SuccessApiResponse("ok", List.of(boardingInfo));
    }
}
