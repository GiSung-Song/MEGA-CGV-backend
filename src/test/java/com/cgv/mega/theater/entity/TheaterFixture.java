package com.cgv.mega.theater.entity;

import com.cgv.mega.common.enums.TheaterType;
import org.springframework.test.util.ReflectionTestUtils;

public final class TheaterFixture {

    private TheaterFixture() {}

    public static Theater createTheater(Long id, String name, int totalSeat, TheaterType type, int basePrice) {
        Theater theater = new Theater();

        ReflectionTestUtils.setField(theater, "id", id);
        ReflectionTestUtils.setField(theater, "name", name);
        ReflectionTestUtils.setField(theater, "totalSeat", totalSeat);
        ReflectionTestUtils.setField(theater, "type", type);
        ReflectionTestUtils.setField(theater, "basePrice", basePrice);

        return theater;
    }

}