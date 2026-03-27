package serenity.doctors_service.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import serenity.doctors_service.dto.MessageDTO;
import serenity.doctors_service.entity.DoctorVerification;

@Component
public class RedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public RedisSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void receiveMessage(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            DoctorVerification verification = mapper.readValue(message, DoctorVerification.class);
            messagingTemplate.convertAndSend("/topic/doctor-verifications", verification);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void receiveChatMessage(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            MessageDTO chatMessage = mapper.readValue(message, MessageDTO.class);
            messagingTemplate.convertAndSend("/topic/chat-messages", chatMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


}