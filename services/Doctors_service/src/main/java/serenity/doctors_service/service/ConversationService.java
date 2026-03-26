package serenity.doctors_service.service;

import org.springframework.stereotype.Service;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.repository.ConversationRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationService implements IConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Override
    public Conversation createConversation(Long user1Id, Long user2Id) {
        Conversation conversation = new Conversation();
        conversation.setUser1Id(user1Id);
        conversation.setUser2Id(user2Id);
        return conversationRepository.save(conversation);
    }

    @Override
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findByUser1IdOrUser2Id(userId, userId);
    }

    @Override
    public Optional<Conversation> getConversationById(Long id) {
        return conversationRepository.findById(id);
    }

    @Override
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }
}
