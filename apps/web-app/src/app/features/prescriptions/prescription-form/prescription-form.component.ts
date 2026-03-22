import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { PrescriptionRequest } from '../../../models/prescription.model';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-prescription-form',
  templateUrl: './prescription-form.component.html',
  styleUrls: ['./prescription-form.component.scss']
})
export class PrescriptionFormComponent implements OnInit {
  patientId: number | null = null;
  recordId: number | null = null;
  prescriptionId: number | null = null;
  isEdit = false;
  loading = false;
  saving = false;

  readonly form = this.fb.group({
    medicationName: ['', [Validators.required, Validators.maxLength(100)]],
    dosage: ['', [Validators.required, Validators.maxLength(50)]],
    frequency: ['', [Validators.required, Validators.maxLength(50)]],
    startDate: ['', [Validators.required]],
    endDate: [''],
    instructions: ['', [Validators.maxLength(500)]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    status: ['ACTIVE', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly prescriptionService: PrescriptionService,
    private readonly auth: AuthService,
    private readonly notification: NotificationService
  ) {}

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    const rid = getParamFromRouteTree(this.route, 'recordId');
    if (!pid || !rid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;
    this.recordId = +rid;

    const path = this.route.snapshot.routeConfig?.path;
    if (path === 'new') {
      this.isEdit = false;
    } else if (path === ':id/edit') {
      this.isEdit = true;
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.prescriptionId = +id;
        this.loadPrescription(this.prescriptionId);
      }
    }
  }

  private loadPrescription(id: number): void {
    this.loading = true;
    this.prescriptionService.getPrescriptionById(id).subscribe({
      next: (p) => {
        this.form.patchValue({
          medicationName: p.medicationName,
          dosage: p.dosage,
          frequency: p.frequency,
          startDate: p.startDate.substring(0, 10),
          endDate: p.endDate ? p.endDate.substring(0, 10) : '',
          instructions: p.instructions ?? '',
          quantity: p.quantity,
          status: p.status
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        if (this.patientId != null && this.recordId != null) {
          this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
        }
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.patientId == null || this.recordId == null) {
      this.form.markAllAsTouched();
      return;
    }
    const doctorId = this.auth.getCurrentUser()?.userId;
    if (!doctorId) {
      this.notification.error('Impossible de déterminer le médecin (session).');
      return;
    }

    const v = this.form.getRawValue();
    const body: PrescriptionRequest = {
      medicationName: v.medicationName!.trim(),
      dosage: v.dosage!.trim(),
      frequency: v.frequency!.trim(),
      startDate: v.startDate!,
      endDate: v.endDate ? v.endDate : null,
      instructions: v.instructions?.trim() || null,
      quantity: v.quantity ?? 1,
      status: v.status!.trim(),
      medicalRecordId: this.recordId,
      patientId: this.patientId,
      doctorId
    };

    this.saving = true;
    const req$ =
      this.isEdit && this.prescriptionId
        ? this.prescriptionService.updatePrescription(this.prescriptionId, body)
        : this.prescriptionService.createPrescription(body);

    req$.subscribe({
      next: () => {
        this.notification.success(this.isEdit ? 'Prescription mise à jour' : 'Prescription créée');
        this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
        this.saving = false;
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  cancel(): void {
    if (this.patientId != null && this.recordId != null) {
      this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
    }
  }
}
