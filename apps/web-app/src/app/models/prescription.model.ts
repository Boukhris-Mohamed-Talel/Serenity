import { Medicine } from './medicine.model';

export interface PrescriptionItem {
  id: number;
  medicine: Medicine;
  dosage: string;
  frequency: string;
  quantity: number;
  startDate: string;
  endDate: string | null;
  instructions: string | null;
}

export interface PrescriptionItemRequest {
  medicineId: number;
  dosage: string;
  frequency: string;
  quantity: number;
  startDate: string;
  endDate?: string | null;
  instructions?: string | null;
}

export interface Prescription {
  id: number;
  medicalRecordId: number;
  patientId: number;
  doctorId: number;
  status: string;
  items: PrescriptionItem[];
  createdAt: string;
  updatedAt: string;
}

export interface PrescriptionRequest {
  medicalRecordId: number;
  patientId: number;
  doctorId?: number;
  status?: string;
  items: PrescriptionItemRequest[];
}
