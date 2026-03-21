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
  doctorId: number;
  doctorName?: string;
  moodScore: number;
  moodDescription: string;
  triggers: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MoodEntry extends MoodEntryResponse {}
