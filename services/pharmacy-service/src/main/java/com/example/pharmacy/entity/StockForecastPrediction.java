package com.example.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "stock_forecast_predictions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_stock_forecast_prediction_pharmacy_medicine_date",
            columnNames = {"pharmacy_id", "medicine_name", "forecast_date"}
        )
    },
    indexes = {
        @Index(name = "idx_stock_forecast_pharmacy_run", columnList = "pharmacy_id, run_at"),
        @Index(name = "idx_stock_forecast_pharmacy_date", columnList = "pharmacy_id, forecast_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockForecastPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "predicted_demand", nullable = false)
    private Double predictedDemand;

    @Enumerated(EnumType.STRING)
    @Column(name = "stockout_risk", nullable = false, length = 10)
    private StockoutRisk stockoutRisk;

    @Column(name = "suggested_reorder_qty", nullable = false)
    private Integer suggestedReorderQty;
}
