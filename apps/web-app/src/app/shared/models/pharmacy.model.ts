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
  latitude: number;
  longitude: number;
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

export interface PatientDefaultPharmacyRequest {
  pharmacyId: number;
}

export interface PatientDefaultPharmacyResponse {
  patientId: number;
  pharmacyId: number;
  pharmacyName: string;
  phone?: string;
  openingHours?: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number;
  longitude?: number;
  supportsEmergency?: boolean;
  selectedAt?: string;
}

export interface PharmacyCandidateResponse {
  id: number;
  name: string;
  phone?: string;
  openingHours?: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number;
  longitude?: number;
  supportsEmergency?: boolean;
  distanceKm?: number;
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

export type StockState = 'IN_STOCK' | 'OUT_OF_STOCK';

export interface StockItemCreateRequest {
  medicineName: string;
  quantity: number;
  imageUrl?: string;
  description?: string;
}

export interface StockQuantityIncrementRequest {
  incrementBy: number;
}

export interface StockItemResponse {
  id: number;
  pharmacyId: number;
  medicineName: string;
  quantity: number;
  imageUrl?: string;
  description?: string;
  state: StockState;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}
