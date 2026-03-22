import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { Prescription } from '../../../models/prescription.model';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-prescription-list',
  templateUrl: './prescription-list.component.html',
  styleUrls: ['./prescription-list.component.scss']
})
export class PrescriptionListComponent implements OnInit {
  patientId: number | null = null;
  recordId: number | null = null;
  prescriptions: Prescription[] = [];
  loading = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly prescriptionService: PrescriptionService,
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
    this.load();
  }

  load(): void {
    if (this.recordId == null) return;
    this.loading = true;
    this.prescriptionService.getPrescriptionsByRecordId(this.recordId).subscribe({
      next: (list) => {
        this.prescriptions = list;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  deletePrescription(id: number): void {
    if (!confirm('Supprimer cette prescription ?')) return;
    this.prescriptionService.deletePrescription(id).subscribe({
      next: () => {
        this.notification.success('Prescription supprimée');
        this.load();
      },
      error: () => {
        /* toast */
      }
    });
  }
}
