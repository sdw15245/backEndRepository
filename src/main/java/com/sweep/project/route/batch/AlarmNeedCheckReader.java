package com.sweep.project.route.batch;

import com.sweep.project.route.domain.RouteTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AlarmNeedCheckReader implements ItemStreamReader<Long> {

    private final RouteTicketRepo routeTicketRepo;
    private final int pageSize;
    private final LocalDateTime updateAt;

    private static final String LAST_ID_KEY = "alarmNeedCheckLastId";

    private Iterator<Long> buffer = Collections.emptyIterator();
    private Long lastId;
    private Long minId;
    private Long maxId;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        minId = executionContext.getLong("minId");
        maxId = executionContext.getLong("maxId");

        if (isEmpty()) {
            log.info("[AlarmNeedCheckReader] 경로 업데이트 후 체크 필요한 알람 없음 (minId={}, maxId={}), 스텝을 건너뜁니다.", minId, maxId);
            return;
        }

        lastId = executionContext.containsKey(LAST_ID_KEY)
                ? executionContext.getLong(LAST_ID_KEY)
                : minId - 1;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (lastId != null) {
            executionContext.putLong(LAST_ID_KEY, lastId);
        }
    }

    @Override
    public Long read() throws Exception {
        if (isEmpty()) return null;

        if (!buffer.hasNext()) {
            List<Long> page = routeTicketRepo.fetchAlarmNeedCheckPage(minId, maxId, lastId, pageSize, updateAt);
            if (page.isEmpty()) return null;
            lastId = page.get(page.size() - 1);
            buffer = page.iterator();
        }
        return buffer.next();
    }

    private boolean isEmpty() {
        return maxId == null || minId == null || maxId < minId;
    }
}
