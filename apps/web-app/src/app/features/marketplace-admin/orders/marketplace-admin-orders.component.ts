import { Component, OnInit } from '@angular/core';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceOrder, MarketplaceOrderStatus } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-marketplace-admin-orders',
  templateUrl: './marketplace-admin-orders.component.html',
  styleUrls: ['./marketplace-admin-orders.component.scss']
})
export class MarketplaceAdminOrdersComponent implements OnInit {
  loading = false;
  orders: MarketplaceOrder[] = [];
  error = '';

  readonly statuses: MarketplaceOrderStatus[] = ['CREATED', 'PAID', 'CANCELLED'];

  constructor(private readonly marketplaceService: MarketplaceService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.error = '';

    this.marketplaceService.getAllOrdersForAdmin().subscribe({
      next: orders => {
        this.orders = orders;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load orders.';
        this.loading = false;
      }
    });
  }

  updateStatus(orderId: number, status: MarketplaceOrderStatus): void {
    this.marketplaceService.updateOrderStatus(orderId, status).subscribe({
      next: updatedOrder => {
        this.orders = this.orders.map(order => order.id === updatedOrder.id ? updatedOrder : order);
      },
      error: () => {
        this.error = 'Failed to update order status.';
      }
    });
  }

  cancelOrder(orderId: number): void {
    if (!confirm('Cancel this order?')) {
      return;
    }

    this.marketplaceService.cancelOrderForAdmin(orderId).subscribe({
      next: () => {
        this.orders = this.orders.map(order =>
          order.id === orderId ? { ...order, status: 'CANCELLED' } : order
        );
      },
      error: () => {
        this.error = 'Failed to cancel order.';
      }
    });
  }
}
