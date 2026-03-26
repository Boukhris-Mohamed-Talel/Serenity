import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import {
  MARKETPLACE_CATEGORIES,
  MARKETPLACE_TYPES,
  MarketplaceProduct,
  MarketplaceProductUpsertRequest
} from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-marketplace-admin-product-form',
  templateUrl: './marketplace-admin-product-form.component.html',
  styleUrls: ['./marketplace-admin-product-form.component.scss']
})
export class MarketplaceAdminProductFormComponent implements OnInit {
  readonly categories = MARKETPLACE_CATEGORIES;
  readonly types = MARKETPLACE_TYPES;

  saving = false;
  loading = false;
  error = '';
  productId: number | null = null;

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    category: ['', Validators.required],
    type: ['', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.10)]],
    imageUrl: [''],
    active: [true, Validators.required]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly marketplaceService: MarketplaceService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      return;
    }

    this.productId = id;
    this.loading = true;

    this.marketplaceService.getProductById(id).subscribe({
      next: (product) => {
        this.patchProduct(product);
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load product.';
        this.loading = false;
      }
    });
  }

  submit(): void {
    this.error = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    const raw = this.form.getRawValue();
    const payload: MarketplaceProductUpsertRequest = {
      name: raw.name ?? '',
      description: raw.description ?? '',
      category: (raw.category ?? 'SELF_CARE') as MarketplaceProductUpsertRequest['category'],
      type: (raw.type ?? 'PHYSICAL') as MarketplaceProductUpsertRequest['type'],
      price: Number(raw.price) || 0,
      imageUrl: raw.imageUrl ?? undefined,
      active: raw.active ?? true
    };

    const request$ = this.productId
      ? this.marketplaceService.updateProduct(this.productId, payload)
      : this.marketplaceService.createProduct(payload);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.router.navigate(['/admin/marketplace']);
      },
      error: (err) => {
        console.error('Product save error:', err);
        this.error = err.error?.message || 'Failed to save product.';
        this.saving = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/marketplace']);
  }

  private patchProduct(product: MarketplaceProduct): void {
    this.form.patchValue({
      name: product.name,
      description: product.description,
      category: product.category,
      type: product.type,
      price: product.price,
      imageUrl: product.imageUrl ?? '',
      active: product.active
    });
  }
}
