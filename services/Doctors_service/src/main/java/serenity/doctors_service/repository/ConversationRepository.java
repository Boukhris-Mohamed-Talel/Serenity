package serenity.doctors_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import serenity.doctors_service.entity.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    Optional<Conversation> findByUser2IdAndUser1Id(Long user2Id, Long user1Id);
}
