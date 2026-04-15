package com.sweep.project.route.batch;

import com.sweep.project.route.domain.RouteTicketRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@RequiredArgsConstructor
public class NullRouteTicketWriter implements ItemWriter<Long> {

    private final RouteTicketRepo routeTicketRepo;

    @Override
    public void write(Chunk<? extends Long> chunk) throws Exception {
        routeTicketRepo.updateNeedCheckBatch(chunk.getItems());
    }
}
