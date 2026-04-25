package com.sweep.project.alarm.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sweep.project.alarm.batch.AlarmBatchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

import static com.sweep.project.alarm.domain.QAlarm.alarm;
import static com.sweep.project.route.domain.QRoute.route;

/**
 * 알람 배치 전용 QueryDSL 레포지토리.
 *
 * <p>조인 구조
 * <pre>
 *   alarm
 *     └─ join alarm.route  (Route)
 * </pre>
 * 조건: alarm.deleted = false
 */
@Repository
@RequiredArgsConstructor
public class AlarmTicketRepo {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 활성 Alarm 의 alarmId min/max 를 반환한다.
     *
     * @param todayKo 오늘 요일 한글 약자 (예: "월", "화") — day 필드 포함 여부 필터에 사용
     */
    public List<Long> getActiveAlarmMinMaxId(String todayKo) {
        Tuple tuple = jpaQueryFactory
                .select(alarm.alarmId.min(), alarm.alarmId.max())
                .from(alarm)
                .where(alarm.deleted.isFalse()
                        .and(alarm.isLoop.isTrue())
                        .and(alarm.day.contains(todayKo)))
                .fetchOne();

        if (tuple == null) {
            return Arrays.asList(null, null);
        }
        return Arrays.asList(
                tuple.get(alarm.alarmId.min()),
                tuple.get(alarm.alarmId.max())
        );
    }

    /**
     * zero-offset cursor 기반 페이지 조회.
     *
     * @param minId    파티션 하한 alarmId (포함)
     * @param maxId    파티션 상한 alarmId (포함)
     * @param lastId   직전 페이지 마지막 alarmId (다음 페이지 시작 커서)
     * @param pageSize 한 번에 읽을 행 수
     * @param todayKo  오늘 요일 한글 약자 — day 필드 포함 여부 필터에 사용
     */
    public List<AlarmBatchDto> fetchActiveAlarmPage(Long minId, Long maxId, Long lastId,
                                                    int pageSize, String todayKo) {
        return jpaQueryFactory
                .select(Projections.constructor(AlarmBatchDto.class,
                        alarm.alarmId,
                        alarm.member.id,
                        alarm.prepareTime,
                        alarm.interval,
                        alarm.arrivalTime,
                        alarm.day,
                        route.totalTime))
                .from(alarm)
                .join(alarm.route, route)
                .where(alarm.deleted.isFalse()
                        .and(alarm.day.isNull()
                                .or(alarm.day.isEmpty())
                                .or(alarm.day.contains(todayKo)))
                        .and(alarm.alarmId.between(minId, maxId))
                        .and(alarm.alarmId.gt(lastId)))
                .orderBy(alarm.alarmId.asc())
                .limit(pageSize)
                .fetch();
    }
}
