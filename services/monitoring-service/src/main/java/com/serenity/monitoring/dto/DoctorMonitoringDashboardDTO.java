package com.serenity.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorMonitoringDashboardDTO {

    private Long totalPatients;
    private Long totalMoodEntries;
    private Long totalClinicalTriggers;
    private Long crisisEvents;
    private Double averageMood;
    private Double averageMoodChange;
    private Long activeHighRiskPatients;
    private List<MoodTrendPointDTO> moodTrend;
    private List<PatientMoodPointDTO> patientPoints;
}

