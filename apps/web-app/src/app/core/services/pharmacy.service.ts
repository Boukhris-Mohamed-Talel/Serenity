import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PatientDefaultPharmacyRequest,
  PatientDefaultPharmacyResponse,
  PrescriptionAlternativeResponse,
  PrescriptionPharmacyReassignRequest,
  PharmacyCandidateResponse,
  PharmacyResponse,
  PharmacyUpsertRequest,
  PrescriptionResponse,
  PrescriptionStatusUpdateRequest,
  StockItemCreateRequest,
  StockItemRenameRequest,
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

  deleteMyPharmacy(): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/me`);
  }

  getInbox(): Observable<PrescriptionResponse[]> {
    return this.http.get<PrescriptionResponse[]>(`${this.API_URL}/prescriptions/inbox`);
  }

  getMyPrescriptions(): Observable<PrescriptionResponse[]> {
    return this.http.get<PrescriptionResponse[]>(`${this.API_URL}/prescriptions/mine`);
  }

  getPrescriptionById(prescriptionId: number): Observable<PrescriptionResponse> {
    return this.http.get<PrescriptionResponse>(`${this.API_URL}/prescriptions/${prescriptionId}`);
  }

  getPrescriptionAlternatives(
    prescriptionId: number,
    latitude: number,
    longitude: number
  ): Observable<PrescriptionAlternativeResponse> {
    return this.http.get<PrescriptionAlternativeResponse>(
      `${this.API_URL}/prescriptions/${prescriptionId}/alternatives?latitude=${latitude}&longitude=${longitude}`
    );
  }

  reassignPrescriptionPharmacy(
    prescriptionId: number,
    payload: PrescriptionPharmacyReassignRequest
  ): Observable<PrescriptionResponse> {
    return this.http.put<PrescriptionResponse>(`${this.API_URL}/prescriptions/${prescriptionId}/pharmacy`, payload);
  }

  getMyDefaultPharmacy(): Observable<PatientDefaultPharmacyResponse> {
    return this.http.get<PatientDefaultPharmacyResponse>(`${this.API_URL}/patient/default`);
  }

  setMyDefaultPharmacy(payload: PatientDefaultPharmacyRequest): Observable<PatientDefaultPharmacyResponse> {
    return this.http.put<PatientDefaultPharmacyResponse>(`${this.API_URL}/patient/default`, payload);
  }

  listPatientPharmacies(city?: string, governorate?: string): Observable<PharmacyCandidateResponse[]> {
    const params: string[] = [];
    if (city && city.trim()) {
      params.push(`city=${encodeURIComponent(city.trim())}`);
    }
    if (governorate && governorate.trim()) {
      params.push(`governorate=${encodeURIComponent(governorate.trim())}`);
    }

    const query = params.length > 0 ? `?${params.join('&')}` : '';
    return this.http.get<PharmacyCandidateResponse[]>(`${this.API_URL}/patient/pharmacies${query}`);
  }

  suggestNearestPharmacies(
    latitude: number,
    longitude: number,
    radiusKm = 20
  ): Observable<PharmacyCandidateResponse[]> {
    const query = `?latitude=${latitude}&longitude=${longitude}&radiusKm=${radiusKm}`;
    return this.http.get<PharmacyCandidateResponse[]>(`${this.API_URL}/patient/pharmacies/nearest${query}`);
  }

  updatePrescriptionStatus(
    prescriptionId: number,
    payload: PrescriptionStatusUpdateRequest
  ): Observable<PrescriptionResponse> {
    return this.http.post<PrescriptionResponse>(
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

  renameStockItem(stockItemId: number, payload: StockItemRenameRequest): Observable<StockItemResponse> {
    return this.http.patch<StockItemResponse>(`${this.API_URL}/stock/${stockItemId}/rename`, payload);
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

  restoreStockItem(stockItemId: number): Observable<StockItemResponse> {
    return this.http.patch<StockItemResponse>(`${this.API_URL}/stock/${stockItemId}/restore`, {});
  }

  deleteArchivedStockItem(stockItemId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/stock/${stockItemId}/permanent`);
  }
}
