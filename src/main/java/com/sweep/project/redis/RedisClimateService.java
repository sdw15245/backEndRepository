package com.sweep.project.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.climate.domain.ClimateApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisClimateService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "climate:";
    private static final long TTL_SECONDS = 15000L; // 5시간 10분

    private static final TypeReference<List<ClimateApiResponse.Item>> ITEM_LIST_TYPE =
            new TypeReference<>() {};

    // ── 키 ──────────────────────────────────────────────
    private String buildKey(int[] nxNy) {
        return KEY_PREFIX + nxNy[0] + ":" + nxNy[1];
    }
    //서브키는 20260420_0500  이런꼴.

    // ── 저장 ─────────────────────────────────────────────
    public void saveClimateInfo(int[] nxNy, Map<String, List<ClimateApiResponse.Item>> data) {
        String key = buildKey(nxNy);
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();

        Map<String, String> serialized = new HashMap<>();
        for (Map.Entry<String, List<ClimateApiResponse.Item>> entry : data.entrySet()) {
            try {
                serialized.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("기후 데이터 직렬화 실패: " + entry.getKey(), e);
            }
        }

        hashOps.putAll(key, serialized);
        stringRedisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
    }

    // ── 서브키(fcstDate_fcstTime) 단건 조회 ───────────────
    public List<ClimateApiResponse.Item> getClimateInfoByTime(int[] nxNy, String fcstKey) {
        String json = (String) stringRedisTemplate.opsForHash().get(buildKey(nxNy), fcstKey);
        if (json == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, ITEM_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("기후 데이터 역직렬화 실패: " + fcstKey, e);
        }
    }
}
