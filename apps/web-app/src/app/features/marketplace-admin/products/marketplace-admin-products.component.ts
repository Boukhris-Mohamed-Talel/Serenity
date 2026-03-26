import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceProduct } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-marketplace-admin-products',
  templateUrl: './marketplace-admin-products.component.html',
  styleUrls: ['./marketplace-admin-products.component.scss']
})
export class MarketplaceAdminProductsComponent implements OnInit {
  loading = false;
  products: MarketplaceProduct[] = [];
  error = '';

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.error = '';

    this.marketplaceService.getAllProductsForAdmin().subscribe({
      next: products => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load products.';
        this.loading = false;
      }
    });
  }

  createProduct(): void {
    this.router.navigate(['/admin/marketplace/products/new']);
  }

  editProduct(productId: number): void {
    this.router.navigate(['/admin/marketplace/products', productId, 'edit']);
  }

  deleteProduct(productId: number): void {
    if (!confirm('Delete this product?')) {
      return;
    }

    this.marketplaceService.deleteProduct(productId).subscribe({
      next: () => this.loadProducts(),
      error: () => {
        this.error = 'Failed to delete product.';
      }
    });
  }
}
