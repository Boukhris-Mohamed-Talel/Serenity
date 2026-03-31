import { Component, Input, OnInit } from '@angular/core';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { AuthService } from '../../../core/services/auth.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reviews',
  templateUrl: './reviews.component.html',
  styleUrls: ['./reviews.component.scss']
})
export class ReviewsComponent implements OnInit {
  @Input() productId: number | null = null;

  reviews: any[] = [];
  averageRating = 0;
  loading = false;
  submitting = false;
  userId: number | null = null;
  reviewForm: FormGroup;
  errorMessage = '';
  successMessage = '';
  hoverRating = 0;

  constructor(
    private marketplaceService: MarketplaceService,
    private authService: AuthService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.reviewForm = this.fb.group({
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      reviewText: ['', [Validators.maxLength(1000)]]
    });
  }

  ngOnInit(): void {
    this.userId = this.authService.getUserId();

    if (this.productId) {
      this.loadReviews();
      this.loadAverageRating();
    }
  }

  loadReviews(): void {
    if (!this.productId) return;

    this.loading = true;
    this.marketplaceService.getProductReviews(this.productId).subscribe({
      next: (reviews) => {
        this.reviews = reviews;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadAverageRating(): void {
    if (!this.productId) return;

    this.marketplaceService.getAverageRating(this.productId).subscribe({
      next: (rating) => {
        this.averageRating = Math.round(rating * 10) / 10;
      },
      error: () => {}
    });
  }

  submitReview(): void {
    this.lastActionMessage = 'Submit clicked at ' + new Date().toLocaleTimeString();
    this.errorMessage = '';
    this.successMessage = '';
    this.userId = this.authService.getUserId();

    if (!this.productId) {
      this.errorMessage = 'Product details are still loading. Please try again in a moment.';
      return;
    }

    if (!this.authService.isLoggedIn()) {
      this.errorMessage = 'Please sign in to submit a review. Redirecting to login...';
      this.goToLogin();
      return;
    }

    if (!this.userId) {
      this.errorMessage = 'Your session is missing user information. Please sign out and sign in again.';
      return;
    }

    if (!this.reviewForm.valid) {
      this.reviewForm.markAllAsTouched();
      this.errorMessage = 'Please fix the highlighted fields before submitting.';
      return;
    }

    this.submitting = true;

    const { rating, reviewText } = this.reviewForm.value;
    this.marketplaceService.createOrUpdateReview(
      this.productId,
      Number(rating),
      String(reviewText || '').trim()
    ).subscribe({
      next: () => {
        this.successMessage = 'Review submitted successfully!';
        this.reviewForm.reset({ rating: 5, reviewText: '' });
        this.loadReviews();
        this.loadAverageRating();
        this.submitting = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to submit review: ' + (err.error?.message || '');
        this.submitting = false;
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  setRating(value: number): void {
    this.reviewForm.patchValue({ rating: value });
    this.reviewForm.get('rating')?.markAsTouched();
  }

  setHoverRating(value: number): void {
    this.hoverRating = value;
  }

  clearHoverRating(): void {
    this.hoverRating = 0;
  }

  isStarFilled(star: number): boolean {
    const rating = Number(this.reviewForm.get('rating')?.value || 0);
    const activeValue = this.hoverRating || rating;
    return star <= activeValue;
  }

  currentRatingLabel(): string {
    const rating = Number(this.reviewForm.get('rating')?.value || 0);
    switch (rating) {
      case 1:
        return 'Poor';
      case 2:
        return 'Fair';
      case 3:
        return 'Good';
      case 4:
        return 'Very Good';
      case 5:
        return 'Excellent';
      default:
        return 'Select a rating';
    }
  }

  reviewTextLength(): number {
    return String(this.reviewForm.get('reviewText')?.value || '').length;
  }

  isNearCharacterLimit(): boolean {
    return this.reviewTextLength() >= 850;
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  deleteReview(reviewId: number): void {
    if (!this.userId) return;

    if (confirm('Are you sure you want to delete this review?')) {
      this.marketplaceService.deleteReview(reviewId).subscribe({
        next: () => {
          this.reviews = this.reviews.filter(r => r.id !== reviewId);
          this.loadAverageRating();
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete review';
        }
      });
    }
  }

  setRating(rating: number): void {
    this.reviewForm.patchValue({ rating });
    this.reviewForm.get('rating')?.markAsTouched();
  }

  setHoverRating(rating: number): void {
    this.hoverRating = rating;
  }

  clearHoverRating(): void {
    this.hoverRating = 0;
  }

  isStarFilled(star: number): boolean {
    const selected = Number(this.reviewForm.get('rating')?.value || 0);
    const activeRating = this.hoverRating || selected;
    return star <= activeRating;
  }

  getRatingArray(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i < rating ? 1 : 0);
  }

  isUserReview(reviewUserId: number): boolean {
    return this.userId === reviewUserId;
  }
}
