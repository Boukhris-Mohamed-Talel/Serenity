package com.serenity.monitoring.repository;

import com.serenity.monitoring.entity.EmotionalTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmotionalTriggerRepository extends JpaRepository<EmotionalTrigger, Long> {

    List<EmotionalTrigger> findByMoodEntryId(Long moodEntryId);

    List<EmotionalTrigger> findByDoctorId(Long doctorId);

    List<EmotionalTrigger> findByMoodEntryIdAndDoctorId(Long moodEntryId, Long doctorId);

    boolean existsByMoodEntryId(Long moodEntryId);
}

