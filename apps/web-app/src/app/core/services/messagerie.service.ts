import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';


@Injectable({ providedIn: 'root' })
export class MessagerieService {
  constructor(private http: HttpClient) {}

  searchUsers(query: string): Observable<any[]> {
    return this.http.get<any[]>(`http://localhost:8082/api/users/search?q=${query}`);
  }

  startConversation(user1Id: number, user2Id: number): Observable<any> {
  const params = new HttpParams()
      .set('user1Id', user1Id.toString())
      .set('user2Id', user2Id.toString());
    return this.http.post<any>(`http://localhost:8082/api/conversations/start`, {}, { params });

  }
}