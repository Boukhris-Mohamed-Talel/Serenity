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
  pharmacyId?: number;
  patientId: number;
  patientName: string;
  doctorName: string;
  medicationName?: string;
  dosage?: string;
  quantity?: number;
  instructions?: string;
  medicineLines?: PrescriptionLineCreateRequest[];
}

export interface PrescriptionLineCreateRequest {
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
  pharmacyId?: number;
  pharmacyName?: string;
  doctorId: number;
  patientId: number;
  doctorName: string;
  patientName: string;
  assignedToPharmacy?: boolean;
  assignmentMessage?: string;
  medicationName?: string;
  dosage?: string;
  quantity?: number;
  instructions?: string;
  medicineLines?: PrescriptionLineResponse[];
  status: PrescriptionStatus;
  rejectionReason?: string;
  readyAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AlternativePharmacyOption {
  pharmacyId: number;
  pharmacyName: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number;
  longitude?: number;
  distanceKm?: number;
  availableQuantity?: number;
}

export interface PerMedicineAlternative {
  lineId: number;
  medicationName: string;
  requiredQuantity: number;
  pharmacies: AlternativePharmacyOption[];
}

export type RecommendationMode = 'FULL_MATCH' | 'PARTIAL_FALLBACK' | 'NONE';

export interface PrescriptionAlternativeResponse {
  prescriptionId: number;
  status: PrescriptionStatus;
  latitude: number;
  longitude: number;
  fullMatchRadiusKm: number;
  partialRadiusKm: number;
  recommendedMode: RecommendationMode;
  message: string;
  fullMatchPharmacies: AlternativePharmacyOption[];
  perMedicineAlternatives: PerMedicineAlternative[];
  selectablePharmacies: AlternativePharmacyOption[];
}

export interface PrescriptionPharmacyReassignRequest {
  pharmacyId: number;
  latitude: number;
  longitude: number;
}

export interface PrescriptionLineResponse {
  id: number;
  medicationName: string;
  dosage: string;
  frequency?: string;
  quantity: number;
  startDate?: string;
  endDate?: string;
  instructions?: string;
}

export type DoctorSuggestionStockStatus = 'IN_STOCK' | 'OUT_OF_STOCK' | 'UNRESOLVED';

export interface DoctorMedicineSuggestionItem {
  medicineName: string;
  stockStatus: DoctorSuggestionStockStatus;
  availableQuantity?: number;
  guidanceMessage?: string;
}

export interface DoctorMedicineSuggestionResponse {
  patientId: number;
  hasDefaultPharmacy: boolean;
  pharmacyId?: number;
  pharmacyName?: string;
  guidanceMessage?: string;
  suggestions: DoctorMedicineSuggestionItem[];
}

export interface DoctorPatientSuggestionItem {
  patientId: number;
  displayName: string;
  profilePictureUrl?: string;
}

export interface DoctorPatientSuggestionResponse {
  suggestions: DoctorPatientSuggestionItem[];
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

export interface StockItemRenameRequest {
  medicineName: string;
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
