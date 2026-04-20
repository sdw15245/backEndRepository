package com.sweep.project.alarm.batch;

import com.sweep.project.alarm.repository.AlarmTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 활성 Alarm 을 zero-offset cursor 방식으로 스트리밍하는 리더.
 * ExecutionContext 에서 minId / maxId (alarm.alarmId 기준) 를 읽어
 * 해당 파티션 범위만 처리한다.
 */
@Slf4j
@RequiredArgsConstructor
public class AlarmZeroOffsetReader implements ItemStreamReader<AlarmBatchDto> {

    private final AlarmTicketRepo alarmTicketRepo;
    private final int pageSize;

    /** 오늘 요일 한글 약자 — DB 페이지 쿼리 필터에 전달 */
    private final String todayKo;

    private static final String LAST_ID_KEY = "alarmLastIdKey";

    private Iterator<AlarmBatchDto> buffer = Collections.emptyIterator();
    private Long startId;
    private Long minId;
    private Long maxId;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        minId = executionContext.getLong("minId");
        maxId = executionContext.getLong("maxId");

        if (isEmpty()) {
            log.info("[AlarmReader] 조회 대상 없음 (minId={}, maxId={}), 스텝을 건너뜁니다.", minId, maxId);
            return;
        }

        startId = executionContext.containsKey(LAST_ID_KEY)
                ? executionContext.getLong(LAST_ID_KEY)
                : minId - 1;
    }

    @Override
    public AlarmBatchDto read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (isEmpty()) return null;

        if (!buffer.hasNext()) {
            List<AlarmBatchDto> data = alarmTicketRepo.fetchActiveAlarmPage(minId, maxId, startId, pageSize, todayKo);
            if (data.isEmpty()) return null;

            startId = data.get(data.size() - 1).getAlarmId();
            buffer = data.iterator();
        }
        return buffer.next();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (startId != null) {
            executionContext.putLong(LAST_ID_KEY, startId);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        ItemStreamReader.super.close();
    }

    private boolean isEmpty() {
        return maxId == null || minId == null || maxId < minId;
    }
}
