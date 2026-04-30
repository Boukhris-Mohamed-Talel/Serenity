package com.example.marketplace.service.impl;

import com.example.marketplace.dto.ProductResponseDTO;
import com.example.marketplace.dto.ProductUpsertRequestDTO;
import com.example.marketplace.entity.Product;
import com.example.marketplace.entity.ProductCategory;
import com.example.marketplace.entity.ProductType;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.ProductRepository;
import com.example.marketplace.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProducts(String query, ProductCategory category, ProductType type) {
        List<Product> products;

        if (StringUtils.hasText(query)) {
            products = productRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(query.trim());
        } else if (category != null && type != null) {
            products = productRepository.findByActiveTrueAndCategoryAndTypeOrderByCreatedAtDesc(category, type);
        } else if (category != null) {
            products = productRepository.findByActiveTrueAndCategoryOrderByCreatedAtDesc(category);
        } else if (type != null) {
            products = productRepository.findByActiveTrueAndTypeOrderByCreatedAtDesc(type);
        } else {
            products = productRepository.findByActiveTrueOrderByCreatedAtDesc();
        }

        return products.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProductsForAdmin() {
        return productRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        Product product = findProductById(id);
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductUpsertRequestDTO request) {
        Integer stock = resolveStockQuantity(request.getType(), request.getStockQuantity());
        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .type(request.getType())
                .price(request.getPrice())
                .active(request.getActive())
                .imageUrl(request.getImageUrl())
                .previewable(request.getPreviewable())
                .previewType(request.getPreviewType())
                .previewUrl(request.getPreviewUrl())
                .contentUrl(request.getContentUrl())
                .stockQuantity(stock)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductUpsertRequestDTO request) {
        Product product = findProductById(id);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription().trim());
        product.setCategory(request.getCategory());
        product.setType(request.getType());
        product.setPrice(request.getPrice());
        product.setActive(request.getActive());
        product.setImageUrl(request.getImageUrl());
        product.setPreviewable(request.getPreviewable());
        product.setPreviewType(request.getPreviewType());
        product.setPreviewUrl(request.getPreviewUrl());
        product.setContentUrl(request.getContentUrl());
        product.setStockQuantity(resolveStockQuantity(request.getType(), request.getStockQuantity()));
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        productRepository.delete(product);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + id));
    }

    private Integer resolveStockQuantity(ProductType type, Integer requested) {
        if (type == ProductType.DIGITAL) {
            return null;
        }
        if (requested == null) {
            throw new IllegalArgumentException("Physical products require a stock quantity (0 or more).");
        }
        if (requested < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        return requested;
    }

    private ProductResponseDTO toResponse(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .type(product.getType())
                .price(product.getPrice())
                .active(product.getActive())
                .imageUrl(product.getImageUrl())
                .previewable(product.getPreviewable())
                .previewType(product.getPreviewType())
                .previewUrl(product.getPreviewUrl())
                .contentUrl(product.getContentUrl())
                .stockQuantity(product.getStockQuantity())
                .build();
    }
}
