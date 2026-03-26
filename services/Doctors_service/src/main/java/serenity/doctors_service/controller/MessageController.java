package serenity.doctors_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import serenity.doctors_service.dto.ConversationDTO;
import serenity.doctors_service.dto.MessageDTO;
import serenity.doctors_service.entity.Message;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.service.IMessageService;
import serenity.doctors_service.service.IConversationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final IMessageService messageService;
    private final IConversationService conversationService;

    public MessageController(IMessageService messageService, IConversationService conversationService) {
        this.messageService = messageService;
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestParam Long conversationId,
            @RequestParam Long senderId,
            @RequestParam String content) {
        ConversationDTO conversationDTO = conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        Conversation conversation = new Conversation();
        conversation.setId(conversationDTO.getId()); // juste l'id nécessaire ici
        MessageDTO message = messageService.sendMessage(conversation, senderId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long conversationId) {
        ConversationDTO conversationDTO = conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        Conversation conversation = new Conversation();
        conversation.setId(conversationDTO.getId());
        List<MessageDTO> messages = messageService.getMessages(conversation);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<MessageDTO> editMessage(
            @PathVariable Long messageId,
            @RequestParam String content) {
        MessageDTO updated = messageService.editMessage(messageId, content);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{messageId}/read")
    public ResponseEntity<MessageDTO> markAsRead(@PathVariable Long messageId) {
        MessageDTO updated = messageService.markAsRead(messageId);
        return ResponseEntity.ok(updated);
    }
}