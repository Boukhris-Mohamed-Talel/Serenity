package serenity.doctors_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import serenity.doctors_service.entity.Conversation;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
}
