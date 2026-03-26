package com.example.marketplace.service;

import com.example.marketplace.clients.InsuranceServiceClient;
import com.example.marketplace.clients.PharmacyServiceClient;
import com.example.marketplace.dto.*;
import com.example.marketplace.repository.ProductRepository;
import com.example.marketplace.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WellnessRecommendationEngine {

    private final InsuranceServiceClient insuranceServiceClient;
    private final PharmacyServiceClient pharmacyServiceClient;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", key = "#userId")
    public RecommendationResponseDTO getPersonalizedRecommendations(Long userId, String jwtToken) {
        try {
            log.info("Generating personalized recommendations for user {}", userId);

            // Step 1 & 2: Fetch approved claims and accepted prescriptions
            List<InsuranceClaimDTO> approvedClaims = insuranceServiceClient.getUserApprovedClaims(userId, jwtToken);
            List<PrescriptionDTO> acceptedPrescriptions = pharmacyServiceClient.getUserAcceptedPrescriptions(userId, jwtToken);

            // Step 3 & 4: Extract keywords and medication names
            Set<String> keywords = extractKeywordsFromClaims(approvedClaims);
            Set<String> medications = extractMedicationsFromPrescriptions(acceptedPrescriptions);

            log.info("Extracted {} keywords and {} medications for user {}", 
                keywords.size(), medications.size(), userId);

            // Step 5: Map to product categories
            Set<KnowledgeMappings.ProductCategory> categories = new HashSet<>();
            
            for (String keyword : keywords) {
                KnowledgeMappings.ProductCategory category = KnowledgeMappings.mapKeywordToCategory(keyword);
                if (category != null) {
                    categories.add(category);
                }
            }
            
            for (String medication : medications) {
                KnowledgeMappings.ProductCategory category = KnowledgeMappings.mapMedicationToCategory(medication);
                if (category != null) {
                    categories.add(category);
                }
            }

            // Step 6: Get top 4 products for each category
            List<ProductRecommendationItemDTO> recommendations = getTopProductsForCategories(categories, keywords, medications);

            // Step 7: Build response
            String reasoning = buildReasoningMessage(approvedClaims, acceptedPrescriptions);
            
            RecommendationResponseDTO response = RecommendationResponseDTO.builder()
                    .recommendations(recommendations)
                    .reasoning(reasoning)
                    .totalRecommendations(recommendations.size())
                    .generatedAt(LocalDateTime.now())
                    .build();

            log.info("Generated {} recommendations for user {}", recommendations.size(), userId);
            return response;

        } catch (Exception e) {
            log.error("Error generating recommendations for user {}: {}", userId, e.getMessage(), e);
            return RecommendationResponseDTO.builder()
                    .recommendations(new ArrayList<>())
                    .reasoning("Unable to generate recommendations at this time")
                    .totalRecommendations(0)
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    private Set<String> extractKeywordsFromClaims(List<InsuranceClaimDTO> claims) {
        Set<String> keywords = new HashSet<>();
        
        String[] keywordTerms = {
            "anxiety", "panic", "stress", 
            "insomnia", "sleep", "sleeplessness",
            "depression", "mood", "sadness",
            "focus", "concentration", "adhd"
        };
        
        for (InsuranceClaimDTO claim : claims) {
            if (claim.getDescription() != null && !claim.getDescription().isEmpty()) {
                String description = claim.getDescription().toLowerCase();
                for (String term : keywordTerms) {
                    if (description.contains(term)) {
                        keywords.add(term);
                    }
                }
            }
        }
        
        return keywords;
    }

    private Set<String> extractMedicationsFromPrescriptions(List<PrescriptionDTO> prescriptions) {
        Set<String> medications = new HashSet<>();
        
        for (PrescriptionDTO prescription : prescriptions) {
            // Add main medication name
            if (prescription.getMedicationName() != null && !prescription.getMedicationName().isEmpty()) {
                medications.add(prescription.getMedicationName());
            }
            
            // Add medication lines if present
            if (prescription.getMedicationLines() != null) {
                for (MedicationLineDTO line : prescription.getMedicationLines()) {
                    if (line.getMedicationName() != null && !line.getMedicationName().isEmpty()) {
                        medications.add(line.getMedicationName());
                    }
                }
            }
        }
        
        return medications;
    }

    private List<ProductRecommendationItemDTO> getTopProductsForCategories(
            Set<KnowledgeMappings.ProductCategory> categories, 
            Set<String> keywords, 
            Set<String> medications) {
        
        Map<Long, ProductRecommendationItemDTO> recommendedProductsMap = new LinkedHashMap<>();
        
        for (KnowledgeMappings.ProductCategory category : categories) {
            String categoryStr = category.toString();
            
            try {
                // Fetch products for this category (limit to top 4)
                List<Product> products = productRepository.findByActiveTrueAndCategoryOrderByCreatedAtDesc(
                        com.example.marketplace.entity.ProductCategory.valueOf(categoryStr)
                );
                
                products.stream()
                        .limit(4)
                        .forEach(product -> {
                            if (!recommendedProductsMap.containsKey(product.getId())) {
                                int confidence = calculateConfidence(product, keywords, medications, categoryStr);
                                
                                ProductRecommendationItemDTO item = ProductRecommendationItemDTO.builder()
                                        .productId(product.getId())
                                        .productName(product.getName())
                                        .category(categoryStr)
                                        .reason(buildProductReason(categoryStr, keywords, medications))
                                        .confidence(confidence)
                                        .build();
                                
                                recommendedProductsMap.put(product.getId(), item);
                            }
                        });
            } catch (IllegalArgumentException e) {
                log.warn("Category {} not found in Product entity: {}", categoryStr, e.getMessage());
            }
        }
        
        return recommendedProductsMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.getConfidence(), a.getConfidence()))
                .collect(Collectors.toList());
    }

    private int calculateConfidence(Product product, Set<String> keywords, Set<String> medications, String category) {
        int confidence = 60;
        
        // Boost confidence based on keyword/medication matches
        if (keywords.size() > 0 || medications.size() > 0) {
            confidence = Math.min(95, confidence + (keywords.size() * 5) + (medications.size() * 5));
        }
        
        return confidence;
    }

    private String buildProductReason(String category, Set<String> keywords, Set<String> medications) {
        List<String> reasons = new ArrayList<>();
        
        if (!keywords.isEmpty()) {
            reasons.add("based on your health conditions (" + String.join(", ", keywords) + ")");
        }
        
        if (!medications.isEmpty()) {
            reasons.add("based on your current medications (" + String.join(", ", medications) + ")");
        }
        
        if (reasons.isEmpty()) {
            reasons.add("recommended for " + category.toLowerCase().replace("_", " "));
        }
        
        return "Recommended " + String.join(" and ", reasons);
    }

    private String buildReasoningMessage(List<InsuranceClaimDTO> claims, List<PrescriptionDTO> prescriptions) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Based on your ");
        
        List<String> parts = new ArrayList<>();
        if (!claims.isEmpty()) {
            parts.add(claims.size() + " approved health claim(s)");
        }
        if (!prescriptions.isEmpty()) {
            parts.add(prescriptions.size() + " active prescription(s)");
        }
        
        reasoning.append(String.join(" and ", parts));
        reasoning.append(", we've curated these personalized wellness products for you.");
        
        return reasoning.toString();
    }
}
