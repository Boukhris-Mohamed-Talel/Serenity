package com.example.appointment.repository;

import com.example.appointment.entity.Appointment;
import com.example.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientUserIdOrderByAppointmentDateDescTimeSlotDesc(Long patientUserId);

    List<Appointment> findByDoctorUserIdOrderByAppointmentDateDescTimeSlotDesc(Long doctorUserId);

    List<Appointment> findAllByOrderByAppointmentDateDescTimeSlotDesc();

    boolean existsByDoctorUserIdAndAppointmentDateAndTimeSlotAndStatusIn(
            Long doctorUserId,
            LocalDate appointmentDate,
            String timeSlot,
            Collection<AppointmentStatus> statuses);

    Optional<Appointment> findByIdAndPatientUserId(Long id, Long patientUserId);

    Optional<Appointment> findByIdAndDoctorUserId(Long id, Long doctorUserId);

    List<Appointment> findByStatusAndAppointmentDateGreaterThanEqual(
            AppointmentStatus status,
            LocalDate minDate);

    List<Appointment> findByDoctorUserIdAndAppointmentDateBetweenAndStatusIn(
            Long doctorUserId,
            LocalDate fromInclusive,
            LocalDate toInclusive,
            Collection<AppointmentStatus> statuses);

    List<Appointment> findByPatientUserIdAndAppointmentDateBetweenAndStatusIn(
            Long patientUserId,
            LocalDate fromInclusive,
            LocalDate toInclusive,
            Collection<AppointmentStatus> statuses);
}
