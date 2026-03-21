import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MoodEntry, MoodEntryRequest, MoodEntryResponse } from '../../shared/models/mood.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class MonitoringService {

  private readonly API_URL = 'http://localhost:8085/api/monitoring/mood';

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService
  ) {}

  /**
   * Create a new mood entry
   */
  createMoodEntry(request: MoodEntryRequest): Observable<MoodEntryResponse> {
    return this.http.post<MoodEntryResponse>(this.API_URL, request);
  }

  /**
   * Get all mood entries for the logged-in user (patient)
   */
  getMoodEntries(patientId: number): Observable<MoodEntryResponse[]> {
    return this.http.get<MoodEntryResponse[]>(`${this.API_URL}?patientId=${patientId}`);
  }

  /**
   * Get all mood entries assigned to a doctor.
   */
  getMoodEntriesForDoctor(doctorId: number): Observable<MoodEntryResponse[]> {
    return this.http.get<MoodEntryResponse[]>(`${this.API_URL}/doctor/${doctorId}`);
  }

  /**
   * Get a specific mood entry by ID
   */
  getMoodEntryById(id: number): Observable<MoodEntryResponse> {
    return this.http.get<MoodEntryResponse>(`${this.API_URL}/${id}`);
  }

  /**
   * Update an existing mood entry
   */
  updateMoodEntry(id: number, request: MoodEntryRequest): Observable<MoodEntryResponse> {
    return this.http.put<MoodEntryResponse>(`${this.API_URL}/${id}`, request);
  }

  /**
   * Delete a mood entry
   */
  deleteMoodEntry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
