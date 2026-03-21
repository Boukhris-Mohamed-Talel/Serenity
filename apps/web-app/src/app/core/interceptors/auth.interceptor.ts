import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Only force logout if the error is from auth endpoints
          // For other services (e.g., monitoring), let the service handle the error gracefully
          const isAuthError = request.url.includes('/auth/') || 
                            request.url.includes('/login') || 
                            request.url.includes('/register');
          
          if (isAuthError) {
            this.authService.logout();
            this.router.navigate(['/auth/login']);
          }
        }
        return throwError(() => error);
      })
    );
  }
}
