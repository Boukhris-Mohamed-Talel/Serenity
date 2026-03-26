import { Component, Input, Output, EventEmitter } from '@angular/core';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-discount',
  templateUrl: './discount.component.html',
  styleUrls: ['./discount.component.scss']
})
export class DiscountComponent {
  @Input() orderAmount = 0;
  @Output() discountApplied = new EventEmitter<{ code: string; discount: number }>();

  couponForm: FormGroup;
  appliedCoupon: any = null;
  discountAmount = 0;
  errorMessage = '';
  loading = false;

  constructor(
    private marketplaceService: MarketplaceService,
    private fb: FormBuilder
  ) {
    this.couponForm = this.fb.group({
      code: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  validateCoupon(): void {
    if (!this.couponForm.valid) {
      return;
    }

    const code = this.couponForm.get('code')?.value;
    this.loading = true;
    this.errorMessage = '';

    this.marketplaceService.validateCoupon(code, this.orderAmount).subscribe({
      next: (coupon) => {
        this.appliedCoupon = coupon;
        this.discountAmount = Number(coupon.discountAmount);
        this.discountApplied.emit({ code, discount: this.discountAmount });
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Invalid coupon code';
        this.appliedCoupon = null;
        this.discountAmount = 0;
        this.loading = false;
      }
    });
  }

  removeCoupon(): void {
    this.appliedCoupon = null;
    this.discountAmount = 0;
    this.couponForm.reset();
    this.errorMessage = '';
    this.discountApplied.emit({ code: '', discount: 0 });
  }
}
