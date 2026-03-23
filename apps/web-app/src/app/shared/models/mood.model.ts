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

