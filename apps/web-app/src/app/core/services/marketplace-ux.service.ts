import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import {
  MarketplaceProduct,
  MarketplaceProductCategory,
  MarketplaceProductType,
  MarketplaceSort
} from '../../shared/models/marketplace.model';

const K = {
  COMPARE: 'mp_compare_ids_v1',
  RECENT: 'mp_recent_products_v1',
  HIDDEN: 'mp_hidden_products_v1',
  GOALS: 'mp_goals_profile_v1',
  DISCLAIMER: 'mp_digital_disclaimer_v1',
  PREVIEW: 'mp_preview_progress_v1',
  ADDRESSES: 'mp_address_book_v1',
  SAVED_SEARCHES: 'mp_saved_searches_v1',
  SEARCH_ALERTS: 'mp_search_alerts_v1'
} as const;

export interface GoalsProfile {
  sleep: number;
  stress: number;
  focus: number;
}

export interface SavedSearchEntry {
  id: string;
  name: string;
  query: string;
  category: MarketplaceProductCategory | '';
  type: MarketplaceProductType | '';
  sort: MarketplaceSort;
  savedAt: string;
}

export interface SearchAlertState {
  savedSearchId: string;
  lastMatchCount: number;
  lastTopProductIds: number[];
}

export interface PreviewProgressState {
  bookPage?: number;
  audioTime?: number;
  videoTime?: number;
  updatedAt: string;
}

export interface AddressBookEntry {
  id: string;
  label: string;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class MarketplaceUxService {
  private readonly compareSubject = new BehaviorSubject<number[]>([]);
  readonly compareIds$ = this.compareSubject.asObservable();

  constructor() {
    this.compareSubject.next(this.readCompare());
  }

  getCompareIds(): number[] {
    return [...this.compareSubject.value];
  }

  isInCompare(productId: number): boolean {
    return this.compareSubject.value.includes(productId);
  }

  toggleCompare(productId: number): boolean {
    const cur = [...this.compareSubject.value];
    const i = cur.indexOf(productId);
    if (i >= 0) {
      cur.splice(i, 1);
    } else {
      if (cur.length >= 3) {
        return false;
      }
      cur.push(productId);
    }
    this.compareSubject.next(cur);
    this.writeJson(K.COMPARE, cur);
    return true;
  }

  removeFromCompare(productId: number): void {
    const cur = this.compareSubject.value.filter(id => id !== productId);
    this.compareSubject.next(cur);
    this.writeJson(K.COMPARE, cur);
  }

  clearCompare(): void {
    this.compareSubject.next([]);
    this.writeJson(K.COMPARE, []);
  }

  pushRecentlyViewed(productId: number): void {
    const raw = this.readJson<number[]>(K.RECENT, []);
    const next = [productId, ...raw.filter(id => id !== productId)].slice(0, 24);
    this.writeJson(K.RECENT, next);
  }

  getRecentlyViewedIds(): number[] {
    return this.readJson<number[]>(K.RECENT, []);
  }

  isHidden(productId: number): boolean {
    return this.readJson<number[]>(K.HIDDEN, []).includes(productId);
  }

  hideProduct(productId: number): void {
    const h = new Set(this.readJson<number[]>(K.HIDDEN, []));
    h.add(productId);
    this.writeJson(K.HIDDEN, [...h]);
  }

  clearHiddenProducts(): void {
    this.writeJson(K.HIDDEN, []);
  }

  unhideProduct(productId: number): void {
    this.writeJson(
      K.HIDDEN,
      this.readJson<number[]>(K.HIDDEN, []).filter(id => id !== productId)
    );
  }

  getGoals(): GoalsProfile {
    const d = { sleep: 3, stress: 3, focus: 3 };
    return this.readJson<GoalsProfile>(K.GOALS, d);
  }

  setGoals(goals: GoalsProfile): void {
    this.writeJson(K.GOALS, {
      sleep: clamp1to5(goals.sleep),
      stress: clamp1to5(goals.stress),
      focus: clamp1to5(goals.focus)
    });
  }

  hasDigitalDisclaimerAck(): boolean {
    try {
      return localStorage.getItem(K.DISCLAIMER) === '1';
    } catch {
      return false;
    }
  }

  setDigitalDisclaimerAck(): void {
    try {
      localStorage.setItem(K.DISCLAIMER, '1');
    } catch {
      /* ignore */
    }
  }

  getPreviewProgress(productId: number): PreviewProgressState | null {
    const map = this.readJson<Record<string, PreviewProgressState>>(K.PREVIEW, {});
    return map[String(productId)] ?? null;
  }

  setPreviewProgress(productId: number, patch: Partial<PreviewProgressState>): void {
    const map = this.readJson<Record<string, PreviewProgressState>>(K.PREVIEW, {});
    const prev = map[String(productId)] ?? { updatedAt: new Date().toISOString() };
    map[String(productId)] = { ...prev, ...patch, updatedAt: new Date().toISOString() };
    this.writeJson(K.PREVIEW, map);
  }

  clearPreviewProgress(productId: number): void {
    const map = this.readJson<Record<string, PreviewProgressState>>(K.PREVIEW, {});
    delete map[String(productId)];
    this.writeJson(K.PREVIEW, map);
  }

  getAddressBook(): AddressBookEntry[] {
    return this.readJson<AddressBookEntry[]>(K.ADDRESSES, []);
  }

  saveAddressBook(entries: AddressBookEntry[]): void {
    this.writeJson(K.ADDRESSES, entries.slice(0, 12));
  }

  addAddressEntry(label: string, text: string): void {
    const entries = this.getAddressBook();
    entries.unshift({
      id: `addr_${Date.now()}`,
      label: label.trim() || 'Saved address',
      text: text.trim()
    });
    this.saveAddressBook(entries);
  }

  getSavedSearches(): SavedSearchEntry[] {
    return this.readJson<SavedSearchEntry[]>(K.SAVED_SEARCHES, []);
  }

  saveCurrentSearch(name: string, entry: Omit<SavedSearchEntry, 'id' | 'savedAt' | 'name'>): void {
    const list = this.getSavedSearches();
    const row: SavedSearchEntry = {
      ...entry,
      id: `ss_${Date.now()}`,
      name: name.trim() || 'Saved search',
      savedAt: new Date().toISOString()
    };
    list.unshift(row);
    this.writeJson(K.SAVED_SEARCHES, list.slice(0, 20));
  }

  removeSavedSearch(id: string): void {
    this.writeJson(
      K.SAVED_SEARCHES,
      this.getSavedSearches().filter(s => s.id !== id)
    );
    const alerts = this.getSearchAlerts().filter(a => a.savedSearchId !== id);
    this.writeJson(K.SEARCH_ALERTS, alerts);
  }

  getSearchAlerts(): SearchAlertState[] {
    return this.readJson<SearchAlertState[]>(K.SEARCH_ALERTS, []);
  }

  setAlertForSearch(savedSearchId: string, enabled: boolean): void {
    let alerts = [...this.getSearchAlerts()];
    alerts = alerts.filter(a => a.savedSearchId !== savedSearchId);
    if (enabled) {
      alerts.push({ savedSearchId, lastMatchCount: -1, lastTopProductIds: [] });
    }
    this.writeJson(K.SEARCH_ALERTS, alerts);
  }

  isAlertEnabled(savedSearchId: string): boolean {
    return this.getSearchAlerts().some(a => a.savedSearchId === savedSearchId);
  }

  /**
   * Call after catalog load for a saved search snapshot; returns human messages for new matches.
   */
  consumeSearchAlertNotifications(
    savedSearchId: string,
    matchingProducts: MarketplaceProduct[]
  ): string | null {
    const alerts = this.getSearchAlerts();
    const idx = alerts.findIndex(a => a.savedSearchId === savedSearchId);
    if (idx < 0) {
      return null;
    }
    const ids = matchingProducts.map(p => p.id).slice(0, 30);
    const count = matchingProducts.length;
    const prev = alerts[idx];
    let message: string | null = null;
    if (prev.lastMatchCount >= 0) {
      const newOnes = ids.filter(id => !prev.lastTopProductIds.includes(id));
      if (newOnes.length > 0 || count > prev.lastMatchCount) {
        message =
          newOnes.length > 0
            ? `${newOnes.length} new listing(s) match a saved search you follow.`
            : 'Your followed search has updated results.';
      }
    }
    alerts[idx] = { ...prev, lastMatchCount: count, lastTopProductIds: ids };
    this.writeJson(K.SEARCH_ALERTS, alerts);
    return message;
  }

  scoreForGoals(product: MarketplaceProduct): number {
    const g = this.getGoals();
    let s = 0;
    const cat = product.category;
    if (g.sleep >= 4 && (cat === 'SLEEP_SUPPORT' || cat === 'MINDFULNESS')) {
      s += g.sleep;
    }
    if (g.stress >= 4 && (cat === 'STRESS_RELIEF' || cat === 'MINDFULNESS' || cat === 'SELF_CARE')) {
      s += g.stress;
    }
    if (g.focus >= 4 && (cat === 'EDUCATION' || cat === 'THERAPY_TOOLS')) {
      s += g.focus;
    }
    if (g.sleep >= 4 && cat === 'SLEEP_SUPPORT') {
      s += 2;
    }
    if (g.stress >= 4 && cat === 'STRESS_RELIEF') {
      s += 2;
    }
    return s;
  }

  private readCompare(): number[] {
    const raw = this.readJson<number[]>(K.COMPARE, []);
    return raw.filter((id, i, a) => a.indexOf(id) === i).slice(0, 3);
  }

  private readJson<T>(key: string, fallback: T): T {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) {
        return fallback;
      }
      return JSON.parse(raw) as T;
    } catch {
      return fallback;
    }
  }

  private writeJson(key: string, value: unknown): void {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      /* ignore */
    }
  }
}

function clamp1to5(n: number): number {
  if (!Number.isFinite(n)) {
    return 3;
  }
  return Math.min(5, Math.max(1, Math.round(n)));
}
