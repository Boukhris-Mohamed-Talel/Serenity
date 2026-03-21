import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PharmacyResponse,
  PharmacyUpsertRequest,
  PrescriptionCreateRequest,
  PrescriptionResponse,
  PrescriptionStatusUpdateRequest
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
}
