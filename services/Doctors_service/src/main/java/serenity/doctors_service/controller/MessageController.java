package serenity.doctors_service.controller;

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

    public MessageController(IMessageService messageService,
                             IConversationService conversationService) {
        this.messageService = messageService;
        this.conversationService = conversationService;
    }

    @PostMapping
    public Message sendMessage(@RequestParam Long conversationId,
                               @RequestParam Long senderId,
                               @RequestParam String content) {
        Conversation conversation = conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return messageService.sendMessage(conversation, senderId, content);
    }

    @GetMapping("/conversation/{conversationId}")
    public List<Message> getMessages(@PathVariable Long conversationId) {
        Conversation conversation = conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return messageService.getMessages(conversation);
    }

    @PutMapping("/{id}")
    public Message editMessage(@PathVariable Long id,
                               @RequestParam String newContent) {
        return messageService.editMessage(id, newContent);
    }

    @DeleteMapping("/{id}")
    public void deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
    }

    @PutMapping("/{id}/read")
    public Message markAsRead(@PathVariable Long id) {
        return messageService.markAsRead(id);
    }
}