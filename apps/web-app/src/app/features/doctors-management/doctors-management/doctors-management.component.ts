import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
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

  constructor(private readonly doctorService: DoctorService) {}

  ngOnInit(): void {
    this.loadDoctors();
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
}
