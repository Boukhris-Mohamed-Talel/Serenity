import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MessagerieService {
  constructor(private http: HttpClient) {}

  searchUsers(query: string): Observable<any[]> {
    return this.http.get<any[]>(`http://localhost:8082/api/users/search?q=${query}`);
  }
}