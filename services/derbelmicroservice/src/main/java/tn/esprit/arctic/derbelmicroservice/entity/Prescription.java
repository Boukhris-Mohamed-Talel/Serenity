package tn.esprit.arctic.derbelmicroservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.arctic.derbelmicroservice.entity.converter.PrescriptionMedicationsConverter;
import tn.esprit.arctic.derbelmicroservice.entity.value.PrescriptionMedication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @NotEmpty
    @Column(name = "medications_data", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = PrescriptionMedicationsConverter.class)
    @Builder.Default
    private List<PrescriptionMedication> medications = new ArrayList<>();

    // Legacy columns kept to stay compatible with existing DB constraints.
    @Column(name = "dosage", nullable = false)
    private String dosage;

    @Column(name = "frequency", nullable = false)
    private String frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @NotNull
    @Column(nullable = false)
    private Long patientId;

    @NotNull
    @Column(nullable = false)
    private Long doctorId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
