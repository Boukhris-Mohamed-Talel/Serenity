package serenity.doctors_service.service;

import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.entity.Message;

import java.util.List;
import java.util.Optional;

public interface IMessageService {
    Message sendMessage(Conversation conversation, Long senderId, String content);

    List<Message> getMessages(Conversation conversation);

    Optional<Message> getMessageById(Long id);

    Message editMessage(Long messageId, String newContent);

    void deleteMessage(Long messageId);

    Message markAsRead(Long messageId);
}
