package serenity.doctors_service.controller;

import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.service.IConversationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final IConversationService conversationService;

    public ConversationController(IConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public Conversation createConversation(@RequestParam Long user1Id,
                                           @RequestParam Long user2Id) {
        return conversationService.createConversation(user1Id, user2Id);
    }

    @GetMapping("/user/{userId}")
    public List<Conversation> getUserConversations(@PathVariable Long userId) {
        return conversationService.getUserConversations(userId);
    }

    @GetMapping("/{id}")
    public Conversation getConversation(@PathVariable Long id) {
        return conversationService.getConversationById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }

    @DeleteMapping("/{id}")
    public void deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
    }
}
