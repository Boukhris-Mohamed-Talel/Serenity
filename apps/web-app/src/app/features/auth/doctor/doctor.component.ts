import { Component, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-doctor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctor.component.html',
  styleUrl: './doctor.component.scss'
})
export class DoctorComponent {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  doctorForm!: FormGroup;
  selectedImage: string | null = null;
  imageFile: File | null = null;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(private formBuilder: FormBuilder,private router: Router) {
    this.initializeForm();
  }

  initializeForm() {
    this.doctorForm = this.formBuilder.group({
      speciality: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.pattern('^[A-Za-z ]+$') 
        ]
    ],
      faceImage: ['', Validators.required]
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Please select a valid image file';
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.errorMessage = 'Image size must be less than 5MB';
        return;
      }

      this.imageFile = file;
      const reader = new FileReader();
      reader.onload = (e) => {
        this.selectedImage = e.target?.result as string;
        this.doctorForm.get('faceImage')?.setValue(file.name);
      };
      reader.readAsDataURL(file);
      this.errorMessage = '';
    }
  }

  triggerFileInput() {
    this.fileInput?.nativeElement?.click();
  }

  onSubmit() {
    if (this.doctorForm.valid) {
      // Handle form submission
      console.log('Form submitted:', {
        speciality: this.doctorForm.get('speciality')?.value,
        image: this.imageFile
      });
      this.successMessage = 'Profile updated successfully!';
      setTimeout(() => {
        this.successMessage = '';
        this.router.navigate(['/']);
      }, 3000);
    } else {
      this.errorMessage = 'Please fill in all required fields';
    }
  }

  removeImage() {
    this.selectedImage = null;
    this.imageFile = null;
    this.doctorForm.get('faceImage')?.reset();
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }
}
