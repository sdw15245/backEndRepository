package com.sweep.project.route.batch;

import com.sweep.project.route.domain.RouteTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ZeroOffSetReader implements ItemStreamReader<RouteBatchDto> {

    private final RouteTicketRepo routeTicketRepo;
    private final int pageSize;
    private final String last_id_key = "lastIdKey";
    private Iterator<RouteBatchDto> buffer = Collections.emptyIterator();
    private Long startId;
    private Long maxId;
    private Long minId;

    @Override
    public void close() throws ItemStreamException {
        ItemStreamReader.super.close();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong(last_id_key, startId);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        minId = executionContext.getLong("minId");
        maxId = executionContext.getLong("maxId");

        if (isEmpty()) {
            log.info("[RouteReader] 조회 대상 없음 (minId={}, maxId={}), 스텝을 건너뜁니다.", minId, maxId);
            return;
        }

        startId = executionContext.containsKey(last_id_key)
                ? executionContext.getLong(last_id_key)
                : minId - 1;
    }

    @Override
    public RouteBatchDto read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (isEmpty()) {
            return null;
        }
        if (!buffer.hasNext()) {
            List<RouteBatchDto> data = routeTicketRepo.fetchPage(minId, maxId, startId, pageSize);
            if (data.isEmpty()) {
                return null;
            }
            startId = data.get(data.size() - 1).getId();
            buffer = data.iterator();
        }
        return buffer.next();
    }

    private boolean isEmpty() {
        return maxId == null || minId == null || maxId < minId;
    }
}
