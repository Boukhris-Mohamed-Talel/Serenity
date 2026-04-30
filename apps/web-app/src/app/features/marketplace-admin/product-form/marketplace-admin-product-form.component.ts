import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import {
  MARKETPLACE_CATEGORIES,
  MARKETPLACE_TYPES,
  PreviewContentType,
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
  readonly previewTypes: PreviewContentType[] = ['VIDEO', 'BOOK', 'AUDIO'];

  saving = false;
  loading = false;
  error = '';
  productId: number | null = null;

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    category: ['', Validators.required],
    type: ['PHYSICAL', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.10)]],
    stockQuantity: [{ value: 0 as number | null, disabled: false }, [Validators.required, Validators.min(0)]],
    imageUrl: [''],
    previewable: [false, Validators.required],
    previewType: ['' as PreviewContentType | ''],
    previewUrl: [''],
    contentUrl: [''],
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
      this.applyStockFieldForType();
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
    const isDigital = raw.type === 'DIGITAL';
    if (raw.type === 'PHYSICAL') {
      const stock = Number(raw.stockQuantity);
      if (!Number.isFinite(stock) || stock < 0) {
        this.error = 'Physical products need a valid stock quantity (0 or more).';
        this.saving = false;
        return;
      }
    }
    const previewable = isDigital && Boolean(raw.previewable);
    const payload: MarketplaceProductUpsertRequest = {
      name: raw.name ?? '',
      description: raw.description ?? '',
      category: (raw.category ?? 'SELF_CARE') as MarketplaceProductUpsertRequest['category'],
      type: (raw.type ?? 'PHYSICAL') as MarketplaceProductUpsertRequest['type'],
      price: Number(raw.price) || 0,
      imageUrl: raw.imageUrl ?? undefined,
      previewable,
      previewType: previewable && raw.previewType ? raw.previewType : undefined,
      previewUrl: previewable ? (raw.previewUrl ?? undefined) : undefined,
      contentUrl: isDigital ? (raw.contentUrl ?? undefined) : undefined,
      active: raw.active ?? true,
      stockQuantity: isDigital ? undefined : Number(raw.stockQuantity)
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
        this.error = err.error?.message || 'Failed to save product.';
        this.saving = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/marketplace']);
  }

  get isDigitalSelected(): boolean {
    return this.form.controls.type.value === 'DIGITAL';
  }

  get isPhysicalSelected(): boolean {
    return this.form.controls.type.value === 'PHYSICAL';
  }

  onTypeChanged(): void {
    this.applyStockFieldForType();
    if (!this.isDigitalSelected) {
      this.form.patchValue({
        previewable: false,
        previewType: '',
        previewUrl: '',
        contentUrl: ''
      });
    }
  }

  private applyStockFieldForType(): void {
    const ctrl = this.form.controls.stockQuantity;
    const type = this.form.controls.type.value;
    if (type === 'DIGITAL') {
      ctrl.clearValidators();
      ctrl.setValue(null, { emitEvent: false });
      ctrl.disable({ emitEvent: false });
    } else if (type === 'PHYSICAL') {
      ctrl.enable({ emitEvent: false });
      ctrl.setValidators([Validators.required, Validators.min(0)]);
      if (ctrl.value === null || ctrl.value === undefined) {
        ctrl.setValue(0, { emitEvent: false });
      }
    } else {
      ctrl.clearValidators();
      ctrl.setValue(null, { emitEvent: false });
      ctrl.disable({ emitEvent: false });
    }
    ctrl.updateValueAndValidity({ emitEvent: false });
  }

  private patchProduct(product: MarketplaceProduct): void {
    this.form.patchValue({
      name: product.name,
      description: product.description,
      category: product.category,
      type: product.type,
      price: product.price,
      stockQuantity: product.type === 'PHYSICAL' ? (product.stockQuantity ?? 0) : null,
      imageUrl: product.imageUrl ?? '',
      previewable: product.previewable,
      previewType: product.previewType ?? '',
      previewUrl: product.previewUrl ?? '',
      contentUrl: product.contentUrl ?? '',
      active: product.active
    });
    this.applyStockFieldForType();
  }
}
