package com.serenity.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodEntryResponseDTO {

    private Long id;
    private Long patientId;
    private String patientName;
    /** Profile avatar URL from {@code user_profiles.avatar} (optional). */
    private String patientAvatarUrl;
    private Long doctorId;  // Assigned doctor who tracks this patient's mood entries
    private String doctorName;
    private Integer moodScore;
    private String moodDescription;
    private String triggers;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date updatedAt;
}
