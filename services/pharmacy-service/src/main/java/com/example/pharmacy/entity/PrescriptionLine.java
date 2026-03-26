package com.example.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prescription_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_order_id", nullable = false)
    private PrescriptionOrder prescriptionOrder;

    @Column(nullable = false)
    private String medicationName;

    @Column(nullable = false)
    private String dosage;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 1500)
    private String instructions;
}
