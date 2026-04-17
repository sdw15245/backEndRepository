package com.sweep.project.route.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRouteTicket is a Querydsl query type for RouteTicket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRouteTicket extends EntityPathBase<RouteTicket> {

    private static final long serialVersionUID = -282674892L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRouteTicket routeTicket = new QRouteTicket("routeTicket");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final BooleanPath deleted = createBoolean("deleted");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.sweep.project.member.domain.QMember member;

    public final BooleanPath needCheck = createBoolean("needCheck");

    public final QRoute route;

    public QRouteTicket(String variable) {
        this(RouteTicket.class, forVariable(variable), INITS);
    }

    public QRouteTicket(Path<? extends RouteTicket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRouteTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRouteTicket(PathMetadata metadata, PathInits inits) {
        this(RouteTicket.class, metadata, inits);
    }

    public QRouteTicket(Class<? extends RouteTicket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.sweep.project.member.domain.QMember(forProperty("member")) : null;
        this.route = inits.isInitialized("route") ? new QRoute(forProperty("route")) : null;
    }

}

