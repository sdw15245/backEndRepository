package com.sweep.project.route.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRoute is a Querydsl query type for Route
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoute extends EntityPathBase<Route> {

    private static final long serialVersionUID = -1997699064L;

    public static final QRoute route = new QRoute("route");

    public final DateTimePath<java.time.LocalDateTime> createDate = createDateTime("createDate", java.time.LocalDateTime.class);

    public final NumberPath<Double> endX = createNumber("endX", Double.class);

    public final NumberPath<Double> endY = createNumber("endY", Double.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath routeData = createString("routeData");

    public final NumberPath<Double> startX = createNumber("startX", Double.class);

    public final NumberPath<Double> startY = createNumber("startY", Double.class);

    public final EnumPath<PathSearchType> type = createEnum("type", PathSearchType.class);

    public QRoute(String variable) {
        super(Route.class, forVariable(variable));
    }

    public QRoute(Path<? extends Route> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRoute(PathMetadata metadata) {
        super(Route.class, metadata);
    }

}

