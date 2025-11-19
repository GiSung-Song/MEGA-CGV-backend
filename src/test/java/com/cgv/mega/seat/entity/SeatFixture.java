package com.cgv.mega.seat.entity;

import com.cgv.mega.common.enums.SeatType;
import com.cgv.mega.theater.entity.Theater;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

public final class SeatFixture {
    private static long SEAT_ID_SEQUENCE = 1L;

    private SeatFixture() {}

    public static void resetSequence() {
        SEAT_ID_SEQUENCE = 1L;
    }

    public static Set<Seat> defaultSeats(Theater theater) {
        resetSequence();

        Set<Seat> seats = new HashSet<>();

        char[] rows = {'A', 'B', 'C', 'D'};
        int cols = 5;

        for (char row : rows) {
            for (int col = 1; col <= cols; col++) {
                SeatType type = (row == 'D') ? SeatType.PREMIUM : SeatType.NORMAL;

                seats.add(createSeat(theater, String.valueOf(row), col, type));
            }
        }

        return seats;
    }

    public static Seat createSeat(Theater theater, String row, int col, SeatType type) {
        Seat seat = new Seat();

        ReflectionTestUtils.setField(seat, "id", SEAT_ID_SEQUENCE++);
        ReflectionTestUtils.setField(seat, "theater", theater);
        ReflectionTestUtils.setField(seat, "rowLabel", row);
        ReflectionTestUtils.setField(seat, "colNumber", col);
        ReflectionTestUtils.setField(seat, "type", type);

        return seat;
    }
}