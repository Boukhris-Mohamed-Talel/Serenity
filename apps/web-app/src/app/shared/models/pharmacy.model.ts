export type PrescriptionStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'READY_FOR_PICKUP'
  | 'COLLECTED'
  | 'EXPIRED';

export interface PharmacyUpsertRequest {
  name: string;
  licenseNumber: string;
  phone?: string;
  openingHours?: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number | null;
  longitude?: number | null;
  supportsEmergency?: boolean;
}

export interface PharmacyResponse {
  id: number;
  ownerUserId: number;
  name: string;
  licenseNumber: string;
  phone?: string;
  openingHours?: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number;
  longitude?: number;
  supportsEmergency?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PrescriptionCreateRequest {
  pharmacyId: number;
  patientId: number;
  patientName: string;
  doctorName: string;
  medicationName: string;
  dosage: string;
  quantity: number;
  instructions?: string;
}

export interface PrescriptionStatusUpdateRequest {
  status: PrescriptionStatus;
  rejectionReason?: string;
}

export interface PrescriptionResponse {
  id: number;
  pharmacyId: number;
  pharmacyName: string;
  doctorId: number;
  patientId: number;
  doctorName: string;
  patientName: string;
  medicationName: string;
  dosage: string;
  quantity: number;
  instructions?: string;
  status: PrescriptionStatus;
  rejectionReason?: string;
  readyAt?: string;
  createdAt: string;
  updatedAt: string;
}
