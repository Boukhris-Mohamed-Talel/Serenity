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

  deleteConfirm: { id: number; name: string } | null = null;

  // Search
  searchName = '';
  isSearching = false;
  searchLoading = false;
  searchResults: Patient[] = [];

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

  search(): void {
    const name = this.searchName.trim();
    if (!name) {
      this.clearSearch();
      return;
    }
    this.isSearching = true;
    this.searchLoading = true;
    this.patientService.searchPatients(name).subscribe({
      next: (results) => {
        this.searchResults = results;
        this.searchLoading = false;
      },
      error: () => {
        this.searchLoading = false;
      }
    });
  }

  clearSearch(): void {
    this.searchName = '';
    this.isSearching = false;
    this.searchResults = [];
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

  openDeletePatient(id: number, name: string): void {
    this.deleteConfirm = { id, name };
  }

  closeDeleteConfirm(): void {
    this.deleteConfirm = null;
  }

  confirmDeletePatient(): void {
    if (!this.deleteConfirm) return;
    const { id } = this.deleteConfirm;
    this.deleteConfirm = null;
    this.patientService.deletePatient(id).subscribe({
      next: () => {
        this.notification.success('Patient deleted');
        this.load();
        if (this.isSearching) this.search();
      },
      error: () => {
        /* toast via interceptor */
      }
    });
  }
}
