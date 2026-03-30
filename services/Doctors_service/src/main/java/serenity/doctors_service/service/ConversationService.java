package serenity.doctors_service.service;

import org.springframework.stereotype.Service;
import serenity.doctors_service.dto.ConversationDTO;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.mapper.ConversationMapper;
import serenity.doctors_service.repository.ConversationRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversationService implements IConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMapper conversationMapper;

    public ConversationService(ConversationRepository conversationRepository, ConversationMapper conversationMapper) {
        this.conversationRepository = conversationRepository;
        this.conversationMapper = conversationMapper;
    }

    @Override
    public ConversationDTO createConversation(Long user1Id, Long user2Id) {
        Conversation conversation = new Conversation();
        conversation.setUser1Id(user1Id);
        conversation.setUser2Id(user2Id);
        Conversation saved = conversationRepository.save(conversation);
        return conversationMapper.toDTO(saved);
    }

    @Override
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUser1IdOrUser2Id(userId, userId);
        return conversations.stream()
                .map(conversationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ConversationDTO> getConversationById(Long id) {
        return conversationRepository.findById(id)
                .map(conversationMapper::toDTO);
    }

    @Override
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }

    @Override
    public Conversation createOrGetConversation(Long user1Id, Long user2Id) {
        Optional<Conversation> existing = conversationRepository
                .findByUser1IdAndUser2Id(user1Id, user2Id)
                .or(() -> conversationRepository.findByUser1IdAndUser2Id(user2Id, user1Id));

        if (existing.isPresent()) return existing.get();

        Conversation conversation = new Conversation();
        conversation.setUser1Id(user1Id);
        conversation.setUser2Id(user2Id);
        return conversationRepository.save(conversation);
    }
}
