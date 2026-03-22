import { Component, OnInit } from '@angular/core';
import { PatientService } from '../../../core/services/patient.service';
import { PageResponseDTO } from '../../../models/page-response.model';
import { Patient } from '../../../models/patient.model';
import { NotificationService } from '../../../shared/services/notification.service';

@Component({
  selector: 'app-patient-list',
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.scss']
})
export class PatientListComponent implements OnInit {
  page: PageResponseDTO<Patient> | null = null;
  loading = false;
  pageIndex = 0;
  readonly pageSize = 10;

  constructor(
    private readonly patientService: PatientService,
    private readonly notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.patientService
      .getAllPatients({
        page: this.pageIndex,
        size: this.pageSize,
        sortBy: 'id',
        direction: 'asc'
      })
      .subscribe({
        next: (p) => {
          this.page = p;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  prev(): void {
    if (this.page && !this.page.first) {
      this.pageIndex--;
      this.load();
    }
  }

  next(): void {
    if (this.page && !this.page.last) {
      this.pageIndex++;
      this.load();
    }
  }

  deletePatient(id: number, name: string): void {
    if (!confirm(`Supprimer le patient ${name} ?`)) return;
    this.patientService.deletePatient(id).subscribe({
      next: () => {
        this.notification.success('Patient supprimé');
        this.load();
      },
      error: () => {
        /* toast via interceptor */
      }
    });
  }
}
