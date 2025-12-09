package com.cgv.mega.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservationGroup is a Querydsl query type for ReservationGroup
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationGroup extends EntityPathBase<ReservationGroup> {

    private static final long serialVersionUID = -272626071L;

    public static final QReservationGroup reservationGroup = new QReservationGroup("reservationGroup");

    public final com.cgv.mega.common.entity.QBaseTimeEntity _super = new com.cgv.mega.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<Reservation, QReservation> reservations = this.<Reservation, QReservation>createList("reservations", Reservation.class, QReservation.class, PathInits.DIRECT2);

    public final EnumPath<com.cgv.mega.reservation.enums.ReservationStatus> status = createEnum("status", com.cgv.mega.reservation.enums.ReservationStatus.class);

    public final NumberPath<Integer> totalPrice = createNumber("totalPrice", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QReservationGroup(String variable) {
        super(ReservationGroup.class, forVariable(variable));
    }

    public QReservationGroup(Path<? extends ReservationGroup> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReservationGroup(PathMetadata metadata) {
        super(ReservationGroup.class, metadata);
    }

}

