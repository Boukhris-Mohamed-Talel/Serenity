package com.serenity.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodTrendPointDTO {

    private String date;
    private Double averageMood;
    private Long entryCount;
    private Long crisisCount;
}

