import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { DoctorResponse } from '../../shared/models/doctor.model';
import { DoctorVerification } from '../../shared/models/doctor-verification.model';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private newDoctorSubject = new Subject<DoctorResponse>();
  public newDoctor$ = this.newDoctorSubject.asObservable();

  private newVerificationSubject = new Subject<DoctorVerification>();
  public newVerification$ = this.newVerificationSubject.asObservable();

  private doctorClient: any = null;
  private verificationClient: any = null;
  private reconnectInterval = 5000;
  private reconnectAttemptsDoctor = 0;
  private reconnectAttemptsVerification = 0;
  private maxReconnectAttempts = 5;

  async connect() {
    try {
      const { Client } = await import('@stomp/stompjs');
      const token = localStorage.getItem('authToken') || '';

      const createClient = (brokerURL: string, onMessage: (message: any) => void) => {
        const config: any = {
          brokerURL,
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          onConnect: () => onMessage(null),
          onDisconnect: () => console.log('⚠️ Disconnected from', brokerURL),
          onStompError: (frame: any) => console.error('❌ STOMP error:', frame),
          onWebSocketError: (error: any) => console.error('❌ WebSocket error details:', error),
        };
        if (token) {
          config.connectHeaders = { Authorization: `Bearer ${token}` };
        }
        return new Client(config);
      };

      this.doctorClient = createClient('ws://localhost:8081/ws', () => {
        this.doctorClient.subscribe('/topic/doctors', (message: any) => {
          try {
            const doctor = JSON.parse(message.body);
            this.newDoctorSubject.next(doctor);
          } catch (error) {
            console.error('❌ Error parsing doctor message:', error);
          }
        });
      });

      this.verificationClient = createClient('ws://localhost:8083/ws-doctor-verification', () => {
        this.verificationClient.subscribe('/topic/doctor-verifications', (message: any) => {
          try {
            const verification = JSON.parse(message.body);
            this.newVerificationSubject.next(verification);
          } catch (error) {
            console.error('❌ Error parsing verification message:', error);
          }
        });
      });

      this.doctorClient.activate();
      this.verificationClient.activate();

      console.log('🔌 Attempting to connect to both WebSocket routes...');
    } catch (error) {
      console.error('❌ Failed to initialize WebSocket:', error);
      this.attemptReconnect();
    }
  }

  private attemptReconnect(): void {
    if (this.reconnectAttemptsDoctor < this.maxReconnectAttempts) {
      this.reconnectAttemptsDoctor++;
      setTimeout(() => this.doctorClient?.activate(), this.reconnectInterval);
    }
    if (this.reconnectAttemptsVerification < this.maxReconnectAttempts) {
      this.reconnectAttemptsVerification++;
      setTimeout(() => this.verificationClient?.activate(), this.reconnectInterval);
    }
  }

  disconnect() {
    if (this.doctorClient?.active) this.doctorClient.deactivate();
    if (this.verificationClient?.active) this.verificationClient.deactivate();
    console.log('🔌 Both WebSockets disconnected');
  }
}