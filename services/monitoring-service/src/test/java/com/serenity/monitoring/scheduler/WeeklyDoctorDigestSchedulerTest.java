package com.serenity.monitoring.scheduler;

import com.serenity.monitoring.service.WeeklyDoctorDigestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeeklyDoctorDigestSchedulerTest {

    @Mock
    private WeeklyDoctorDigestService weeklyDoctorDigestService;

    @InjectMocks
    private WeeklyDoctorDigestScheduler scheduler;

    @Test
    void runWeeklyDoctorDigest_callsService() {
        scheduler.runWeeklyDoctorDigest();
        verify(weeklyDoctorDigestService).generateWeeklyDigests();
    }
}
