import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import {
  MARKETPLACE_CATEGORIES,
  MARKETPLACE_TYPES,
  MarketplaceProduct,
  MarketplaceProductCategory,
  MarketplaceProductType,
  ProductRecommendationItem
} from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit, OnDestroy {
  products: MarketplaceProduct[] = [];
  loading = false;
  query = '';
  selectedCategory: MarketplaceProductCategory | '' = '';
  selectedType: MarketplaceProductType | '' = '';
  resetAnimating = false;
  showQuiz = false;
  quizSubmitting = false;
  quizError = '';
  quizReasoning = '';
  quizRecommendations: ProductRecommendationItem[] = [];
  recentlyAddedProductId: number | null = null;
  cartToastVisible = false;
  cartToastMessage = '';

  anxietyLevel = 3;
  stressLevel = 3;
  sleepNeed = 3;

  private searchDebounceId: number | null = null;
  private readonly searchDebounceMs = 220;
  private addFeedbackTimeoutId: number | null = null;
  private toastTimeoutId: number | null = null;

  readonly categories = MARKETPLACE_CATEGORIES;
  readonly types = MARKETPLACE_TYPES;

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  ngOnDestroy(): void {
    if (this.searchDebounceId !== null) {
      window.clearTimeout(this.searchDebounceId);
      this.searchDebounceId = null;
    }
    if (this.addFeedbackTimeoutId !== null) {
      window.clearTimeout(this.addFeedbackTimeoutId);
      this.addFeedbackTimeoutId = null;
    }
    if (this.toastTimeoutId !== null) {
      window.clearTimeout(this.toastTimeoutId);
      this.toastTimeoutId = null;
    }
  }

  loadProducts(): void {
    this.loading = true;
    this.marketplaceService.getProducts({
      query: this.query,
      category: this.selectedCategory,
      type: this.selectedType
    }).subscribe({
      next: products => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  clearFilters(): void {
    this.query = '';
    this.selectedCategory = '';
    this.selectedType = '';
    this.loadProducts();
  }

  clearFiltersSmooth(): void {
    this.resetAnimating = true;
    this.clearFilters();
    window.setTimeout(() => {
      this.resetAnimating = false;
    }, 320);
  }

  onQueryInputChange(): void {
    if (this.searchDebounceId !== null) {
      window.clearTimeout(this.searchDebounceId);
    }

    this.searchDebounceId = window.setTimeout(() => {
      this.loadProducts();
    }, this.searchDebounceMs);
  }

  toggleQuiz(): void {
    this.showQuiz = !this.showQuiz;
    this.quizError = '';
  }

  submitQuiz(): void {
    this.quizSubmitting = true;
    this.quizError = '';
    this.quizReasoning = '';

    this.marketplaceService.getQuizRecommendations({
      anxietyLevel: this.anxietyLevel,
      stressLevel: this.stressLevel,
      sleepNeed: this.sleepNeed
    }).subscribe({
      next: response => {
        this.quizRecommendations = response.recommendations || [];
        this.quizReasoning = response.reasoning || '';
        this.quizSubmitting = false;
      },
      error: err => {
        this.quizError = err?.error?.message || 'Unable to generate recommendations right now.';
        this.quizSubmitting = false;
      }
    });
  }

  resetQuiz(): void {
    this.anxietyLevel = 3;
    this.stressLevel = 3;
    this.sleepNeed = 3;
    this.quizReasoning = '';
    this.quizError = '';
    this.quizRecommendations = [];
  }

  addToCart(product: MarketplaceProduct): void {
    this.marketplaceService.addToCart(product, 1);
    this.recentlyAddedProductId = product.id;
    this.cartToastMessage = `Added ${product.name} to cart`;
    this.cartToastVisible = true;

    if (this.addFeedbackTimeoutId !== null) {
      window.clearTimeout(this.addFeedbackTimeoutId);
    }
    this.addFeedbackTimeoutId = window.setTimeout(() => {
      this.recentlyAddedProductId = null;
    }, 900);

    if (this.toastTimeoutId !== null) {
      window.clearTimeout(this.toastTimeoutId);
    }
    this.toastTimeoutId = window.setTimeout(() => {
      this.cartToastVisible = false;
    }, 2200);
  }

  openDetails(productId: number): void {
    this.router.navigate(['/marketplace/product', productId]);
  }

  isManager(): boolean {
    return this.authService.hasRole('MARKETPLACE_MANAGER') || this.authService.isAdmin();
  }

  get cartCount(): number {
    return this.marketplaceService.getCartSnapshot().reduce((sum, item) => sum + item.quantity, 0);
  }
}
