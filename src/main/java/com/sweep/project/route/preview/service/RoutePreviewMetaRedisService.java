package com.sweep.project.route.preview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.route.RouteSegment;
import com.sweep.project.route.TrafficResponse;
import com.sweep.project.route.bus.BusRoute;
import com.sweep.project.route.domain.WalkSegment;
import com.sweep.project.route.mixed.MixedRoute;
import com.sweep.project.route.preview.dto.RoutePreviewMetaDto;
import com.sweep.project.route.subway.SubwayRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutePreviewMetaRedisService {

    private static final String KEY_PREFIX = "route:preview-meta:v1:";
    private static final long TTL_MINUTES = 30;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void saveRouteMetas(List<? extends TrafficResponse> routes) {
        for (TrafficResponse route : routes) {
            saveRouteMeta(route);
        }
    }

    public Optional<RoutePreviewMetaDto> find(String routePreviewId) {
        String key = buildKey(routePreviewId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            log.debug("[Redis][preview-meta] cache miss key={}", key);
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, RoutePreviewMetaDto.class));
        } catch (JsonProcessingException e) {
            log.error("[Redis][preview-meta] deserialize failed key={}", key, e);
            return Optional.empty();
        }
    }

    private void saveRouteMeta(TrafficResponse route) {
        RoutePreviewMetaDto meta = toMeta(route);
        if (meta.getMapObj() == null || meta.getMapObj().isBlank()) {
            return;
        }
        /**
         *
         * /route/boarding 진행 할때마다 새로운 uuid값으로 데이터가 저장되는 구조인대
         * 이러면 너무많이 데이터가 쌓이기도하고, trafficresponse를 캐싱해둔 데이터하고 연결되지가 않는다.
         * 그냥 mapobj값을 키값으로 하는게 좋아보이는대.
         * */
        String routePreviewId = UUID.randomUUID().toString();
        assignRoutePreviewId(route, routePreviewId);

        try {
            String json = objectMapper.writeValueAsString(meta);
            stringRedisTemplate.opsForValue()
                    .set(buildKey(routePreviewId), json, TTL_MINUTES, TimeUnit.MINUTES);
            log.info("[Redis][preview-meta] saved routePreviewId={} ttl={}m", routePreviewId, TTL_MINUTES);
        } catch (JsonProcessingException e) {
            log.error("[Redis][preview-meta] serialize failed routePreviewId={}", routePreviewId, e);
        }
    }

    private RoutePreviewMetaDto toMeta(TrafficResponse route) {
        if (route instanceof SubwayRoute subwayRoute) {
            return RoutePreviewMetaDto.builder()
                    .mapObj(subwayRoute.getMapObj())
                    .segments(toCachedSegments(subwayRoute.getSegments()))
                    .build();
        }
        if (route instanceof BusRoute busRoute) {
            return RoutePreviewMetaDto.builder()
                    .mapObj(busRoute.getMapObj())
                    .segments(toCachedSegments(busRoute.getSegments()))
                    .build();
        }
        if (route instanceof MixedRoute mixedRoute) {
            return RoutePreviewMetaDto.builder()
                    .mapObj(mixedRoute.getMapObj())
                    .segments(toCachedSegments(mixedRoute.getSegments()))
                    .build();
        }
        return RoutePreviewMetaDto.builder().segments(List.of()).build();
    }

    private List<RoutePreviewMetaDto.CachedSegmentDto> toCachedSegments(List<RouteSegment> routeSegments) {
        if (routeSegments == null) {
            return List.of();
        }

        List<RoutePreviewMetaDto.CachedSegmentDto> cachedSegments = new ArrayList<>();
        for (RouteSegment segment : routeSegments) {
            cachedSegments.add(toCachedSegment(segment));
        }
        return cachedSegments;
    }

    private RoutePreviewMetaDto.CachedSegmentDto toCachedSegment(RouteSegment segment) {
        if (segment instanceof SubwayRoute.SubwaySegment subwaySegment) {
            return RoutePreviewMetaDto.CachedSegmentDto.builder()
                    .trafficType(subwaySegment.getTrafficType())
                    .laneName(subwaySegment.getLineName())
                    .startName(subwaySegment.getStartStation())
                    .endName(subwaySegment.getEndStation())
                    .build();
        }
        if (segment instanceof BusRoute.BusSegment busSegment) {
            return RoutePreviewMetaDto.CachedSegmentDto.builder()
                    .trafficType(busSegment.getTrafficType())
                    .laneName(busSegment.getBusNo())
                    .busType(busSegment.getBusType())
                    .startName(busSegment.getStartStop())
                    .endName(busSegment.getEndStop())
                    .build();
        }
        if (segment instanceof WalkSegment) {
            return RoutePreviewMetaDto.CachedSegmentDto.builder()
                    .trafficType(segment.getTrafficType())
                    .laneName("")
                    .build();
        }
        return RoutePreviewMetaDto.CachedSegmentDto.builder()
                .trafficType(segment.getTrafficType())
                .laneName("")
                .build();
    }

    private void assignRoutePreviewId(TrafficResponse route, String routePreviewId) {
        if (route instanceof SubwayRoute subwayRoute) {
            subwayRoute.setRoutePreviewId(routePreviewId);
        } else if (route instanceof BusRoute busRoute) {
            busRoute.setRoutePreviewId(routePreviewId);
        } else if (route instanceof MixedRoute mixedRoute) {
            mixedRoute.setRoutePreviewId(routePreviewId);
        }
    }

    private String buildKey(String routePreviewId) {
        return KEY_PREFIX + routePreviewId;
    }
}
