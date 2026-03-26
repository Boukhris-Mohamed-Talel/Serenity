import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MarketplaceOrder } from '../../../shared/models/marketplace.model';
import { MarketplaceService } from '../../../core/services/marketplace.service';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent {
  loading = false;
  error = '';
  successOrder: MarketplaceOrder | null = null;

  readonly checkoutForm = this.fb.group({
    shippingAddress: ['', [Validators.required, Validators.maxLength(500)]],
    customerNote: ['', [Validators.maxLength(1000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly marketplaceService: MarketplaceService,
    private readonly router: Router
  ) {}

  get cartTotal(): number {
    return this.marketplaceService.getCartTotal();
  }

  submit(): void {
    this.error = '';
    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      return;
    }

    const shippingAddress = this.checkoutForm.value.shippingAddress || '';
    const customerNote = this.checkoutForm.value.customerNote || undefined;

    if (this.marketplaceService.getCartSnapshot().length === 0) {
      this.error = 'Your cart is empty.';
      return;
    }

    this.loading = true;
    this.marketplaceService.checkout(shippingAddress, customerNote).subscribe({
      next: order => {
        this.successOrder = order;
        this.loading = false;
      },
      error: () => {
        this.error = 'Checkout failed. Please try again.';
        this.loading = false;
      }
    });
  }

  goToOrders(): void {
    this.router.navigate(['/marketplace/orders']);
  }
}
