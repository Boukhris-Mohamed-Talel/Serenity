import { Component, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceUxService } from '../../../core/services/marketplace-ux.service';
import { MARKETPLACE_CATEGORIES, MarketplaceProduct, PreviewContentType } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-product-compare',
  templateUrl: './product-compare.component.html',
  styleUrls: ['./product-compare.component.scss']
})
export class ProductCompareComponent implements OnInit {
  products: MarketplaceProduct[] = [];
  loading = false;

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly ux: MarketplaceUxService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const ids = this.ux.getCompareIds();
    if (ids.length === 0) {
      this.products = [];
      return;
    }
    this.loading = true;
    forkJoin(
      ids.map(id =>
        this.marketplaceService.getProductById(id).pipe(catchError(() => of(null as unknown as MarketplaceProduct)))
      )
    ).subscribe({
      next: rows => {
        this.products = rows.filter((p): p is MarketplaceProduct => !!p && typeof p.id === 'number');
        this.loading = false;
      },
      error: () => {
        this.products = [];
        this.loading = false;
      }
    });
  }

  remove(id: number): void {
    this.ux.removeFromCompare(id);
    this.load();
  }

  clear(): void {
    this.ux.clearCompare();
    this.products = [];
  }

  categoryLabel(cat: string): string {
    return MARKETPLACE_CATEGORIES.find(c => c.value === cat)?.label ?? cat;
  }

  bestFor(product: MarketplaceProduct): string[] {
    const tags: string[] = [];
    switch (product.category) {
      case 'SLEEP_SUPPORT':
        tags.push('Sleep');
        break;
      case 'STRESS_RELIEF':
      case 'MINDFULNESS':
        tags.push('Stress');
        break;
      case 'EDUCATION':
      case 'THERAPY_TOOLS':
        tags.push('Focus');
        break;
      case 'SELF_CARE':
        tags.push('Daily care');
        break;
      default:
        tags.push('Wellness');
    }
    if (product.type === 'DIGITAL') {
      tags.push('On-demand');
    } else {
      tags.push('Shippable');
    }
    return tags;
  }

  previewSummary(product: MarketplaceProduct): string {
    if (product.type !== 'DIGITAL' || !product.previewable) {
      return '—';
    }
    const t = product.previewType as PreviewContentType | undefined;
    if (t === 'AUDIO') {
      return 'Audio · ~90s sample';
    }
    if (t === 'VIDEO') {
      return 'Video · ~30s sample';
    }
    if (t === 'BOOK') {
      return 'Reading · sample pages';
    }
    return 'Digital preview';
  }
}
