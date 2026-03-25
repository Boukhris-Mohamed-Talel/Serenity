import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { DoctorService } from '../../../core/services/doctor.service';
import { DoctorVerificationService } from '../../../core/services/doctor-verification.service';
import { DoctorResponse } from '../../../shared/models/doctor.model';
import { DoctorVerification } from '../../../shared/models/doctor-verification.model';

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

  showVerificationModal = false;
  selectedDoctor: DoctorResponse | null = null;
  selectedVerification: DoctorVerification | null = null;
  verificationLoading = false;
  verificationError: string | null = null;

  constructor(
    private readonly doctorService: DoctorService,
    private readonly doctorVerificationService: DoctorVerificationService,
    private readonly httpClient: HttpClient,
    private readonly sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.loadDoctors();
  }

  onViewVerification(doctor: DoctorResponse): void {
    console.log('onViewVerification called for doctor:', doctor);
    this.selectedDoctor = doctor;
    this.showVerificationModal = true;
    this.verificationLoading = true;
    this.verificationError = null;
    this.selectedVerification = null;

    this.doctorVerificationService.getVerificationByDoctorId(doctor.id).subscribe({
      next: (verification) => {
        console.log('Verification data received:', verification);
        if (verification === null) {
          console.log('No verification record found for doctor:', doctor.id);
          this.verificationError = 'No verification record found for this doctor';
          this.selectedVerification = null;
        } else {
          console.log('Verification ID:', verification.verification_id);
          console.log('Verification status:', verification.status);
          console.log('Verification licenseNumber:', verification.licenseNumber);
          console.log('Verification cv:', verification.cv);
          console.log('Verification diploma:', verification.diploma);
          console.log('Verification nationalId:', verification.nationalId);
          console.log('Verification submittedAt:', verification.submittedAt);
          this.selectedVerification = verification;
        }
        this.verificationLoading = false;
        console.log('selectedVerification set:', this.selectedVerification);
      },
      error: (err) => {
        console.error('Error loading verification:', err);
        this.verificationError = 'Failed to load verification details';
        this.verificationLoading = false;
      }
    });
  }

  closeVerificationModal(): void {
    this.showVerificationModal = false;
    this.selectedVerification = null;
    this.selectedDoctor = null;
  }

  getStatusBadgeClass(status: string | undefined): string {
    if (!status) return 'badge-warning';
    switch (status) {
      case 'VERIFIED':
        return 'badge-success';
      case 'REJECTED':
        return 'badge-danger';
      case 'PENDING':
      default:
        return 'badge-warning';
    }
  }

  getStatusLabel(status: string | undefined): string {
    if (!status) return 'Pending';
    return status.charAt(0) + status.slice(1).toLowerCase();
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
