import { Component } from '@angular/core';

@Component({
  selector: 'app-contrat',
  templateUrl: './contrat.component.html',
  styleUrl: './contrat.component.scss'
})
export class ContratComponent {
  agreed = false;
  agreementTimestamp = '';
 
  handleAgree(): void {
    const now = new Date();
    this.agreementTimestamp = now.toLocaleString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
    this.agreed = true;
  }

}
