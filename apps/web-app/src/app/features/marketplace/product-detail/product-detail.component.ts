import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { AuthService } from '../../../core/services/auth.service';
import { MarketplaceProduct } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent implements OnInit {
  product: MarketplaceProduct | null = null;
  loading = false;
  quantity = 1;
  inWishlist = false;
  userId: number | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly marketplaceService: MarketplaceService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.userId = this.authService.getUserId();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/marketplace']);
      return;
    }

    this.loading = true;
    this.marketplaceService.getProductById(id).subscribe({
      next: product => {
        this.product = product;
        this.loading = false;
        this.checkWishlistStatus(id);
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  checkWishlistStatus(productId: number): void {
    if (!this.userId) return;
    
    this.marketplaceService.isProductInWishlist(productId).subscribe({
      next: (inWishlist) => {
        this.inWishlist = inWishlist;
      }
    });
  }

  addToCart(): void {
    if (!this.product) {
      return;
    }
    this.marketplaceService.addToCart(this.product, this.quantity);
    this.router.navigate(['/marketplace/cart']);
  }

  toggleWishlist(): void {
    if (!this.product || !this.userId) {
      this.router.navigate(['/auth/login']);
      return;
    }

    if (this.inWishlist) {
      this.marketplaceService.removeFromWishlist(this.product.id).subscribe({
        next: () => {
          this.inWishlist = false;
        }
      });
    } else {
      this.marketplaceService.addToWishlist(this.product.id).subscribe({
        next: () => {
          this.inWishlist = true;
        }
      });
    }
  }
}
