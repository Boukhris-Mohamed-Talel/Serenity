import { Component, OnInit, NgZone, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

declare const google: any;
declare const FB: any;

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit, AfterViewInit {
  @ViewChild('googleBtn') googleBtn!: ElementRef;

  registerForm!: FormGroup;
  errorMessage = '';
  loading = false;
  socialLoading = false;
  googleReady = false;
  facebookReady = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly ngZone: NgZone
  ) { }

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }

    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      phone: [''],
      dateOfBirth: ['']
    });

    this.initFacebookSdk();
  }

  ngAfterViewInit(): void {
    this.initGoogleSignIn();
  }

  onSubmit(): void {
    if (this.registerForm.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    const formValue = { ...this.registerForm.value };
    if (formValue.dateOfBirth) {
      formValue.dateOfBirth = new Date(formValue.dateOfBirth).toISOString();
    } else {
      delete formValue.dateOfBirth;
    }

    this.authService.register(formValue).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }

  signInWithGoogle(): void {
    if (!this.googleReady) {
      this.errorMessage = 'Google Sign-In is still loading. Please try again in a moment.';
      return;
    }
    const btnEl = this.googleBtn?.nativeElement?.querySelector('[role="button"]');
    if (btnEl) {
      btnEl.click();
    } else {
      try {
        google.accounts.id.prompt();
      } catch {
        this.errorMessage = 'Google Sign-In is not available. Please try again shortly.';
      }
    }
  }

  signInWithFacebook(): void {
    if (!this.facebookReady) {
      this.errorMessage = 'Facebook Login is still loading. Please try again in a moment.';
      return;
    }

    this.socialLoading = true;
    this.errorMessage = '';

    try {
      FB.login((response: any) => {
        this.ngZone.run(() => {
          if (response.authResponse) {
            this.authService.loginWithFacebook(response.authResponse.accessToken).subscribe({
              next: () => this.router.navigate(['/']),
              error: (err) => {
                this.socialLoading = false;
                this.errorMessage = err.error?.message || 'Facebook sign-up failed';
              }
            });
          } else {
            this.socialLoading = false;
          }
        });
      }, { scope: 'email,public_profile' });
    } catch {
      this.socialLoading = false;
      this.errorMessage = 'Facebook Login requires HTTPS. It won\'t work on http://localhost.';
    }
  }

  private initGoogleSignIn(): void {
    const checkGoogle = setInterval(() => {
      if (google !== undefined && google.accounts) {
        clearInterval(checkGoogle);
        try {
          google.accounts.id.initialize({
            client_id: environment.googleClientId,
            callback: (response: any) => this.handleGoogleResponse(response),
            use_fedcm_for_prompt: false
          });
          google.accounts.id.renderButton(this.googleBtn.nativeElement, {
            type: 'icon',
            shape: 'circle',
            size: 'small'
          });
          this.googleReady = true;
        } catch {
          console.warn('Google Sign-In initialization failed');
        }
      }
    }, 300);
    setTimeout(() => clearInterval(checkGoogle), 10000);
  }

  private handleGoogleResponse(response: any): void {
    this.ngZone.run(() => {
      this.socialLoading = true;
      this.errorMessage = '';

      this.authService.loginWithGoogle(response.credential).subscribe({
        next: () => this.router.navigate(['/']),
        error: (err) => {
          this.socialLoading = false;
          this.errorMessage = err.error?.message || 'Google sign-up failed';
        }
      });
    });
  }

  private initFacebookSdk(): void {
    const checkFB = setInterval(() => {
      if (FB !== undefined) {
        clearInterval(checkFB);
        try {
          FB.init({
            appId: environment.facebookAppId,
            cookie: true,
            xfbml: true,
            version: 'v19.0'
          });
          this.facebookReady = true;
        } catch {
          console.warn('Facebook SDK initialization failed');
        }
      }
    }, 300);
    setTimeout(() => clearInterval(checkFB), 10000);
  }
}
