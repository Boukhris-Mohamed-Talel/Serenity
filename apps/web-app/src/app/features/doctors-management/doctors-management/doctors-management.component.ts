import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { DoctorService } from '../../../core/services/doctor.service';
import { DoctorResponse } from '../../../shared/models/doctor.model';

@Component({
  selector: 'app-doctors-management',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  providers: [DoctorService],
  templateUrl: './doctors-management.component.html',
  styleUrl: './doctors-management.component.scss'
})
export class DoctorsManagementComponent implements OnInit {
  doctors: DoctorResponse[] = [];
  isLoading = false;
  error: string | null = null;
  imageCache = new Map<string, SafeUrl>();

  constructor(
    private readonly doctorService: DoctorService,
    private readonly httpClient: HttpClient,
    private readonly sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.loadDoctors();
  }

  onViewVerification(doctor: DoctorResponse): void {
    // Placeholder for navigation or modal logic
    alert('View verification for ' + this.getFullName(doctor));
  }

  loadDoctors(): void {
    this.isLoading = true;
    this.error = null;
    this.doctorService.getDoctors().subscribe({
      next: (data) => {
        this.doctors = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading doctors:', err);
        this.error = 'Failed to load doctors';
        this.isLoading = false;
      }
    });
  }

  getFullName(doctor: DoctorResponse): string {
    return `Dr. ${doctor.firstName} ${doctor.lastName}`;
  }

  getProfilePictureUrl(doctor: DoctorResponse): SafeUrl | null {
    if (!doctor.profilePictureUrl) {
      return null;
    }

    // Check if already cached
    if (this.imageCache.has(doctor.profilePictureUrl)) {
      return this.imageCache.get(doctor.profilePictureUrl) || null;
    }

    // Load image through HTTP client (with JWT auth)
    const imageUrl = `http://localhost:8082/${doctor.profilePictureUrl}`;
    this.httpClient.get(imageUrl, { responseType: 'blob' }).subscribe({
      next: (imageBlob) => {
        const objectUrl = URL.createObjectURL(imageBlob);
        const safeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
        this.imageCache.set(doctor.profilePictureUrl, safeUrl);
      },
      error: (err) => {
        console.error('Failed to load image:', imageUrl, err);
      }
    });

    return null;
  }

  onImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    console.error('Image failed to load:', imgElement.src);
    imgElement.style.display = 'none';
  }
}
