import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DoctorResponse } from '../../shared/models/doctor.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DoctorService {
  private readonly API_URL = `${environment.apiUrl}/doctors`;

  constructor(private readonly http: HttpClient) {}

  getDoctors(): Observable<DoctorResponse[]> {
    return this.http.get<DoctorResponse[]>(this.API_URL);
  }

  getDoctorById(id: number): Observable<DoctorResponse> {
    return this.http.get<DoctorResponse>(`${this.API_URL}/${id}`);
  }

  verifyDoctor(doctorId: number): Observable<any> {
    return this.http.put(`${this.API_URL}/VerifyDoctor/${doctorId}`, {});
  }

  deleteDoctor(doctorId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/${doctorId}`);
  }
}
