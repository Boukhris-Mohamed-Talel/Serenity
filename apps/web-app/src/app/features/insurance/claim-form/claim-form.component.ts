import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { InsuranceService } from '../../../core/services/insurance.service';
import { INSURANCE_COMPANIES, INSURANCE_GRADES } from '../../../shared/models/insurance.model';

@Component({
  selector: 'app-claim-form',
  templateUrl: './claim-form.component.html',
  styleUrls: ['./claim-form.component.scss']
})
export class ClaimFormComponent {
  claimForm: FormGroup;
  files: File[] = [];
  submitting = false;
  errorMessage = '';

  readonly companies = INSURANCE_COMPANIES;
  readonly grades = INSURANCE_GRADES;
  estimatedReimbursement: number | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly insuranceService: InsuranceService,
    private readonly router: Router
  ) {
    this.claimForm = this.fb.group({
      description: ['', [Validators.required, Validators.minLength(10)]],
      amount: [null, [Validators.required, Validators.min(0.01)]],
      insuranceCompany: ['', Validators.required],
      insuranceGrade: [null, Validators.required]
    });

    this.claimForm.get('amount')?.valueChanges.subscribe(() => this.calculateReimbursement());
    this.claimForm.get('insuranceGrade')?.valueChanges.subscribe(() => this.calculateReimbursement());
  }

  calculateReimbursement(): void {
    const amount = this.claimForm.get('amount')?.value;
    const gradeValue = this.claimForm.get('insuranceGrade')?.value;
    if (amount && gradeValue) {
      const grade = this.grades.find(g => g.value === Number(gradeValue));
      if (grade) {
        this.estimatedReimbursement = Math.round(amount * (grade.percentage / 100) * 100) / 100;
        return;
      }
    }
    this.estimatedReimbursement = null;
  }

  getSelectedGradePercentage(): number | null {
    const gradeValue = this.claimForm.get('insuranceGrade')?.value;
    if (!gradeValue) return null;
    const grade = this.grades.find(g => g.value === Number(gradeValue));
    return grade ? grade.percentage : null;
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      const newFiles = Array.from(input.files);
      this.files = [...this.files, ...newFiles];
    }
  }

  removeFile(index: number): void {
    this.files.splice(index, 1);
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  onSubmit(): void {
    if (this.claimForm.invalid) {
      Object.keys(this.claimForm.controls).forEach(key => {
        this.claimForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    const formValue = {
      ...this.claimForm.value,
      insuranceGrade: Number(this.claimForm.value.insuranceGrade)
    };

    this.insuranceService.submitClaim(formValue, this.files).subscribe({
      next: () => {
        this.router.navigate(['/insurance']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to submit claim. Please try again.';
        this.submitting = false;
      }
    });
  }
}
