import { Component } from '@angular/core';

@Component({
  selector: 'app-messagerie',
  templateUrl: './messagerie.component.html',
  styleUrl: './messagerie.component.scss'
})
export class MessagerieComponent {

  messages: any[] = [
    { text: 'Hello!', type: 'received' },
    { text: 'Hi 👋', type: 'sent' }
  ];

  newMessage = '';

  menuVisible = false;
  menuX = 0;
  menuY = 0;
  selectedIndex = -1;

  editingIndex = -1;
  editText = '';

  sendMessage() {
    if (!this.newMessage.trim()) return;

    this.messages.push({
      text: this.newMessage,
      type: 'sent'
    });

    this.newMessage = '';
  }

  openMenu(event: MouseEvent, index: number) {
    event.preventDefault();

    this.menuVisible = true;
    this.menuX = event.clientX;
    this.menuY = event.clientY;
    this.selectedIndex = index;
  }

  deleteMessage() {
    this.messages.splice(this.selectedIndex, 1);
    this.menuVisible = false;
  }

  editMessage() {
    this.editingIndex = this.selectedIndex;
    this.editText = this.messages[this.selectedIndex].text;
    this.menuVisible = false;
  }

  saveEdit() {
    this.messages[this.editingIndex].text = this.editText;
    this.editingIndex = -1;
  }

}
