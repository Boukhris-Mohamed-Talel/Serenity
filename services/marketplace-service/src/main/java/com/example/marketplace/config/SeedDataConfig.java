package com.example.marketplace.config;

import com.example.marketplace.entity.Product;
import com.example.marketplace.entity.ProductCategory;
import com.example.marketplace.entity.ProductType;
import com.example.marketplace.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedMarketplaceProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            List<Product> products = List.of(
                    Product.builder()
                            .name("Mindfulness Journal")
                            .description("A structured journal with guided prompts to support emotional awareness and daily grounding.")
                            .category(ProductCategory.SELF_CARE)
                            .type(ProductType.PHYSICAL)
                            .price(new BigDecimal("39.90"))
                            .active(true)
                            .imageUrl("https://images.unsplash.com/photo-1455390582262-044cdead277a")
                            .build(),
                    Product.builder()
                            .name("Breathing Companion Audio Pack")
                            .description("Digital audio sessions for stress management, calming transitions, and sleep preparation.")
                            .category(ProductCategory.STRESS_RELIEF)
                            .type(ProductType.DIGITAL)
                            .price(new BigDecimal("24.00"))
                            .active(true)
                            .imageUrl("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4")
                            .build(),
                    Product.builder()
                            .name("Sleep Reset Toolkit")
                            .description("Evidence-informed sleep support kit with routine cards and comfort tools.")
                            .category(ProductCategory.SLEEP_SUPPORT)
                            .type(ProductType.PHYSICAL)
                            .price(new BigDecimal("58.00"))
                            .active(true)
                            .imageUrl("https://images.unsplash.com/photo-1505693416388-ac5ce068fe85")
                            .build(),
                    Product.builder()
                            .name("Cognitive Reframing Mini Course")
                            .description("A concise digital course introducing practical reframing techniques for challenging thought patterns.")
                            .category(ProductCategory.EDUCATION)
                            .type(ProductType.DIGITAL)
                            .price(new BigDecimal("45.50"))
                            .active(true)
                            .imageUrl("https://images.unsplash.com/photo-1522202176988-66273c2fd55f")
                            .build(),
                        Product.builder()
                            .name("Sensory Fidget Focus Kit")
                            .description("A compact fidget and sensory calming set designed to support focus and reduce anxious restlessness during study or work sessions.")
                            .category(ProductCategory.THERAPY_TOOLS)
                            .type(ProductType.PHYSICAL)
                            .price(new BigDecimal("31.20"))
                            .active(true)
                            .imageUrl("https://images.unsplash.com/photo-1556328824-9f0f7f8f66ba")
                            .build(),
                        Product.builder()
                            .name("Anxiety Soothing Weighted Wrap")
                            .description("A soft weighted wrap that provides comforting pressure cues to promote calm and stress relief after long days.")
                            .category(ProductCategory.STRESS_RELIEF)
                            .type(ProductType.PHYSICAL)
                            .price(new BigDecimal("64.90"))
                            .active(true)
                            .imageUrl("https://images.unsplash.com/photo-1515378791036-0648a3ef77b2")
                            .build()
            );

            productRepository.saveAll(products);
        };
    }
}
