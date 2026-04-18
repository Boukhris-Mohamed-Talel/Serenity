package com.example.pharmacy.scheduler;

import com.example.pharmacy.service.impl.StockForecastAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockForecastScheduler {

    private final StockForecastAiClient stockForecastAiClient;

    @Scheduled(
        cron = "${app.ai-service.daily-cron:0 0 2 * * *}",
        zone = "${app.ai-service.daily-zone:Africa/Tunis}"
    )
    public void runDailyForecast() {
        stockForecastAiClient.runForecast(null);
    }
}
