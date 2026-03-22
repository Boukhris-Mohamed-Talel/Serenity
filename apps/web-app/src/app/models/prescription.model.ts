/**
 * Aligné sur PrescriptionResponseDTO
 */
export interface Prescription {
  id: number;
  medicationName: string;
  dosage: string;
  frequency: string;
  startDate: string;
  endDate: string | null;
  instructions: string | null;
  quantity: number;
  status: string;
  medicalRecordId: number;
  patientId: number;
  doctorId: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Aligné sur PrescriptionRequestDTO
 */
export interface PrescriptionRequest {
  medicationName: string;
  dosage: string;
  frequency: string;
  startDate: string;
  endDate: string | null;
  instructions: string | null;
  quantity: number;
  status: string;
  medicalRecordId: number;
  patientId: number;
  doctorId: number;
}
