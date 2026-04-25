package com.sweep.project.route.batch;

import com.sweep.project.route.domain.RouteTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class RoutePartitioner implements Partitioner {


    private final RouteTicketRepo routeTicketRepo;
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        List<Long> ids=routeTicketRepo.getMinMaxId(LocalDateTime.now());
        Map<String, ExecutionContext> result = new LinkedHashMap<>();

        Long maxId=ids.get(1);
        Long minId=ids.get(0);

        if (minId == null || maxId == null) {
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", 0L);
            ctx.putLong("maxId", -1L);
            result.put("routeUpdate0", ctx);
            return result;
        }

        long totalRange = maxId - minId + 1;
        long rangePerPartition = Math.max(1, (long) Math.ceil((double) totalRange / gridSize));

        for (int i = 0; i < gridSize; i++) {
            long min = minId + ((long) i * rangePerPartition);
            long max = Math.min(min + rangePerPartition - 1, maxId);

            if (min > maxId) break;

            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", min);
            ctx.putLong("maxId", max);
            result.put("routeUpdate" + i, ctx);
        }
        return result;
    }
}
