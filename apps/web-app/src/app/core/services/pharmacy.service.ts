import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DoctorMedicineSuggestionResponse,
  PatientDefaultPharmacyRequest,
  PatientDefaultPharmacyResponse,
  PharmacyCandidateResponse,
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

  suggestDoctorMedicines(patientId: number, query: string): Observable<DoctorMedicineSuggestionResponse> {
    const encodedQuery = encodeURIComponent(query.trim());
    return this.http.get<DoctorMedicineSuggestionResponse>(
      `${this.API_URL}/doctor/medicine-suggestions?patientId=${patientId}&query=${encodedQuery}`
    );
  }

  getInbox(): Observable<PrescriptionResponse[]> {
    return this.http.get<PrescriptionResponse[]>(`${this.API_URL}/prescriptions/inbox`);
  }

  getMyPrescriptions(): Observable<PrescriptionResponse[]> {
    return this.http.get<PrescriptionResponse[]>(`${this.API_URL}/prescriptions/mine`);
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
