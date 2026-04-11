export interface MoodEntryRequest {
  patientId: number;
  moodScore: number;  // 1-10
  moodDescription: string;   // Description of mood
  triggers?: string;   // Optional emotional triggers
}

export interface MoodEntryResponse {
  id: number;
  patientId: number;
  patientName?: string;
  /** From user_profiles.avatar (when set in Profile). */
  patientAvatarUrl?: string | null;
  doctorId: number;
  doctorName?: string;
  moodScore: number;
  moodDescription: string;
  triggers: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MoodEntry extends MoodEntryResponse {}

export interface EmotionalTriggerRequest {
  moodEntryId: number;
  triggerType: string;
  description: string;
  intensity: number;
}

export interface EmotionalTriggerResponse {
  id: number;
  moodEntryId: number;
  doctorId: number;
  triggerType: string;
  description: string;
  intensity: number;
  recordedAt: string;
}

export interface CrisisAlertPayload {
  doctorId: number;
  patientId: number;
  patientFullName: string;
  moodLevel: number;
  message: string;
  timestamp: string;
}

export interface MoodTrendPoint {
  date: string;
  averageMood: number;
  entryCount: number;
  crisisCount: number;
}

export interface PatientMoodPoint {
  patientId: number;
  patientName: string;
  patientAvatarUrl?: string | null;
  x: number;
  latestMoodScore: number;
  averageMoodScore: number;
  moodChange: number;
  entryCount: number;
  crisisCount: number;
  latestEntryAt: string;
  latestTriggerType?: string | null;
  latestTriggerDescription?: string | null;
  latestTriggerIntensity?: number | null;
  latestTriggerAt?: string | null;
}

export interface DoctorMonitoringDashboard {
  totalPatients: number;
  totalMoodEntries: number;
  totalClinicalTriggers: number;
  crisisEvents: number;
  averageMood: number;
  averageMoodChange: number;
  activeHighRiskPatients: number;
  moodTrend: MoodTrendPoint[];
  patientPoints: PatientMoodPoint[];
}

