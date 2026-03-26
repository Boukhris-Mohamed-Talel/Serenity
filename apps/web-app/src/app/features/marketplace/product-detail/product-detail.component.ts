import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
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

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly marketplaceService: MarketplaceService
  ) {}

  ngOnInit(): void {
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
      },
      error: () => {
        this.loading = false;
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
}
