import { Component, OnInit } from '@angular/core';
import { MessagerieService } from '../../../core/services/messagerie.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-messagerie',
  templateUrl: './messagerie.component.html',
  styleUrls: ['./messagerie.component.scss']
})
export class MessagerieComponent implements OnInit {

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

  // Barre de recherche et conversations

  conversations: any[] = [
    { id: 1, name: 'John Doe', lastMessage: 'Last message preview...' },
    { id: 2, name: 'Jane Smith', lastMessage: 'Another message...' }
  ];
  filteredConversations: any[] = [];
  activeConversationId: number | null = null;
  activeConversationName: string = '';

  searchTerm: string = '';
filteredUsers: any[] = [];
private searchSubject = new Subject<string>();

constructor(private messagerieService: MessagerieService) {
  this.searchSubject.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    switchMap(term => {
      if (!term.trim()) {
        this.filteredUsers = [];
        return [];
      }
      return this.messagerieService.searchUsers(term);
    })
  ).subscribe(users => this.filteredUsers = users);
}

onSearch() {
  this.searchSubject.next(this.searchTerm);
}

  ngOnInit() {
    this.filteredConversations = [...this.conversations];
  }

  filterConversations() {
    this.filteredConversations = this.conversations.filter(c =>
      c.name.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  selectConversation(convo: any) {
    this.activeConversationId = convo.id;
    this.activeConversationName = convo.name;
    // Vous pouvez charger les messages de cette conversation ici
  }

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

  searchActive: boolean = false; // nouvel état

  onFocusSearch() {
    this.searchActive = true;
  }

  cancelSearch() {
    this.searchActive = false;
    this.searchTerm = '';
    this.filteredUsers = [];
  }

}