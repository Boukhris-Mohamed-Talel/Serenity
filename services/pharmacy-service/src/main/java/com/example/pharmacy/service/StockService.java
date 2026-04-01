package com.example.pharmacy.service;

import com.example.pharmacy.dto.StockItemCreateRequestDTO;
import com.example.pharmacy.dto.StockItemResponseDTO;

import java.util.List;

public interface StockService {
    List<StockItemResponseDTO> listMyStock(String query, boolean includeArchived);
    StockItemResponseDTO createStockItem(StockItemCreateRequestDTO request);
    StockItemResponseDTO incrementQuantity(Long stockItemId, Integer incrementBy);
    StockItemResponseDTO markOutOfStock(Long stockItemId);
    StockItemResponseDTO restoreStockItem(Long stockItemId);
    void archiveStockItem(Long stockItemId);

    StockItemResponseDTO renameStockItem(Long stockItemId, String medicineName);

    void deleteArchivedStockItem(Long stockItemId);
}
