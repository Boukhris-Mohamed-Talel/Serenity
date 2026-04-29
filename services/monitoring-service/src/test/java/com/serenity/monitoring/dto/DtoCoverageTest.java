package com.serenity.monitoring.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoCoverageTest {

    @Test
    void apiResponse_and_doctorDto_and_alertDtos_coverGeneratedMethods() {
        ApiResponse<String> api = ApiResponse.success("ok", "data");
        assertTrue(api.isSuccess());
        assertEquals("ok", api.getMessage());
        assertNotNull(api.getTimestamp());
        assertNotNull(api.toString());
        assertNotNull(api.hashCode());

        DoctorDTO d1 = DoctorDTO.builder().id(1L).firstName("A").lastName("B").email("a@b.com").role("DOCTOR").build();
        DoctorDTO d2 = new DoctorDTO(1L, "A", "B", "a@b.com", "DOCTOR");
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());

        CrisisAlertPayload alert = CrisisAlertPayload.builder()
                .doctorId(1L).patientId(2L).patientFullName("P").moodLevel(3).message("m").timestamp(new Date()).build();
        assertEquals(1L, alert.getDoctorId());
        alert.setMessage("updated");
        assertEquals("updated", alert.getMessage());

        WeeklyDoctorDigestPayload payload = WeeklyDoctorDigestPayload.builder()
                .doctorId(3L).weekStartDate(LocalDate.now()).weekEndDate(LocalDate.now())
                .crisisCount(1).worseningPatients(1).noCheckinPatients(1).summaryMessage("s").generatedAt(new Date()).build();
        assertEquals(3L, payload.getDoctorId());
        WeeklyDoctorDigestResponseDTO digest = WeeklyDoctorDigestResponseDTO.builder()
                .id(9L).doctorId(3L).weekStartDate(LocalDate.now()).weekEndDate(LocalDate.now())
                .crisisCount(1).worseningPatients(1).noCheckinPatients(1).summaryMessage("s").generatedAt(new Date()).build();
        assertEquals(9L, digest.getId());
        digest.setSummaryMessage("changed");
        assertEquals("changed", digest.getSummaryMessage());
    }

    @Test
    void moodAndEmotionalDtos_coverConstructorsBuildersAndAccessors() {
        MoodEntryRequestDTO req = MoodEntryRequestDTO.builder()
                .patientId(1L).doctorId(2L).moodScore(5).moodDescription("description text").triggers("t").build();
        assertEquals(1L, req.getPatientId());
        req.setMoodScore(7);
        assertEquals(7, req.getMoodScore());

        MoodEntryResponseDTO response = MoodEntryResponseDTO.builder()
                .id(10L).patientId(1L).patientName("p").patientAvatarUrl("u")
                .doctorId(2L).doctorName("d").moodScore(7).moodDescription("m").triggers("t")
                .createdAt(new Date()).updatedAt(new Date()).aiRiskLevel("LOW_RISK")
                .aiRiskConfidence(0.5).aiRiskRecommendation("none").aiRiskType("x")
                .aiMediumRiskType("y").aiRiskScore(1).build();
        assertEquals(10L, response.getId());
        assertNotNull(response.toString());

        EmotionalTriggerRequest trReq = EmotionalTriggerRequest.builder()
                .moodEntryId(11L).triggerType("WORK_STRESS").description("description text")
                .intensity(3).build();
        assertEquals("WORK_STRESS", trReq.getTriggerType());

        EmotionalTriggerResponse trRes = EmotionalTriggerResponse.builder()
                .id(1L).moodEntryId(11L).doctorId(2L).triggerType("WORK_STRESS")
                .description("d").intensity(4).recordedAt(LocalDateTime.now()).build();
        assertEquals(1L, trRes.getId());
        assertNotNull(trRes.hashCode());
    }

    @Test
    void patientMentalHealthRecordDto_and_nestedTypes_coverGeneratedMethods() {
        PatientMentalHealthRecordDTO.TriggerRecordItem t = PatientMentalHealthRecordDTO.TriggerRecordItem.builder()
                .triggerType("WORK_STRESS").description("desc").intensity(4).recordedAt(LocalDateTime.now()).build();
        PatientMentalHealthRecordDTO.MoodEntryRecordItem m = PatientMentalHealthRecordDTO.MoodEntryRecordItem.builder()
                .moodEntryId(1L).moodScore(6).moodDescription("desc").triggers("t")
                .createdAt(new Date()).clinicalTriggers(List.of(t)).build();
        PatientMentalHealthRecordDTO dto = PatientMentalHealthRecordDTO.builder()
                .patientId(1L).patientFullName("p").patientEmail("e").patientAvatarUrl("a")
                .patientBio("bio").preferredLanguage("en").ageDisplay("20 years")
                .doctorId(2L).doctorFullName("doc").generatedAt(LocalDateTime.now())
                .totalMoodEntries(1).totalClinicalTriggers(1).averageMoodScore(6.0)
                .stabilitySummary("stable").moodEntries(List.of(m)).build();

        assertEquals(1L, dto.getPatientId());
        assertEquals(1, dto.getMoodEntries().size());
        assertNotNull(dto.toString());
        assertNotNull(m.toString());
        assertNotNull(t.toString());

        PatientMentalHealthRecordDTO dto2 = PatientMentalHealthRecordDTO.builder()
                .patientId(1L).patientFullName("p").patientEmail("e").patientAvatarUrl("a")
                .patientBio("bio").preferredLanguage("en").ageDisplay("20 years")
                .doctorId(2L).doctorFullName("doc").generatedAt(dto.getGeneratedAt())
                .totalMoodEntries(1).totalClinicalTriggers(1).averageMoodScore(6.0)
                .stabilitySummary("stable").moodEntries(List.of(m)).build();
        assertEquals(dto, dto2);
        assertEquals(dto.hashCode(), dto2.hashCode());
        assertEquals(m, PatientMentalHealthRecordDTO.MoodEntryRecordItem.builder()
                .moodEntryId(1L).moodScore(6).moodDescription("desc").triggers("t")
                .createdAt(m.getCreatedAt()).clinicalTriggers(List.of(t)).build());
        assertEquals(t, PatientMentalHealthRecordDTO.TriggerRecordItem.builder()
                .triggerType("WORK_STRESS").description("desc").intensity(4).recordedAt(t.getRecordedAt()).build());
    }

    @Test
    void additionalDtoEqualityHashcodeCoverage() {
        MoodEntryResponseDTO a = new MoodEntryResponseDTO();
        a.setId(1L);
        a.setPatientId(2L);
        a.setDoctorId(3L);
        a.setMoodScore(4);
        a.setMoodDescription("d");
        a.setTriggers("t");
        a.setAiRiskLevel("LOW_RISK");
        a.setAiRiskConfidence(0.4);
        a.setAiRiskRecommendation("r");
        a.setAiRiskType("x");
        a.setAiMediumRiskType("y");
        a.setAiRiskScore(1);
        MoodEntryResponseDTO b = new MoodEntryResponseDTO(1L, null, null, null, 3L, null, 4, "d", "t", null, null,
                "LOW_RISK", 0.4, "r", "x", "y", 1);
        assertNotNull(a.toString());
        assertNotNull(a.hashCode());
        assertNotNull(b.hashCode());

        EmotionalTriggerRequest r1 = new EmotionalTriggerRequest(1L, "WORK_STRESS", "desc long enough", 2);
        EmotionalTriggerRequest r2 = new EmotionalTriggerRequest(1L, "WORK_STRESS", "desc long enough", 2);
        assertEquals(r1, r2);

        EmotionalTriggerResponse e1 = new EmotionalTriggerResponse(1L, 2L, 3L, "WORK_STRESS", "d", 2, LocalDateTime.now());
        EmotionalTriggerResponse e2 = new EmotionalTriggerResponse(1L, 2L, 3L, "WORK_STRESS", "d", 2, e1.getRecordedAt());
        assertEquals(e1, e2);

        DoctorDTO d = new DoctorDTO();
        d.setId(9L);
        d.setFirstName("f");
        d.setLastName("l");
        d.setEmail("e");
        d.setRole("DOCTOR");
        assertNotNull(d.toString());
    }
}
