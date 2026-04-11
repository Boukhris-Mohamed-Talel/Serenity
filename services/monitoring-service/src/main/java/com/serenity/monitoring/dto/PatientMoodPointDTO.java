package com.serenity.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMoodPointDTO {

    private Long patientId;
    private String patientName;
    private String patientAvatarUrl;
    private Integer x;
    private Integer latestMoodScore;
    private Double averageMoodScore;
    private Double moodChange;
    private Long entryCount;
    private Long crisisCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date latestEntryAt;

    private String latestTriggerType;
    private String latestTriggerDescription;
    private Integer latestTriggerIntensity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime latestTriggerAt;
}

