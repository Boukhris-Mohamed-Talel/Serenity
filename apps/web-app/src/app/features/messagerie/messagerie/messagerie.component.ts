import { Component, OnInit, OnDestroy } from '@angular/core';
import { MessagerieService } from '../../../core/services/messagerie.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { WebSocketService } from '../../../core/services/web-socket.service';

@Component({
  selector: 'app-messagerie',
  templateUrl: './messagerie.component.html',
  styleUrls: ['./messagerie.component.scss']
})
export class MessagerieComponent implements OnInit, OnDestroy {

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
  messageContent: string = '';
  currentUserId: number | null = null;

  private wsSubscription: Subscription | null = null;

  constructor(
    private messagerieService: MessagerieService,
    private authService: AuthService,
    private userService: UserService,
    private webSocketService: WebSocketService
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
    document.addEventListener('click', () => {
      this.menuVisible = false;
    });

    this.filteredConversations = [...this.conversations];
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) return;

    this.currentUserId = currentUser.userId;

    this.messagerieService.getUserConversations(this.currentUserId).subscribe({
      next: (convos) => {
        const otherUserIds = convos.map(c => c.user1Id === this.currentUserId ? c.user2Id : c.user1Id);

        this.userService.getUsersNamesById(otherUserIds).subscribe({
          next: (users) => {
            const usersMap = new Map(users.map(u => [u.id, `${u.firstName} ${u.lastName}`]));
            this.conversations = convos.map(c => ({
              ...c,
              otherUserName: usersMap.get(c.user1Id === this.currentUserId ? c.user2Id : c.user1Id)
            }));
            this.filteredConversations = [...this.conversations];
          },
          error: (err) => console.error('Erreur récupération noms utilisateurs:', err)
        });
      },
      error: (err) => console.error('Erreur chargement conversations:', err)
    });

    // 👇 WebSocket
    this.webSocketService.connect();

    this.wsSubscription = this.webSocketService.newMessage$.subscribe((msg: any) => {
      if (msg.conversationId === this.activeConversationId) {
        const alreadyExists = this.messages.some(m => m.id === msg.id);
        if (!alreadyExists) {
          this.messages.push({
            id: msg.id,
            text: msg.content,
            type: msg.senderId === this.currentUserId ? 'sent' : 'received',
            createdAt: msg.createdAt,
            senderId: msg.senderId
          });
        }
      }
    });
  }

  ngOnDestroy() {
    this.wsSubscription?.unsubscribe();
    this.webSocketService.disconnect();
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
    if (!currentUser?.userId) return;

    const currentUserId: number = currentUser.userId;

    this.messagerieService.startConversation(currentUserId, user.id).subscribe({
      next: (conversation) => {
        this.messagerieService.getConversationMessages(conversation.id).subscribe({
          next: (msgs) => {
            this.messages = msgs;
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
      error: (err) => console.error('Erreur lors du démarrage de la conversation:', err)
    });
  }

  selectConversation(convo: any) {
    this.activeConversationId = convo.id;
    this.activeConversationName = convo.otherUserName;

    this.messagerieService.getConversationMessages(convo.id).subscribe({
      next: (msgs) => {
        this.messages = msgs.map(msg => ({
          text: msg.content || '',
          type: msg.senderId === this.authService.getCurrentUser()?.userId ? 'sent' : 'received',
          id: msg.id,
          createdAt: msg.createdAt,
          senderId: msg.senderId
        }));
      },
      error: (err) => {
        console.error('Erreur lors du chargement des messages :', err);
        this.messages = [];
      }
    });
  }

  sendMessage() {
    if (!this.messageContent.trim()) return;
    if (this.activeConversationId === null || this.currentUserId === null) return;

    const conversationId = this.activeConversationId;
    const senderId = this.currentUserId;
    const content = this.messageContent;

    this.messagerieService.sendMessages(conversationId, senderId, content).subscribe({
      next: (res) => {
        const alreadyExists = this.messages.some(m => m.id === res.id);
        if (!alreadyExists) {
          this.messages.push({
            id: res.id,
            text: res.content,
            type: 'sent',
            createdAt: res.createdAt,
            senderId: res.senderId
          });
        }
        this.messageContent = '';
      },
      error: (err) => console.error('Erreur lors de l\'envoi du message:', err)
    });
  }

  openMenu(event: MouseEvent, index: number) {
    event.preventDefault();
    this.menuVisible = true;
    this.menuX = event.clientX;
    this.menuY = event.clientY;
    this.selectedIndex = index;
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

  startEdit(index: number) {
    this.editingIndex = index;
    this.editText = this.messages[index].text;
    this.menuVisible = false;
  }

  saveEditMessage(index: number) {
    const msg = this.messages[index];
    if (!this.editText.trim() || msg.text === this.editText) {
      this.cancelEdit();
      return;
    }

    this.messagerieService.editMessage(msg.id, this.editText).subscribe({
      next: (updated) => {
        this.messages[index].text = updated.content;
        this.editingIndex = -1;
      },
      error: (err) => {
        console.error('Erreur modification message:', err);
        this.editingIndex = -1;
      }
    });
  }

  cancelEdit() {
    this.editingIndex = -1;
  }

  removeMessage() {
    if (this.selectedIndex < 0) return;
    const msg = this.messages[this.selectedIndex];

    this.messagerieService.deleteMessage(msg.id).subscribe({
      next: () => {
        this.messages.splice(this.selectedIndex, 1);
        this.menuVisible = false;
      },
      error: (err) => console.error('Erreur suppression message:', err)
    });
  }
}