package com.example.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "insurance_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date claimDate;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.PENDING;

    @Column(nullable = false)
    private String insuranceCompany;

    @Column(nullable = false)
    private Integer insuranceGrade;

    @Column(nullable = false)
    private Double reimbursementAmount;

    @Column(unique = true)
    private String externalRef;

    @ElementCollection
    @CollectionTable(name = "claim_files", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "file_path")
    @Builder.Default
    private List<String> filePaths = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "insuranceClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Remboursement> remboursements = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.claimDate = new Date();
    }
}
