import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponseDTO } from '../../models/api-response.model';
import { PageResponseDTO } from '../../models/page-response.model';
import { PageQuery } from '../../models/page-query.model';
import { Patient, PatientRequest } from '../../models/patient.model';
import { unwrapApiResponse } from '../../shared/utils/api-response.utils';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly base = environment.medicalApiUrl;

  constructor(private readonly http: HttpClient) {}

  getAllPatients(query: PageQuery = {}): Observable<PageResponseDTO<Patient>> {
    const params = this.buildPageParams(query);
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<PageResponseDTO<Patient>>>(`${this.base}/patients`, { params })
    );
  }

  getPatientById(id: number): Observable<Patient> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<Patient>>(`${this.base}/patients/${id}`)
    );
  }

  createPatient(body: PatientRequest): Observable<Patient> {
    return unwrapApiResponse(
      this.http.post<ApiResponseDTO<Patient>>(`${this.base}/patients`, body)
    );
  }

  updatePatient(id: number, body: PatientRequest): Observable<Patient> {
    return unwrapApiResponse(
      this.http.put<ApiResponseDTO<Patient>>(`${this.base}/patients/${id}`, body)
    );
  }

  deletePatient(id: number): Observable<void> {
    return this.http.delete<ApiResponseDTO<unknown>>(`${this.base}/patients/${id}`).pipe(map(() => undefined));
  }

  searchPatients(name: string): Observable<Patient[]> {
    const params = new HttpParams().set('name', name);
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<Patient[]>>(`${this.base}/patients/search`, { params })
    );
  }

  private buildPageParams(q: PageQuery): HttpParams {
    let p = new HttpParams();
    if (q.page !== undefined) p = p.set('page', String(q.page));
    if (q.size !== undefined) p = p.set('size', String(q.size));
    if (q.sortBy !== undefined) p = p.set('sortBy', q.sortBy);
    if (q.direction !== undefined) p = p.set('direction', q.direction);
    return p;
  }
}
