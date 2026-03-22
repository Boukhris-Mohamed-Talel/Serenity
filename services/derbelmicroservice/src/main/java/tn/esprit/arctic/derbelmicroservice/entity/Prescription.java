package tn.esprit.arctic.derbelmicroservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @Size(max = 100)
    @Column(nullable = false)
    private String medicationName;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false)
    private String dosage;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false)
    private String frequency;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500)
    private String instructions;

    @Min(1)
    private int quantity;

    @NotBlank
    @Column(nullable = false)
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
