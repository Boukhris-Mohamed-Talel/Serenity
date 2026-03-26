package serenity.doctors_service.service;

import serenity.doctors_service.entity.Conversation;

import java.util.List;
import java.util.Optional;

public interface IConversationService {
    Conversation createConversation(Long user1Id, Long user2Id);

    List<Conversation> getUserConversations(Long userId);

    Optional<Conversation> getConversationById(Long id);

    void deleteConversation(Long id);
}
