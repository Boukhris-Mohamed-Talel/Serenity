import { Component, OnInit } from '@angular/core';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceOrder } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.scss']
})
export class OrderHistoryComponent implements OnInit {
  loading = false;
  orders: MarketplaceOrder[] = [];

  constructor(private readonly marketplaceService: MarketplaceService) {}

  ngOnInit(): void {
    this.loading = true;
    this.marketplaceService.getMyOrders().subscribe({
      next: orders => {
        this.orders = orders;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
