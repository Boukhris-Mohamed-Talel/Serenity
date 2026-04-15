package serenity.doctors_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import serenity.doctors_service.entity.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    Optional<Conversation> findByUser2IdAndUser1Id(Long user2Id, Long user1Id);

    @Query("""
SELECT c, m
FROM Conversation c
LEFT JOIN c.messages m ON m.createdAt = (
    SELECT MAX(m2.createdAt)
    FROM Message m2
    WHERE m2.conversation = c
)
ORDER BY m.createdAt DESC NULLS LAST
""")
    List<Object[]> findConversationsWithLastMessage();
}
