import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MarketplaceRoutingModule } from './marketplace-routing.module';
import { ProductListComponent } from './product-list/product-list.component';
import { ProductDetailComponent } from './product-detail/product-detail.component';
import { CartComponent } from './cart/cart.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { OrderHistoryComponent } from './order-history/order-history.component';
import { WishlistComponent } from './wishlist/wishlist.component';
import { ReviewsComponent } from './reviews/reviews.component';
import { MarketplaceLayoutComponent } from './marketplace-layout/marketplace-layout.component';
import { ProductCompareComponent } from './product-compare/product-compare.component';

@NgModule({
  declarations: [
    MarketplaceLayoutComponent,
    ProductListComponent,
    ProductDetailComponent,
    CartComponent,
    CheckoutComponent,
    OrderHistoryComponent,
    WishlistComponent,
    ReviewsComponent,
    ProductCompareComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    MarketplaceRoutingModule
  ]
})
export class MarketplaceModule {}
