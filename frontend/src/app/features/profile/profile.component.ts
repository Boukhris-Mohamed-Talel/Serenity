import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { UserResponse } from '../../shared/models/user.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  profileForm!: FormGroup;
  user: UserResponse | null = null;
  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';
  editMode = false;
  languages = [
    { value: 'en', label: 'English' },
    { value: 'fr', label: 'Français' },
    { value: 'es', label: 'Español' },
    { value: 'de', label: 'Deutsch' },
    { value: 'ar', label: 'العربية' }
  ];

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      phone: [''],
      dateOfBirth: [''],
      bio: ['', [Validators.maxLength(1000)]],
      avatar: [''],
      preferredLanguage: ['en'],
      isAnonymous: [false]
    });

    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.user = user;
        this.profileForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          phone: user.phone || '',
          dateOfBirth: this.formatDateForInput(user.dateOfBirth),
          bio: user.profile?.bio || '',
          avatar: user.profile?.avatar || '',
          preferredLanguage: user.profile?.preferredLanguage || 'en',
          isAnonymous: user.profile?.isAnonymous || false
        });
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load profile';
        this.loading = false;
      }
    });
  }

  private formatDateForInput(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const year = d.getUTCFullYear();
    const month = String(d.getUTCMonth() + 1).padStart(2, '0');
    const day = String(d.getUTCDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  toggleEdit(): void {
    this.editMode = !this.editMode;
    this.successMessage = '';
    this.errorMessage = '';
  }

  cancelEdit(): void {
    this.editMode = false;
    this.successMessage = '';
    this.errorMessage = '';
    if (this.user) {
      this.profileForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        phone: this.user.phone || '',
        dateOfBirth: this.formatDateForInput(this.user.dateOfBirth),
        bio: this.user.profile?.bio || '',
        avatar: this.user.profile?.avatar || '',
        preferredLanguage: this.user.profile?.preferredLanguage || 'en',
        isAnonymous: this.user.profile?.isAnonymous || false
      });
    }
  }

  onSubmit(): void {
    if (this.profileForm.invalid) return;

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const formValue = { ...this.profileForm.value };
    if (formValue.dateOfBirth) {
      formValue.dateOfBirth = new Date(formValue.dateOfBirth).toISOString();
    } else {
      delete formValue.dateOfBirth;
    }

    this.userService.updateProfile(formValue).subscribe({
      next: (user) => {
        this.user = user;
        this.saving = false;
        this.editMode = false;
        this.successMessage = 'Profile updated successfully';
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message || 'Failed to update profile';
      }
    });
  }

  getInitials(): string {
    if (!this.user) return '?';
    return (this.user.firstName?.charAt(0) || '') + (this.user.lastName?.charAt(0) || '');
  }

  getMemberSince(): string {
    if (!this.user?.createdAt) return '';
    return new Date(this.user.createdAt).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
}
