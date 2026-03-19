import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-select-role',
  templateUrl: './select-role.component.html',
  styleUrl: './select-role.component.scss'
})
export class SelectRoleComponent {
  selectedRole: string | null = null;

  roles = [
    {
      id: 'patient',
      title: 'I am here seeking help for my medical conditions',
      icon: 'medical_services'
    },
    {
      id: 'doctor',
      title: 'I am a doctor wanting to join your team',
      icon: 'health_and_safety'
    },
    {
      id: 'insurer',
      title: 'I am an Insurer wanting to work with you',
      icon: 'admin_panel_settings'
    }
  ];

  constructor(private router: Router) {}

  selectRole(roleId: string) {
    this.selectedRole = roleId;
  }

  continue() {
    if (this.selectedRole) {
      this.router.navigate(['/auth/register'], { queryParams: { role: this.selectedRole }});
    }
  }
}
