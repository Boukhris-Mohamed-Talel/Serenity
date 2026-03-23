package com.example.pharmacy.controller;

import com.example.pharmacy.dto.StockItemCreateRequestDTO;
import com.example.pharmacy.dto.StockItemResponseDTO;
import com.example.pharmacy.dto.StockQuantityIncrementRequestDTO;
import com.example.pharmacy.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy/stock")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PHARMACIST')")
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<StockItemResponseDTO>> listStock(
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return ResponseEntity.ok(stockService.listMyStock(query, includeArchived));
    }

    @PostMapping
    public ResponseEntity<StockItemResponseDTO> createStockItem(@Valid @RequestBody StockItemCreateRequestDTO request) {
        return ResponseEntity.ok(stockService.createStockItem(request));
    }

    @PatchMapping("/{stockItemId}/increment")
    public ResponseEntity<StockItemResponseDTO> incrementQuantity(
        @PathVariable Long stockItemId,
        @Valid @RequestBody StockQuantityIncrementRequestDTO request
    ) {
        return ResponseEntity.ok(stockService.incrementQuantity(stockItemId, request.getIncrementBy()));
    }

    @PatchMapping("/{stockItemId}/out-of-stock")
    public ResponseEntity<StockItemResponseDTO> markOutOfStock(@PathVariable Long stockItemId) {
        return ResponseEntity.ok(stockService.markOutOfStock(stockItemId));
    }

    @PostMapping("/{stockItemId}/out-of-stock")
    public ResponseEntity<StockItemResponseDTO> markOutOfStockPost(@PathVariable Long stockItemId) {
        return ResponseEntity.ok(stockService.markOutOfStock(stockItemId));
    }

    @DeleteMapping("/{stockItemId}")
    public ResponseEntity<Void> archiveStockItem(@PathVariable Long stockItemId) {
        stockService.archiveStockItem(stockItemId);
        return ResponseEntity.noContent().build();
    }
}
