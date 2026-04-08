import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../shared/models/user.model';

type DashboardRole = 'PATIENT' | 'DOCTOR' | 'PHARMACIST';

interface DashboardPerson {
  name: string;
  role: string;
  initials: string;
}

interface DashboardTimelineRow {
  title: string;
  when: string;
}

interface DashboardListRow {
  name: string;
  schedule: string;
}

interface DashboardMetric {
  label: string;
  value: number;
  unit: string;
  alt?: boolean;
}

interface DashboardPreset {
  greeting: string;
  subGreeting: string;
  focusTitle: string;
  focusSubtitle: string;
  focusTask: string;
  focusChecklist: string;
  focusNote: string;
  metricsTitle: string;
  metricRows: [DashboardMetric, DashboardMetric];
  circleTitle: string;
  circleRows: DashboardPerson[];
  contactLabel: string;
  timelineTitle: string;
  timelineSubtitle: string;
  timelineRows: DashboardTimelineRow[];
  listTitle: string;
  listSubtitle: string;
  listRows: DashboardListRow[];
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  private user: UserResponse | null = null;
  readonly patientPreset: DashboardPreset = {
    greeting: 'Let’s personalize your care journey.',
    subGreeting: 'Daily mindfulness',
    focusTitle: 'Daily Focus',
    focusSubtitle: 'Daily mindfulness',
    focusTask: "Today’s Task: Deep Breathing (10 min)",
    focusChecklist: 'Guided Meditation',
    focusNote: 'Add a note for your current mood and comfort level.',
    metricsTitle: 'My Vitals',
    metricRows: [
      { label: 'Heart Rate (last 24h)', value: 72, unit: 'bpm' },
      { label: 'Sleep Quality', value: 8.5, unit: 'hrs', alt: true }
    ],
    circleTitle: 'Care Circle',
    circleRows: [
      { name: 'Dr. Elara Vance', role: 'Primary', initials: 'EV' },
      { name: 'Nurse Chen', role: 'Coordinator', initials: 'NC' }
    ],
    contactLabel: 'Contact Care Circle',
    timelineTitle: 'Upcoming Appointments',
    timelineSubtitle: 'Next appts',
    timelineRows: [
      { title: 'First Session - Dr. X', when: 'April 2, 10:00 AM' },
      { title: 'Follow-up - Dr. X', when: 'April 12, 7:00 AM' },
      { title: 'Next Checkup - Dr. X', when: 'April 22, 8:00 AM' }
    ],
    listTitle: 'Current Medications',
    listSubtitle: 'Prescribed meds',
    listRows: [
      { name: 'Xanax 5mg', schedule: 'Daily' },
      { name: 'Advil 10mg', schedule: 'Morning' },
      { name: 'Ibuprofen 10mg', schedule: 'Daily' },
      { name: 'Naproxen 10mg', schedule: 'Morning' }
    ]
  };

  readonly doctorPreset: DashboardPreset = {
    greeting: 'Here is your clinical snapshot for today.',
    subGreeting: 'Clinical priorities',
    focusTitle: 'Daily Focus',
    focusSubtitle: 'Clinical priorities',
    focusTask: "Today’s Task: Review all high-risk patients before 12:00 PM",
    focusChecklist: 'Check crisis alerts and pending follow-ups',
    focusNote: 'Capture short care notes to keep the team aligned.',
    metricsTitle: 'Doctor Metrics',
    metricRows: [
      { label: 'Patients Today', value: 14, unit: 'pts' },
      { label: 'Avg Consultation Time', value: 28, unit: 'min', alt: true }
    ],
    circleTitle: 'Care Team',
    circleRows: [
      { name: 'Nurse Amira', role: 'Coordinator', initials: 'NA' },
      { name: 'Pharm. Youssef', role: 'Pharmacy', initials: 'PY' }
    ],
    contactLabel: 'Contact Team',
    timelineTitle: 'Upcoming Consultations',
    timelineSubtitle: 'Today and next sessions',
    timelineRows: [
      { title: 'Patient Follow-up - Sara L.', when: '10:30 AM' },
      { title: 'Initial Assessment - Adam H.', when: '1:00 PM' },
      { title: 'Medication Review - Lina R.', when: '4:15 PM' }
    ],
    listTitle: 'Patients Requiring Attention',
    listSubtitle: 'Priority watchlist',
    listRows: [
      { name: 'Sara L.', schedule: 'Mood dip trend - check today' },
      { name: 'Nour B.', schedule: 'Missed last appointment' },
      { name: 'Adam H.', schedule: 'First consultation pending notes' },
      { name: 'Lina R.', schedule: 'Medication adjustment review' }
    ]
  };

  readonly pharmacistPreset: DashboardPreset = {
    greeting: 'Keep prescriptions flowing and stock ready.',
    subGreeting: 'Pharmacy operations',
    focusTitle: 'Daily Focus',
    focusSubtitle: 'Pharmacy operations',
    focusTask: "Today’s Task: Clear pending prescriptions in the first hour",
    focusChecklist: 'Prioritize low-stock medicine requests',
    focusNote: 'Log substitutions early to avoid pickup delays.',
    metricsTitle: 'Pharmacy Metrics',
    metricRows: [
      { label: 'Pending Prescriptions', value: 18, unit: 'rx' },
      { label: 'Low Stock Medicines', value: 6, unit: 'items', alt: true }
    ],
    circleTitle: 'Pharmacy Team',
    circleRows: [
      { name: 'Dr. Maha S.', role: 'Prescriber', initials: 'MS' },
      { name: 'Ops Karim', role: 'Inventory', initials: 'OK' }
    ],
    contactLabel: 'Contact Prescribers',
    timelineTitle: 'Prescription Queue',
    timelineSubtitle: 'Next workflow tasks',
    timelineRows: [
      { title: 'Ready for pickup confirmations', when: 'Within 30 min' },
      { title: 'Pending validation batch', when: 'Before 2:00 PM' },
      { title: 'Restock critical SKUs', when: 'By end of day' }
    ],
    listTitle: 'Inventory Attention List',
    listSubtitle: 'Critical stock levels',
    listRows: [
      { name: 'Sertraline 50mg', schedule: 'Only 4 units left' },
      { name: 'Melatonin 3mg', schedule: 'Only 3 units left' },
      { name: 'Omega-3 Softgel', schedule: 'Only 5 units left' },
      { name: 'Vitamin D 1000 IU', schedule: 'Only 2 units left' }
    ]
  };

  constructor(
    public readonly authService: AuthService,
    private readonly userService: UserService
  ) {}

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => this.user = user
    });
  }

  getDisplayName(): string {
    if (this.user?.profile?.isAnonymous) return 'Anonymous';
    if (this.user?.firstName) return this.user.firstName;
    return (this.authService.getCurrentUser()?.email || '').split('@')[0];
  }

  get activePreset(): DashboardPreset {
    switch (this.currentRole) {
      case 'DOCTOR':
        return this.doctorPreset;
      case 'PHARMACIST':
        return this.pharmacistPreset;
      default:
        return this.patientPreset;
    }
  }

  get currentRole(): DashboardRole {
    if (this.authService.hasRole('DOCTOR')) return 'DOCTOR';
    if (this.authService.hasRole('PHARMACIST')) return 'PHARMACIST';
    return 'PATIENT';
  }

  onContactCareCircle(): void {
    // Hook up to messaging flow when available.
  }
}