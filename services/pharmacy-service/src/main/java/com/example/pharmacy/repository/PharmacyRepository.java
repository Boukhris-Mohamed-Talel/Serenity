package com.example.pharmacy.repository;

import com.example.pharmacy.entity.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
    Optional<Pharmacy> findByOwnerUserId(Long ownerUserId);
    List<Pharmacy> findAllByOrderByNameAsc();
}
