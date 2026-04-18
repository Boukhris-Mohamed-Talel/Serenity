package com.example.pharmacy.repository;

import com.example.pharmacy.entity.StockForecastPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockForecastPredictionRepository extends JpaRepository<StockForecastPrediction, Long> {

    @Query("""
        select p from StockForecastPrediction p
        where p.pharmacy.id = :pharmacyId
          and p.runAt = (
            select max(p2.runAt) from StockForecastPrediction p2
            where p2.pharmacy.id = :pharmacyId
          )
        order by p.medicineName asc, p.forecastDate asc
        """)
    List<StockForecastPrediction> findLatestRunRowsByPharmacyId(@Param("pharmacyId") Long pharmacyId);
}
