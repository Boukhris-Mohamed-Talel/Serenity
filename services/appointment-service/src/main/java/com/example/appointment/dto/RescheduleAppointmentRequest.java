package com.example.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleAppointmentRequest {

    @NotNull
    private LocalDate appointmentDate;

    @NotNull
    @Size(min = 3, max = 8)
    private String timeSlot;
}
