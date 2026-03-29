export interface DoctorInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  fullName: string;
}

export interface PatientInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  fullName: string;
  assignedDoctorId?: number;
  assignedDoctorName?: string;
}
