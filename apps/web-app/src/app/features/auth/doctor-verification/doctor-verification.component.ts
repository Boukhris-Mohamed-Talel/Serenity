import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-doctor-verification',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctor-verification.component.html',
  styleUrl: './doctor-verification.component.scss'
})
export class DoctorVerificationComponent implements OnInit {
  verificationForm!: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';
  currentStep = 1;

  cvFile: File | null = null;
  diplomaFile: File | null = null;
  cvPreview: string | null = null;
  diplomaPreview: string | null = null;

  cvDragOver = false;
  diplomaDragOver = false;

  constructor(private readonly fb: FormBuilder) {}

  ngOnInit(): void {
    this.initForm();
  }

  initForm(): void {
    this.verificationForm = this.fb.group({
      licenseNumber: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(20)]],
      nationalId: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(20)]],
      cv: [null, Validators.required],
      diploma: [null, Validators.required]
    });
  }

  // License Number
  get licenseNumber() {
    return this.verificationForm.get('licenseNumber');
  }

  // National ID
  get nationalId() {
    return this.verificationForm.get('nationalId');
  }

  // CV File handling
  onCvDragOver(event: DragEvent): void {
    event.preventDefault();
    this.cvDragOver = true;
  }

  onCvDragLeave(): void {
    this.cvDragOver = false;
  }

  onCvDropped(event: DragEvent): void {
    event.preventDefault();
    this.cvDragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleCvFile(files[0]);
    }
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleCvFile(input.files[0]);
    }
  }

  handleCvFile(file: File): void {
    const validTypes = ['application/pdf', 'image/png', 'image/jpeg', 'image/jpg'];
    if (!validTypes.includes(file.type)) {
      this.errorMessage = 'CV must be PDF or Image (PNG, JPG)';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'CV file must be less than 5MB';
      return;
    }
    this.cvFile = file;
    this.verificationForm.patchValue({ cv: file });
    this.generatePreview(file, 'cv');
    this.errorMessage = '';
  }

  removeCv(): void {
    this.cvFile = null;
    this.cvPreview = null;
    this.verificationForm.patchValue({ cv: null });
  }

  // Diploma File handling
  onDiplomaDragOver(event: DragEvent): void {
    event.preventDefault();
    this.diplomaDragOver = true;
  }

  onDiplomaDragLeave(): void {
    this.diplomaDragOver = false;
  }

  onDiplomaDropped(event: DragEvent): void {
    event.preventDefault();
    this.diplomaDragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleDiplomaFile(files[0]);
    }
  }

  onDiplomaSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleDiplomaFile(input.files[0]);
    }
  }

  handleDiplomaFile(file: File): void {
    const validTypes = ['application/pdf', 'image/png', 'image/jpeg', 'image/jpg'];
    if (!validTypes.includes(file.type)) {
      this.errorMessage = 'Diploma must be PDF or Image (PNG, JPG)';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'Diploma file must be less than 5MB';
      return;
    }
    this.diplomaFile = file;
    this.verificationForm.patchValue({ diploma: file });
    this.generatePreview(file, 'diploma');
    this.errorMessage = '';
  }

  removeDiploma(): void {
    this.diplomaFile = null;
    this.diplomaPreview = null;
    this.verificationForm.patchValue({ diploma: null });
  }

  generatePreview(file: File, type: 'cv' | 'diploma'): void {
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        if (type === 'cv') {
          this.cvPreview = e.target?.result as string;
        } else {
          this.diplomaPreview = e.target?.result as string;
        }
      };
      reader.readAsDataURL(file);
    }
  }

  nextStep(): void {
    if (this.currentStep === 1) {
      if (this.licenseNumber?.valid && this.nationalId?.valid) {
        this.currentStep = 2;
      }
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  onSubmit(): void {
    if (this.verificationForm.invalid) {
      this.errorMessage = 'Please complete all required fields';
      return;
    }

    this.loading = true;
    // Simulating API call
    setTimeout(() => {
      this.loading = false;
      this.successMessage = 'Verification submitted successfully! We will review your documents.';
      setTimeout(() => {
        this.initForm();
        this.cvFile = null;
        this.diplomaFile = null;
        this.cvPreview = null;
        this.diplomaPreview = null;
        this.currentStep = 1;
        this.successMessage = '';
      }, 2000);
    }, 1500);
  }

  getFileName(file: File | null): string {
    return file ? file.name : '';
  }

  getFileSize(file: File | null): string {
    if (!file) return '';
    const mb = (file.size / (1024 * 1024)).toFixed(2);
    return `${mb} MB`;
  }
}
