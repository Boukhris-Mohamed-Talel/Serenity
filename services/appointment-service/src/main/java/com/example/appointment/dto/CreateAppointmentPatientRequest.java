package com.example.appointment.dto;

import com.example.appointment.entity.AppointmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentPatientRequest {

    @NotNull
    @Positive
    private Long doctorUserId;

    @NotNull
    private LocalDate appointmentDate;

    @NotNull
    @Size(min = 3, max = 8)
    private String timeSlot;

    @NotNull
    private AppointmentType type;

    @Size(max = 2000)
    private String notes;
}
