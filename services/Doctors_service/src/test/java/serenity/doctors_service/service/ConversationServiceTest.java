package serenity.doctors_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;
import serenity.doctors_service.dto.ConversationDTO;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.mapper.ConversationMapper;
import serenity.doctors_service.repository.ConversationRepository;
import serenity.doctors_service.repository.MessageRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateConversation() {
        Conversation conversation = new Conversation();
        conversation.setUser1Id(1L);
        conversation.setUser2Id(2L);

        Conversation saved = new Conversation();
        saved.setId(10L);
        saved.setUser1Id(1L);
        saved.setUser2Id(2L);

        ConversationDTO dto = new ConversationDTO();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);
        when(conversationMapper.toDTO(saved)).thenReturn(dto);

        ConversationDTO result = conversationService.createConversation(1L, 2L);

        assertNotNull(result);
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(conversationMapper, times(1)).toDTO(saved);
    }

    @Test
    void shouldReturnUserConversations() {
        Conversation c1 = new Conversation();
        Conversation c2 = new Conversation();

        when(conversationRepository.findByUser1IdOrUser2Id(1L, 1L))
                .thenReturn(List.of(c1, c2));

        when(conversationMapper.toDTO(any())).thenReturn(new ConversationDTO());

        List<ConversationDTO> result = conversationService.getUserConversations(1L);

        assertEquals(2, result.size());
        verify(conversationRepository).findByUser1IdOrUser2Id(1L, 1L);
    }

    @Test
    void shouldGetConversationById() {
        Conversation conversation = new Conversation();
        ConversationDTO dto = new ConversationDTO();

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(conversationMapper.toDTO(conversation)).thenReturn(dto);

        Optional<ConversationDTO> result = conversationService.getConversationById(1L);

        assertTrue(result.isPresent());
        assertEquals(dto, result.get());
    }

    @Test
    void shouldDeleteConversation() {
        conversationService.deleteConversation(1L);
        verify(conversationRepository).deleteById(1L);
    }

    @Test
    void shouldReturnExistingConversation() {
        Conversation existing = new Conversation();

        when(conversationRepository.findByUser1IdAndUser2Id(1L, 2L))
                .thenReturn(Optional.of(existing));

        Conversation result = conversationService.createOrGetConversation(1L, 2L);

        assertEquals(existing, result);
    }

    @Test
    void shouldCallAnalyzeConversationAPI() {
        when(messageRepository.findByConversationId(1L)).thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn("positive");

        String result = conversationService.analyzeConversation(1L);

        assertEquals("positive", result);
        verify(restTemplate).postForObject(anyString(), any(), eq(String.class));
    }
}
