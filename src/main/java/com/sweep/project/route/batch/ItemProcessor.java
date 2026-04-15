package com.sweep.project.route.batch;

public class ItemProcessor implements org.springframework.batch.item.ItemProcessor<RouteBatchDto, RouteBatchDto> {

    @Override
    public RouteBatchDto process(RouteBatchDto item) throws Exception {
        return item;
    }
}
