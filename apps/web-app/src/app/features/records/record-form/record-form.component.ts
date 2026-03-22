import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { MedicalRecordRequest } from '../../../models/medical-record.model';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-record-form',
  templateUrl: './record-form.component.html',
  styleUrls: ['./record-form.component.scss']
})
export class RecordFormComponent implements OnInit {
  patientId: number | null = null;
  recordId: number | null = null;
  isEdit = false;
  loading = false;
  saving = false;

  readonly form = this.fb.group({
    diagnosis: ['', [Validators.required, Validators.maxLength(255)]],
    notes: ['', [Validators.maxLength(2000)]],
    date: ['', [Validators.required]],
    severity: ['MEDIUM', [Validators.required]],
    status: ['ACTIVE', [Validators.required]]
  });

  readonly severities = ['LOW', 'MEDIUM', 'HIGH'] as const;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recordService: MedicalRecordService,
    private readonly auth: AuthService,
    private readonly notification: NotificationService
  ) {}

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    if (!pid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;

    const path = this.route.snapshot.routeConfig?.path;
    if (path === 'new') {
      this.isEdit = false;
    } else if (path === ':recordId/edit') {
      this.isEdit = true;
      const rid = this.route.snapshot.paramMap.get('recordId');
      if (rid) {
        this.recordId = +rid;
        this.loadRecord(this.recordId);
      }
    }
  }

  private loadRecord(id: number): void {
    this.loading = true;
    this.recordService.getRecordById(id).subscribe({
      next: (r) => {
        this.form.patchValue({
          diagnosis: r.diagnosis,
          notes: r.notes ?? '',
          date: r.date.substring(0, 10),
          severity: r.severity,
          status: r.status
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/patients', this.patientId, 'records']);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.patientId == null) {
      this.form.markAllAsTouched();
      return;
    }
    const doctorId = this.auth.getCurrentUser()?.userId;
    if (!doctorId) {
      this.notification.error('Impossible de déterminer le médecin (session).');
      return;
    }

    const v = this.form.getRawValue();
    const body: MedicalRecordRequest = {
      diagnosis: v.diagnosis!.trim(),
      notes: v.notes?.trim() || null,
      date: v.date!,
      severity: v.severity as MedicalRecordRequest['severity'],
      status: v.status!.trim(),
      patientId: this.patientId,
      doctorId
    };

    this.saving = true;
    const req$ =
      this.isEdit && this.recordId
        ? this.recordService.updateRecord(this.recordId, body)
        : this.recordService.createRecord(body);

    req$.subscribe({
      next: (rec) => {
        this.notification.success(this.isEdit ? 'Dossier mis à jour' : 'Dossier créé');
        this.router.navigate(['/patients', this.patientId, 'records']);
        this.saving = false;
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  cancel(): void {
    if (this.patientId != null) {
      this.router.navigate(['/patients', this.patientId, 'records']);
    }
  }
}
