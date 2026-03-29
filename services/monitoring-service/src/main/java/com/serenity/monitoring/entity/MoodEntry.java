package com.serenity.monitoring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "mood_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long doctorId;  // Assigned doctor for this patient (Round-Robin assignment)

    @Column(nullable = false)
    private Integer moodScore;  // 1-10 scale

    @Column(columnDefinition = "TEXT")
    private String moodDescription;  // Renamed from 'condition' (reserved keyword)

    @Column(columnDefinition = "TEXT")
    private String triggers;  // Emotional triggers (comma-separated or JSON)

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
    }
}
