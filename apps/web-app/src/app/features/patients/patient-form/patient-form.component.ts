import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PatientService } from '../../../core/services/patient.service';
import { PatientRequest } from '../../../models/patient.model';
import { NotificationService } from '../../../shared/services/notification.service';

@Component({
  selector: 'app-patient-form',
  templateUrl: './patient-form.component.html',
  styleUrls: ['./patient-form.component.scss']
})
export class PatientFormComponent implements OnInit {
  /** Chargement initial (édition) */
  loading = false;
  saving = false;
  patientId: number | null = null;
  isEdit = false;

  readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    dateOfBirth: [''],
    gender: ['', [Validators.maxLength(10)]],
    bloodType: ['', [Validators.maxLength(5)]],
    allergies: ['', [Validators.maxLength(500)]],
    phone: ['', [Validators.maxLength(20)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly patientService: PatientService,
    private readonly notification: NotificationService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && this.route.snapshot.routeConfig?.path === ':id/edit') {
      this.isEdit = true;
      this.patientId = +idParam;
      this.loadPatient(this.patientId);
    }
  }

  private loadPatient(id: number): void {
    this.loading = true;
    this.patientService.getPatientById(id).subscribe({
      next: (p) => {
        this.form.patchValue({
          firstName: p.firstName,
          lastName: p.lastName,
          dateOfBirth: p.dateOfBirth ? p.dateOfBirth.substring(0, 10) : '',
          gender: p.gender ?? '',
          bloodType: p.bloodType ?? '',
          allergies: p.allergies ?? '',
          phone: p.phone ?? ''
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: PatientRequest = {
      firstName: v.firstName!.trim(),
      lastName: v.lastName!.trim(),
      dateOfBirth: v.dateOfBirth ? v.dateOfBirth : null,
      gender: v.gender?.trim() || null,
      bloodType: v.bloodType?.trim() || null,
      allergies: v.allergies?.trim() || null,
      phone: v.phone?.trim() || null
    };

    this.saving = true;
    const req$ =
      this.isEdit && this.patientId
        ? this.patientService.updatePatient(this.patientId, body)
        : this.patientService.createPatient(body);

    req$.subscribe({
      next: (p) => {
        this.notification.success(this.isEdit ? 'Patient mis à jour' : 'Patient créé');
        this.router.navigate(['/patients', p.id]);
        this.saving = false;
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  cancel(): void {
    if (this.isEdit && this.patientId) {
      this.router.navigate(['/patients', this.patientId]);
    } else {
      this.router.navigate(['/patients']);
    }
  }
}
