import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpResponse,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { DebugSessionService } from '../debug/debug-session.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  private requestSeq = 1;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly debugSessionService: DebugSessionService
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();
    const userId = this.authService.getUserId();
    const requestId = this.requestSeq++;
    const startedAt = performance.now();

    if (token) {
      const headers: Record<string, string> = {
        Authorization: `Bearer ${token}`
      };
      if (userId != null) {
        headers['userId'] = String(userId);
      }

      request = request.clone({
        setHeaders: headers
      });
    }

    this.debugSessionService.log('HTTP', 'HTTP request', {
      requestId,
      method: request.method,
      url: request.urlWithParams
    });

    return next.handle(request).pipe(
      tap(event => {
        if (event instanceof HttpResponse) {
          this.debugSessionService.log('HTTP', 'HTTP response', {
            requestId,
            method: request.method,
            url: request.urlWithParams,
            status: event.status,
            durationMs: Math.round(performance.now() - startedAt)
          });
        }
      }),
      catchError((error: HttpErrorResponse) => {
        this.debugSessionService.log('ERROR', 'HTTP error', {
          requestId,
          method: request.method,
          url: request.urlWithParams,
          status: error.status,
          message: error.message,
          durationMs: Math.round(performance.now() - startedAt)
        }, 'error');

        if (error.status === 401) {
          this.debugSessionService.log('AUTH', 'Unauthorized response, forcing logout', {
            requestId,
            url: request.urlWithParams
          }, 'warn');
          this.authService.logout();
          this.router.navigate(['/auth/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
