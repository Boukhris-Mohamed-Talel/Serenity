package com.example.marketplace.repository;

import com.example.marketplace.entity.Product;
import com.example.marketplace.entity.ProductCategory;
import com.example.marketplace.entity.ProductType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    List<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    List<Product> findByActiveTrueAndCategoryAndTypeOrderByCreatedAtDesc(ProductCategory category, ProductType type);

    List<Product> findByActiveTrueAndCategoryOrderByCreatedAtDesc(ProductCategory category);

    List<Product> findByActiveTrueAndTypeOrderByCreatedAtDesc(ProductType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
