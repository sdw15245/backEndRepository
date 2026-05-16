package com.sweep.project.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.route.preview.dto.RoutePreviewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutePreviewRedisService {

    private static final String MAP_OBJ_KEY_PREFIX = "preview:v1:";
    private static final String ROUTE_ID_KEY_PREFIX = "route:preview:v1:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<RoutePreviewDto> find(String decodedMapObj) {
        return findByKey(MAP_OBJ_KEY_PREFIX + decodedMapObj);
    }

    public void save(String decodedMapObj, RoutePreviewDto dto) {
        saveByKey(MAP_OBJ_KEY_PREFIX + decodedMapObj, dto);
    }

    public Optional<RoutePreviewDto> findByRouteId(Long routeId) {
        return findByKey(ROUTE_ID_KEY_PREFIX + routeId);
    }

    public void saveByRouteId(Long routeId, RoutePreviewDto dto) {
        saveByKey(ROUTE_ID_KEY_PREFIX + routeId, dto);
    }

    private Optional<RoutePreviewDto> findByKey(String key) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            log.debug("[Redis][preview] cache miss key={}", key);
            return Optional.empty();
        }
        try {
            RoutePreviewDto dto = objectMapper.readValue(json, RoutePreviewDto.class);
            log.info("[Redis][preview] cache hit key={}", key);
            return Optional.of(dto);
        } catch (JsonProcessingException e) {
            log.error("[Redis][preview] deserialize failed - ignore cache key={}", key, e);
            return Optional.empty();
        }
    }

    private void saveByKey(String key, RoutePreviewDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            long ttl = secondsUntilMidnight();
            Boolean written = stringRedisTemplate.opsForValue().setIfAbsent(key, json, ttl, TimeUnit.SECONDS);
            log.info("[Redis][preview] save key={} ttl={}s written={}", key, ttl, written);
        } catch (JsonProcessingException e) {
            log.error("[Redis][preview] serialize failed - skip cache key={}", key, e);
        }
    }

    private long secondsUntilMidnight() {
        int seconds = LocalTime.of(23, 59, 59).toSecondOfDay() - LocalTime.now().toSecondOfDay();
        return Math.max(seconds, 60);
    }
}
