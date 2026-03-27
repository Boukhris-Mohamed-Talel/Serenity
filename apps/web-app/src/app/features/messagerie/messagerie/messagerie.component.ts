import { Component, OnInit } from '@angular/core';
import { MessagerieService } from '../../../core/services/messagerie.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-messagerie',
  templateUrl: './messagerie.component.html',
  styleUrls: ['./messagerie.component.scss']
})
export class MessagerieComponent implements OnInit {

  messages: any[] = [];
  newMessage = '';

  menuVisible = false;
  menuX = 0;
  menuY = 0;
  selectedIndex = -1;

  editingIndex = -1;
  editText = '';

  conversations: any[] = [];
  filteredConversations: any[] = [];
  activeConversationId: number | null = null;
  activeConversationName: string = '';

  searchTerm: string = '';
  filteredUsers: any[] = [];
  private searchSubject = new Subject<string>();

  searchActive: boolean = false;

  constructor(private messagerieService: MessagerieService,
              private authService: AuthService
  ) {
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

  ngOnInit() {
    this.filteredConversations = [...this.conversations];
  }

  onSearch() {
    this.searchSubject.next(this.searchTerm);
  }

  onFocusSearch() {
    this.searchActive = true;
  }

  cancelSearch() {
    this.searchActive = false;
    this.searchTerm = '';
    this.filteredUsers = [];
  }

  
selectUser(user: any) {
  const currentUser = this.authService.getCurrentUser();
  console.log('Current user:', currentUser);

  if (!currentUser?.userId) return;

  const currentUserId: number = currentUser.userId;
  console.log('Current user ID:', currentUserId);
  console.log('Selected user ID:', user.id);

  this.messagerieService.startConversation(currentUserId, user.id).subscribe({
  next: (conversation) => {
    console.log('Conversation started:', conversation);

    /*this.activeConversationId = conversation.id;
    this.activeConversationName = `${user.firstName} ${user.lastName}`;*/

    // Récupérer les messages après création
    this.messagerieService.getConversationMessages(conversation.id).subscribe({
      next: (msgs) => {
        this.messages = msgs;
        console.log('Messages récupérés pour la conversation :', this.messages);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des messages :', err);
        this.messages = [];
      }
    });

    if (!this.conversations.find(c => c.id === conversation.id)) {
      this.conversations.push({
        id: conversation.id,
        name: `${user.firstName} ${user.lastName}`,
        lastMessage: ''
      });
    }

    this.filteredConversations = [...this.conversations];
    this.cancelSearch();
  },
  error: (err) => {
    console.error('Erreur lors du démarrage de la conversation:', err);
  }
});
}

selectConversation(convo: any) {
  console.log('Conversation sélectionnée :', convo);

  this.activeConversationId = convo.id;
  this.activeConversationName = convo.name;
  console.log('ID actif :', this.activeConversationId);
  console.log('Nom actif :', this.activeConversationName);

  this.messagerieService.getConversationMessages(convo.id).subscribe({
    next: (msgs) => {
      console.log('Raw messages API :', msgs); // vérifier ce qui arrive

      this.messages = msgs.map(msg => ({
        text: msg.content || '', // utiliser content pour text
        type: msg.senderId === this.authService.getCurrentUser()?.userId ? 'sent' : 'received',
        id: msg.id,
        createdAt: msg.createdAt,
        senderId: msg.senderId
      }));

      console.log('Messages formatés pour le UI :', this.messages);
    },
    error: (err) => {
      console.error('Erreur lors du chargement des messages :', err);
      this.messages = [];
    }
  });
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
}