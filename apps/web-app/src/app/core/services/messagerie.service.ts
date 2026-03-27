import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';


@Injectable({ providedIn: 'root' })
export class MessagerieService {
  constructor(private http: HttpClient,
              private authService: AuthService
  ) {}

  searchUsers(query: string): Observable<any[]> {
    return this.http.get<any[]>(`http://localhost:8082/api/users/search?q=${query}`);
  }

  startConversation(user1Id: number, user2Id: number): Observable<any> {
  const params = new HttpParams()
      .set('user1Id', user1Id.toString())
      .set('user2Id', user2Id.toString());
    return this.http.post<any>(`http://localhost:8082/api/conversations/start`, {}, { params });

  }

  getConversationMessages(conversationId: number): Observable<any[]> {
    const token = this.authService.getToken();
    return this.http.get<any[]>(
      `http://localhost:8082/api/messages/conversation/${conversationId}`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
  }

  getUserConversations(user_id: number):Observable<any[]> {
    const token = this.authService.getToken();
    return this.http.get<any[]>(
      `http://localhost:8082/api/conversations/user/${user_id}`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
  }

  sendMessages(conversationId: number, senderId: number, content: string): Observable<any> {
    const token = this.authService.getToken();
    const params = `?conversationId=${conversationId}&senderId=${senderId}&content=${encodeURIComponent(content)}`;
    return this.http.post<any>(
      `http://localhost:8082/api/messages${params}`,
      null,
      { headers: { Authorization: `Bearer ${token}` } }
    );
  }
}