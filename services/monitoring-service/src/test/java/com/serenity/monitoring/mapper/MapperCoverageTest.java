package com.serenity.monitoring.mapper;

import com.serenity.monitoring.dto.EmotionalTriggerRequest;
import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapperCoverageTest {

    @Test
    void moodEntryMapperImpl_mapsEntityDtoAndList() {
        MoodEntryMapperImpl mapper = new MoodEntryMapperImpl();
        MoodEntryRequestDTO request = MoodEntryRequestDTO.builder()
                .patientId(2L).doctorId(4L).moodScore(6).moodDescription("desc").triggers("t").build();
        MoodEntry entity = mapper.toEntity(request);

        assertEquals(2L, entity.getPatientId());
        assertEquals(4L, entity.getDoctorId());
        assertNotNull(mapper.toResponseDTO(entity));
        assertEquals(1, mapper.toResponseDTOList(List.of(entity)).size());
        assertNull(mapper.toEntity(null));
        assertNull(mapper.toResponseDTO(null));
        assertNull(mapper.toResponseDTOList(null));
    }

    @Test
    void emotionalTriggerMapper_generatedImplMapsRequestAndResponse() {
        EmotionalTriggerMapper mapper = Mappers.getMapper(EmotionalTriggerMapper.class);
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(10L).triggerType("WORK_STRESS").description("desc").intensity(4).build();
        EmotionalTrigger entity = mapper.toEntity(request);
        entity.setDoctorId(3L);
        entity.setMoodEntry(MoodEntry.builder().id(10L).build());

        assertEquals(10L, entity.getMoodEntry().getId());
        assertEquals("WORK_STRESS", entity.getTriggerType());
        assertNotNull(mapper.toResponse(entity));
    }
}
