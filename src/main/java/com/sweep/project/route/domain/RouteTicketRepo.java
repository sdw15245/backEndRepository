package com.sweep.project.route.domain;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.commons.lang.Assert;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sweep.project.route.batch.RouteBatchDto;
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

import static com.sweep.project.route.domain.QRoute.*;
import static com.sweep.project.route.domain.QRouteTicket.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RouteTicketRepo {

    private final JPAQueryFactory jpaQueryFactory;
    private final ObjectMapper objectMapper;

    public List<Long> getMinMaxId(LocalDateTime now) {
        LocalDateTime twoMonthsAgo = now.minusMonths(2).toLocalDate().atStartOfDay();

        Tuple tuple = jpaQueryFactory
                .select(route.id.min(), route.id.max())
                .from(routeTicket)
                .join(route).on(route.eq(routeTicket.route))
                .where(routeTicket.deleted.isFalse()
                        .and(routeTicket.createdAt.before(twoMonthsAgo)))
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
                .from(routeTicket)
                .join(route).on(route.eq(routeTicket.route))
                .where(route.id.between(minId, maxId)
                        .and(route.id.gt(lastId)))
                .orderBy(route.id.asc())
                .limit(pageSize)
                .fetch();
    }

    private final JdbcTemplate jdbcTemplate;

    public void updateRouteBatch(Map<Long,String> id_JsonMap, LocalDateTime updateAt) {

        String sql = "UPDATE route SET route_data = ?, total_time = ?, create_date = ? WHERE id = ?";
        Timestamp ts = Timestamp.valueOf(updateAt);

        List<Map.Entry<Long, String>> entries = new ArrayList<>(id_JsonMap.entrySet());

        //오디세이에서 불러오는 결과는 확정되게 3개를 불러오는게 아니므로
        //만약 route id값에 비해서 오디세이에서 불러온값이 적다면은 빈칸은 null처리가 필요
        //만약 null처리된 route를 참조하는 routeticket이 존재한다면  남아잇는 애들중에서 업데이트하게하고
        //null처리 된값은 그대로 남기는 것이 필요할듯-->나중에 갯수가 늘어날수잇으니.

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String json = entries.get(i).getValue();
                ps.setString(1, json);  // null이면 DB NULL로 저장
                ps.setObject(2, parseTotalTime(json));  // null이면 DB NULL로 저장
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
     * 이번 배치(updateAt)에서 route_data가 null로 업데이트된 route를 참조하는
     * 삭제되지 않은 route_ticket의 id min/max를 반환한다.
     */
    public List<Long> getNullRouteTicketMinMaxId(LocalDateTime updateAt) {
        com.querydsl.core.Tuple tuple = jpaQueryFactory
                .select(routeTicket.id.min(), routeTicket.id.max())
                .from(routeTicket)
                .join(routeTicket.route, route)
                .where(route.routeData.isNull()
                        .and(route.createDate.eq(updateAt))
                        .and(routeTicket.deleted.isFalse()))
                .fetchOne();

        if (tuple == null) {
            return Arrays.asList(null, null);
        }
        return Arrays.asList(tuple.get(routeTicket.id.min()), tuple.get(routeTicket.id.max()));
    }

    /**
     * 커서 기반 페이지네이션으로 대상 route_ticket id 목록을 스트리밍한다.
     */
    public List<Long> fetchNullRouteTicketPage(Long minId, Long maxId, Long lastId,
                                               int pageSize, LocalDateTime updateAt) {
        return jpaQueryFactory
                .select(routeTicket.id)
                .from(routeTicket)
                .join(routeTicket.route, route)
                .where(route.routeData.isNull()
                        .and(route.createDate.eq(updateAt))
                        .and(routeTicket.deleted.isFalse())
                        .and(routeTicket.id.between(minId, maxId))
                        .and(routeTicket.id.gt(lastId)))
                .orderBy(routeTicket.id.asc())
                .limit(pageSize)
                .fetch();
    }

    /**
     * route_ticket의 need_check를 true로 일괄 업데이트한다.
     */
    public void updateNeedCheckBatch(List<? extends Long> ids) {
        String sql = "UPDATE route_ticket SET need_check = true WHERE id = ?";
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
