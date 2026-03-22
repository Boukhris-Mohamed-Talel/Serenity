import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PharmacyResponse,
  PharmacyUpsertRequest,
  PrescriptionCreateRequest,
  PrescriptionResponse,
  PrescriptionStatusUpdateRequest,
  StockItemCreateRequest,
  StockItemResponse,
  StockQuantityIncrementRequest
} from '../../shared/models/pharmacy.model';

@Injectable({
  providedIn: 'root'
})
export class PharmacyService {

  private readonly API_URL = `${environment.apiUrl}/pharmacy`;

  constructor(private readonly http: HttpClient) {}

  getMyPharmacy(): Observable<PharmacyResponse> {
    return this.http.get<PharmacyResponse>(`${this.API_URL}/me`);
  }

  upsertMyPharmacy(payload: PharmacyUpsertRequest): Observable<PharmacyResponse> {
    return this.http.post<PharmacyResponse>(`${this.API_URL}/me`, payload);
  }

  createPrescription(payload: PrescriptionCreateRequest): Observable<PrescriptionResponse> {
    return this.http.post<PrescriptionResponse>(`${this.API_URL}/prescriptions`, payload);
  }

  getInbox(): Observable<PrescriptionResponse[]> {
    return this.http.get<PrescriptionResponse[]>(`${this.API_URL}/prescriptions/inbox`);
  }

  updatePrescriptionStatus(
    prescriptionId: number,
    payload: PrescriptionStatusUpdateRequest
  ): Observable<PrescriptionResponse> {
    return this.http.patch<PrescriptionResponse>(
      `${this.API_URL}/prescriptions/${prescriptionId}/status`,
      payload
    );
  }

  listStock(query?: string, includeArchived = false): Observable<StockItemResponse[]> {
    const params: string[] = [`includeArchived=${includeArchived}`];
    if (query && query.trim()) {
      params.push(`query=${encodeURIComponent(query.trim())}`);
    }
    return this.http.get<StockItemResponse[]>(`${this.API_URL}/stock?${params.join('&')}`);
  }

  createStockItem(payload: StockItemCreateRequest): Observable<StockItemResponse> {
    return this.http.post<StockItemResponse>(`${this.API_URL}/stock`, payload);
  }

  incrementStockItem(
    stockItemId: number,
    payload: StockQuantityIncrementRequest
  ): Observable<StockItemResponse> {
    return this.http.patch<StockItemResponse>(`${this.API_URL}/stock/${stockItemId}/increment`, payload);
  }

  markOutOfStock(stockItemId: number): Observable<StockItemResponse> {
    return this.http.post<StockItemResponse>(`${this.API_URL}/stock/${stockItemId}/out-of-stock`, {});
  }

  archiveStockItem(stockItemId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/stock/${stockItemId}`);
  }
}
