/**
 * Aligné sur PrescriptionResponseDTO
 */
export interface Prescription {
  id: number;
  medications: PrescriptionMedication[];
  medicalRecordId: number;
  patientId: number;
  doctorId: number;
  createdAt: string;
  updatedAt: string;
}

export interface PrescriptionMedication {
  medicationName: string;
  dosage: string;
  frequency: string;
  startDate: string;
  endDate: string | null;
  instructions: string | null;
  quantity: number;
  status: 'ACTIVE' | 'INACTIVE';
}

/**
 * Aligné sur PrescriptionRequestDTO
 */
export interface PrescriptionRequest {
  medications: PrescriptionMedication[];
  medicalRecordId: number;
  patientId: number;
  doctorId?: number;
}
