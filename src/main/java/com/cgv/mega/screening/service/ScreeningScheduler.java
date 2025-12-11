package com.cgv.mega.screening.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScreeningScheduler {

    private final ScreeningService screeningService;

    @Scheduled(cron = "0 */5 * * * *")
    public void endPastScreeningsJob() {
        screeningService.endPastScreenings();
    }
}