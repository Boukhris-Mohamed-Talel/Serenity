import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { DoctorResponse } from '../../shared/models/doctor.model';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private newDoctorSubject = new Subject<DoctorResponse>();
  public newDoctor$ = this.newDoctorSubject.asObservable();
  private client: any = null;
  private reconnectInterval = 5000; // 5 seconds
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;

  async connect() {
    try {
      // Use dynamic import to load StompClient only when needed
      const { Client } = await import('@stomp/stompjs');

      // Get JWT token from localStorage if available
      const token = localStorage.getItem('authToken') || '';

      const config: any = {
        brokerURL: 'ws://localhost:8082/ws',
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: (frame: any) => {
          console.log('✅ WebSocket/STOMP connected successfully', frame);
          this.reconnectAttempts = 0;

          if (this.client) {
            this.client.subscribe('/topic/doctors', (message: any) => {
              try {
                const doctor = JSON.parse(message.body);
                console.log("📨 New doctor received via WebSocket:", doctor);
                this.newDoctorSubject.next(doctor);
              } catch (error) {
                console.error('❌ Error parsing doctor message:', error);
              }
            });
          }
        },
        onDisconnect: (frame: any) => {
          console.log('⚠️ WebSocket/STOMP disconnected', frame);
          this.attemptReconnect();
        },
        onStompError: (frame: any) => {
          console.error('❌ STOMP error:', frame);
        },
        onWebSocketError: (error: any) => {
          console.error('❌ WebSocket error details:', error);
        }
      };

      // Add Authorization header if token exists
      if (token) {
        config.connectHeaders = { Authorization: `Bearer ${token}` };
      }

      this.client = new Client(config);

      console.log('🔌 Attempting to connect to WebSocket...');
      this.client.activate();
    } catch (error) {
      console.error('❌ Failed to initialize WebSocket:', error);
      this.attemptReconnect();
    }
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`🔄 Attempting to reconnect WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      setTimeout(() => {
        this.connect();
      }, this.reconnectInterval);
    } else {
      console.error('❌ Max WebSocket reconnection attempts reached. WebSocket functionality disabled.');
    }
  }

  disconnect() {
    if (this.client && this.client.active) {
      this.client.deactivate();
      console.log('🔌 WebSocket disconnected');
    }
  }
}