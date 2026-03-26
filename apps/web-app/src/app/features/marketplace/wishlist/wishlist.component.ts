import { Component, OnInit } from '@angular/core';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { MarketplaceProduct } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-wishlist',
  templateUrl: './wishlist.component.html',
  styleUrls: ['./wishlist.component.scss']
})
export class WishlistComponent implements OnInit {
  wishlistItems: any[] = [];
  loading = false;
  errorMessage = '';
  userId: number | null = null;

  constructor(
    private marketplaceService: MarketplaceService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userId = this.authService.getUserId();
    if (this.userId) {
      this.loadWishlist();
    }
  }

  loadWishlist(): void {
    if (!this.userId) return;
    
    this.loading = true;
    this.errorMessage = '';
    this.marketplaceService.getUserWishlist().subscribe({
      next: (items) => {
        this.wishlistItems = items;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load wishlist';
        console.error('Error loading wishlist:', err);
        this.loading = false;
      }
    });
  }

  removeFromWishlist(productId: number): void {
    if (!this.userId) return;

    this.marketplaceService.removeFromWishlist(productId).subscribe({
      next: () => {
        this.wishlistItems = this.wishlistItems.filter(item => item.productId !== productId);
      },
      error: (err) => {
        this.errorMessage = 'Failed to remove item from wishlist';
        console.error('Error removing from wishlist:', err);
      }
    });
  }

  viewProduct(productId: number): void {
    this.router.navigate(['/marketplace/product', productId]);
  }

  addToCart(productId: number): void {
    const item = this.wishlistItems.find(w => w.productId === productId);
    if (item) {
      const product: MarketplaceProduct = {
        id: item.productId,
        name: item.productName,
        description: 'Saved wishlist item',
        category: 'SELF_CARE',
        type: 'PHYSICAL',
        price: item.productPrice,
        active: true,
        imageUrl: item.productImageUrl
      };
      this.marketplaceService.addToCart(product);
      this.router.navigate(['/marketplace/cart']);
    }
  }

  goToMarketplace(): void {
    this.router.navigate(['/marketplace']);
  }
}
