import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DoctorInfo, PatientInfo } from '../../shared/models/doctor.model';

@Injectable({
  providedIn: 'root'
})
export class DoctorService {

  private readonly API_URL = `${environment.monitoringUrl}/monitoring/doctors`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Get all patients assigned to a doctor (doctor sees names, not IDs).
   */
  getPatientsForDoctor(doctorId: number): Observable<PatientInfo[]> {
    return this.http.get<PatientInfo[]>(`${this.API_URL}/${doctorId}/patients`);
  }

  /**
   * Get the doctor responsible for a patient.
   */
  getDoctorForPatient(patientId: number): Observable<DoctorInfo> {
    return this.http.get<DoctorInfo>(`${this.API_URL}/patients/${patientId}/doctor`);
  }

  /**
   * Assign/reassign doctor to patient (optional helper for future UI flows).
   */
  assignDoctorToPatient(doctorId: number, patientId: number): Observable<PatientInfo> {
    return this.http.post<PatientInfo>(`${this.API_URL}/${doctorId}/patients/${patientId}`, {});
  }
}
