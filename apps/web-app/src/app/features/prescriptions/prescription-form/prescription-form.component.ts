import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { MedicineService } from '../../../core/services/medicine.service';
import { PrescriptionItemRequest, PrescriptionRequest } from '../../../models/prescription.model';
import { Medicine } from '../../../models/medicine.model';
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

  medicines: Medicine[] = [];

  readonly form = this.fb.group({
    status: ['ACTIVE', [Validators.required]],
    items: this.fb.array<FormGroup>([this.createItemGroup()])
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly prescriptionService: PrescriptionService,
    private readonly medicineService: MedicineService,
    private readonly notification: NotificationService
  ) {}

  get itemsArray(): FormArray<FormGroup> {
    return this.form.controls.items;
  }

  addItem(): void {
    this.itemsArray.push(this.createItemGroup());
  }

  removeItem(index: number): void {
    if (this.itemsArray.length <= 1) return;
    this.itemsArray.removeAt(index);
  }

  private createItemGroup(initial?: {
    medicineId?: number;
    dosage?: string;
    frequency?: string;
    quantity?: number;
    startDate?: string;
    endDate?: string;
    instructions?: string;
  }): FormGroup {
    return this.fb.group({
      medicineId: [initial?.medicineId ?? '', [Validators.required]],
      dosage: [initial?.dosage ?? '', [Validators.required, Validators.maxLength(50)]],
      frequency: [initial?.frequency ?? '', [Validators.required, Validators.maxLength(50)]],
      quantity: [initial?.quantity ?? 1, [Validators.required, Validators.min(1)]],
      startDate: [initial?.startDate ?? '', [Validators.required]],
      endDate: [initial?.endDate ?? ''],
      instructions: [initial?.instructions ?? '', [Validators.maxLength(500)]]
    });
  }

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    const rid = getParamFromRouteTree(this.route, 'recordId');
    if (!pid || !rid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;
    this.recordId = +rid;

    this.loadMedicines();

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

  private loadMedicines(): void {
    this.medicineService.getAll().subscribe({
      next: (list) => (this.medicines = list),
      error: () => this.notification.error('Impossible de charger les médicaments')
    });
  }

  private loadPrescription(id: number): void {
    this.loading = true;
    this.prescriptionService.getPrescriptionById(id).subscribe({
      next: (p) => {
        this.form.patchValue({ status: p.status || 'ACTIVE' });
        this.itemsArray.clear();
        const items = p.items?.length ? p.items : [];
        if (!items.length) {
          this.itemsArray.push(this.createItemGroup());
        } else {
          items.forEach(item =>
            this.itemsArray.push(this.createItemGroup({
              medicineId: item.medicine?.id,
              dosage: item.dosage ?? '',
              frequency: item.frequency ?? '',
              quantity: item.quantity ?? 1,
              startDate: item.startDate ? item.startDate.substring(0, 10) : '',
              endDate: item.endDate ? item.endDate.substring(0, 10) : '',
              instructions: item.instructions ?? ''
            }))
          );
        }
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

    const v = this.form.getRawValue();
    const rawItems = (v.items ?? []) as Array<Record<string, unknown>>;
    const items: PrescriptionItemRequest[] = rawItems
      .filter(m => m['medicineId'])
      .map(m => ({
        medicineId: Number(m['medicineId']),
        dosage: String(m['dosage'] ?? '').trim(),
        frequency: String(m['frequency'] ?? '').trim(),
        quantity: Number(m['quantity'] ?? 1),
        startDate: String(m['startDate'] ?? ''),
        endDate: m['endDate'] ? String(m['endDate']) : null,
        instructions: String(m['instructions'] ?? '').trim() || null
      }));

    if (!items.length) {
      this.notification.error('Ajoute au moins un médicament.');
      return;
    }

    const body: PrescriptionRequest = {
      items,
      status: String(v.status ?? 'ACTIVE'),
      medicalRecordId: this.recordId,
      patientId: this.patientId
    };

    this.saving = true;
    const req$ = this.isEdit && this.prescriptionId
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
