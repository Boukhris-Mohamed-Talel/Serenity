export interface InsuranceClaimRequest {
  description: string;
  amount: number;
  insuranceCompany: string;
  insuranceGrade: number;
}

export interface InsuranceClaimResponse {
  id: number;
  description: string;
  claimDate: string;
  amount: number;
  insuranceCompany: string;
  insuranceGrade: number;
  reimbursementAmount: number;
  status: string;
  externalRef: string;
  filePaths: string[];
  userId: number;
  userFullName: string;
  remboursements: RemboursementResponse[];
}

export const INSURANCE_COMPANIES = [
  'Insurance 1',
  'Insurance 2',
  'Insurance 3',
  'Insurance 4',
  'Insurance 5'
];

export const INSURANCE_GRADES: { value: number; label: string; percentage: number }[] = [
  { value: 1, label: 'Grade 1', percentage: 10 },
  { value: 2, label: 'Grade 2', percentage: 12 },
  { value: 3, label: 'Grade 3', percentage: 18 },
  { value: 4, label: 'Grade 4', percentage: 25 },
  { value: 5, label: 'Grade 5', percentage: 45 }
];

export interface RemboursementResponse {
  id: number;
  montant: number;
  date: string;
  statut: string;
  claimId: number;
}

export interface InsuranceNotification {
  id: number;
  userId: number;
  claimId: number | null;
  type: 'CLAIM_SENT_TO_INSURER' | 'CLAIM_APPROVED' | 'CLAIM_REJECTED' | 'DOCUMENTS_REQUESTED';
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationUnreadCountResponse {
  unreadCount: number;
}
