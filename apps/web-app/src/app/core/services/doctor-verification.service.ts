import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DoctorVerification } from '../../shared/models/doctor-verification.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DoctorVerificationService {
  private readonly API_URL = `${environment.apiUrl}/doctor-verifications`;

  constructor(private readonly http: HttpClient) {}

  getVerificationByDoctorId(doctorId: number): Observable<DoctorVerification | null> {
    return this.http.get<DoctorVerification[]>(`${this.API_URL}/FindByDoctorID/${doctorId}`).pipe(
      map(response => {
        console.log('API Response (raw):', response);
        // API returns an array, so we take the first element or return null
        return Array.isArray(response) && response.length > 0 ? response[0] : null;
      })
    );
  }

  getVerifications(): Observable<DoctorVerification[]> {
    return this.http.get<DoctorVerification[]>(this.API_URL);
  }

  getVerificationById(id: number): Observable<DoctorVerification> {
    return this.http.get<DoctorVerification>(`${this.API_URL}/${id}`);
  }
}
