import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { MedicalRecord } from '../../../models/medical-record.model';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-record-list',
  templateUrl: './record-list.component.html',
  styleUrls: ['./record-list.component.scss']
})
export class RecordListComponent implements OnInit {
  patientId: number | null = null;
  records: MedicalRecord[] = [];
  loading = false;

  deleteConfirm: { id: number } | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recordService: MedicalRecordService,
    private readonly notification: NotificationService
  ) {}

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    if (!pid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;
    this.load();
  }

  load(): void {
    if (this.patientId == null) return;
    this.loading = true;
    this.recordService.getRecordsByPatientId(this.patientId).subscribe({
      next: (list) => {
        this.records = list;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openDeleteRecord(id: number): void {
    this.deleteConfirm = { id };
  }

  closeDeleteConfirm(): void {
    this.deleteConfirm = null;
  }

  confirmDeleteRecord(): void {
    if (!this.deleteConfirm) return;
    const { id } = this.deleteConfirm;
    this.deleteConfirm = null;
    this.recordService.deleteRecord(id).subscribe({
      next: () => {
        this.notification.success('Record deleted');
        this.load();
      },
      error: () => {
        /* toast */
      }
    });
  }
}
