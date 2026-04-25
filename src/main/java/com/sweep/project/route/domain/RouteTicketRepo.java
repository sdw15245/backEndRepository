package com.sweep.project.route.domain;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.Tuple;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sweep.project.fcm.domain.QFcmToken;
import com.sweep.project.route.batch.RouteBatchDto;
import com.sweep.project.route.dto.NeedCheckAlertDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sweep.project.alarm.domain.QAlarm.alarm;
import static com.sweep.project.fcm.domain.QFcmToken.fcmToken;
import static com.sweep.project.route.domain.QRoute.route;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RouteTicketRepo {

    private final JPAQueryFactory jpaQueryFactory;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public List<Long> getMinMaxId(LocalDateTime now) {
        LocalDateTime twoMonthsAgo = now.minusMonths(2).toLocalDate().atStartOfDay();

        Tuple tuple = jpaQueryFactory
                .select(route.id.min(), route.id.max())
                .from(route)
                .where(route.createDate.before(twoMonthsAgo))
                .fetchOne();

        if (tuple == null) {
            return Arrays.asList(null, null);
        }
        return Arrays.asList(tuple.get(route.id.min()), tuple.get(route.id.max()));
    }

    public List<RouteBatchDto> fetchPage(Long minId, Long maxId, Long lastId, int pageSize) {
        return jpaQueryFactory
                .select(Projections.constructor(RouteBatchDto.class,
                        route.id,
                        route.type,
                        route.startX,
                        route.startY,
                        route.endX,
                        route.endY))
                .where(route.id.between(minId, maxId)
                        .and(route.id.gt(lastId)))
                .orderBy(route.id.asc())
                .limit(pageSize)
                .fetch();
    }

    public void updateRouteBatch(Map<Long, String> id_JsonMap, LocalDateTime updateAt) {

        String sql = "UPDATE route SET route_data = ?, total_time = ?, create_date = ? WHERE id = ?";
        Timestamp ts = Timestamp.valueOf(updateAt);

        List<Map.Entry<Long, String>> entries = new ArrayList<>(id_JsonMap.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String json = entries.get(i).getValue();
                ps.setString(1, json);
                ps.setObject(2, parseTotalTime(json));
                ps.setTimestamp(3, ts);
                ps.setLong(4, entries.get(i).getKey());
            }
            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });
    }

    private Integer parseTotalTime(String routeJson) {
        if (routeJson == null) return null;
        try {
            return objectMapper.readTree(routeJson).path("totalTime").asInt(0);
        } catch (Exception e) {
            log.warn("[RouteTicketRepo] totalTime 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    // ── NullRouteCheck step 전용 ───────────────────────────────────────────────

    /**
     * route가 업데이트 된 alaram id들만 추출.
     *
     * */
    public List<Long> getAlarmNeedCheckMinMaxId(LocalDateTime updateAt) {
        Tuple tuple = jpaQueryFactory
                .select(alarm.alarmId.min(), alarm.alarmId.max())
                .from(alarm)
                .join(alarm.route, route)
                .where(route.createDate.eq(updateAt)
                        .and(alarm.deleted.isFalse()))
                .fetchOne();

        if (tuple == null) {
            return Arrays.asList(null, null);
        }
        return Arrays.asList(tuple.get(alarm.alarmId.min()), tuple.get(alarm.alarmId.max()));
    }

    /**
     * 정해진 범위 내의 alarm id값들만 추출.
     *
     * */
    public List<Long> fetchAlarmNeedCheckPage(Long minId, Long maxId, Long lastId,
                                         int pageSize, LocalDateTime updateAt) {
        return jpaQueryFactory
                .select(alarm.alarmId)
                .from(alarm)
                .join(alarm.route, route)
                .where(alarm.alarmId.between(minId, maxId)
                        .and(alarm.alarmId.gt(lastId)))
                .orderBy(alarm.alarmId.asc())
                .limit(pageSize)
                .fetch();
    }

    /**
     * 추출된 알람들과 연관된 fcm token들 추출
     *
     * */
    public List<NeedCheckAlertDto> getFcmTokenFromAlarm(List<? extends Long> ids) {
        Map<Long, List<String>> grouped = jpaQueryFactory
                .from(alarm)
                .join(fcmToken).on(fcmToken.memberId.eq(alarm.member.id))
                .where(alarm.alarmId.in(ids))
                .transform(GroupBy.groupBy(alarm.alarmId).as(GroupBy.list(fcmToken.token)));

        return grouped.entrySet().stream()
                .map(e -> new NeedCheckAlertDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public void updateNeedCheckBatch(List<? extends Long> ids) {
        String sql = "UPDATE alarm SET need_check = true WHERE alarm_id = ?";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, ids.get(i));
            }
            @Override
            public int getBatchSize() {
                return ids.size();
            }
        });
    }
}
