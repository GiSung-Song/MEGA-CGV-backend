package com.cgv.mega.reservation.service;

import com.cgv.mega.reservation.dto.DeleteScreeningSeatKeyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReservationEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteScreeningSeatKeyEventHandler(DeleteScreeningSeatKeyEvent event) {
        redisTemplate.delete(event.keys());
    }
}
