package com.serenity.monitoring.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serenity.monitoring.dto.EmotionalTriggerRequest;
import com.serenity.monitoring.dto.EmotionalTriggerResponse;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.exception.ResourceNotFoundException;
import com.serenity.monitoring.mapper.EmotionalTriggerMapper;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.service.EmotionalTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class EmotionalTriggerServiceImpl implements EmotionalTriggerService {

    private final EmotionalTriggerRepository emotionalTriggerRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmotionalTriggerMapper emotionalTriggerMapper;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public EmotionalTriggerResponse createTrigger(Long pathMoodEntryId, EmotionalTriggerRequest request) {
        AuthUser authUser = extractCurrentUser();
        ensureRole(authUser, "DOCTOR");

        if (!pathMoodEntryId.equals(request.getMoodEntryId())) {
            throw new IllegalStateException("Path moodEntryId and body moodEntryId must match");
        }

        MoodEntry moodEntry = moodEntryRepository.findById(pathMoodEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("MoodEntry not found"));

        if (!moodEntry.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You are not assigned to this patient");
        }

        EmotionalTrigger trigger = emotionalTriggerMapper.toEntity(request);
        trigger.setMoodEntry(moodEntry);
        trigger.setDoctorId(authUser.userId());
        trigger.setRecordedAt(LocalDateTime.now());

        EmotionalTrigger saved = emotionalTriggerRepository.save(trigger);
        return emotionalTriggerMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<EmotionalTriggerResponse> getTriggersByMoodEntryId(Long moodEntryId) {
        AuthUser authUser = extractCurrentUser();
        MoodEntry moodEntry = moodEntryRepository.findById(moodEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("MoodEntry not found"));

        if ("PATIENT".equals(authUser.role()) && !moodEntry.getPatientId().equals(authUser.userId())) {
            throw new AccessDeniedException("You do not own this mood entry");
        }
        if ("DOCTOR".equals(authUser.role()) && !moodEntry.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You are not assigned to this patient");
        }
        if (!"PATIENT".equals(authUser.role()) && !"DOCTOR".equals(authUser.role())) {
            throw new AccessDeniedException("Only doctor or patient can view triggers");
        }

        return emotionalTriggerRepository.findByMoodEntryId(moodEntryId).stream()
                .map(emotionalTriggerMapper::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public EmotionalTriggerResponse getTriggerById(Long id) {
        AuthUser authUser = extractCurrentUser();
        EmotionalTrigger trigger = emotionalTriggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmotionalTrigger not found"));

        MoodEntry moodEntry = trigger.getMoodEntry();
        if ("PATIENT".equals(authUser.role()) && !moodEntry.getPatientId().equals(authUser.userId())) {
            throw new AccessDeniedException("You do not own this mood entry");
        }
        if ("DOCTOR".equals(authUser.role()) && !moodEntry.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You are not assigned to this patient");
        }
        if (!"PATIENT".equals(authUser.role()) && !"DOCTOR".equals(authUser.role())) {
            throw new AccessDeniedException("Only doctor or patient can view this trigger");
        }

        return emotionalTriggerMapper.toResponse(trigger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EmotionalTriggerResponse updateTrigger(Long id, EmotionalTriggerRequest request) {
        AuthUser authUser = extractCurrentUser();
        ensureRole(authUser, "DOCTOR");

        EmotionalTrigger trigger = emotionalTriggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmotionalTrigger not found"));

        if (!trigger.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You cannot update another doctor's trigger");
        }

        // Keep immutable ownership fields and parent relation.
        trigger.setTriggerType(request.getTriggerType());
        trigger.setDescription(request.getDescription());
        trigger.setIntensity(request.getIntensity());

        EmotionalTrigger updated = emotionalTriggerRepository.save(trigger);
        return emotionalTriggerMapper.toResponse(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTrigger(Long id) {
        AuthUser authUser = extractCurrentUser();
        ensureRole(authUser, "DOCTOR");

        EmotionalTrigger trigger = emotionalTriggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmotionalTrigger not found"));

        if (!trigger.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You cannot delete another doctor's trigger");
        }

        emotionalTriggerRepository.delete(trigger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<EmotionalTriggerResponse> getTriggersByDoctorId(Long doctorId) {
        AuthUser authUser = extractCurrentUser();
        ensureRole(authUser, "DOCTOR");

        if (!doctorId.equals(authUser.userId())) {
            throw new AccessDeniedException("You can only read your own triggers");
        }

        return emotionalTriggerRepository.findByDoctorId(doctorId).stream()
                .map(emotionalTriggerMapper::toResponse)
                .toList();
    }

    private void ensureRole(AuthUser user, String role) {
        if (!role.equals(user.role())) {
            throw new AccessDeniedException("Access denied for role: " + user.role());
        }
    }

    private AuthUser extractCurrentUser() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String authHeader = attrs.getRequest().getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AccessDeniedException("Missing Bearer token");
            }

            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new AccessDeniedException("Invalid token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payloadJson = objectMapper.readTree(payload);

            String email = payloadJson.path("sub").asText(null);
            if (email == null || email.isBlank()) {
                throw new AccessDeniedException("Token subject is missing");
            }

            String roles = payloadJson.path("roles").asText("");
            String role = parseRole(roles);

            UserAccount user = userAccountRepository.findByEmail(email)
                    .orElseThrow(() -> new AccessDeniedException("User not found for token subject"));

            return new AuthUser(user.getId(), role);
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AccessDeniedException("Unable to resolve user from token");
        }
    }

    private String parseRole(String rolesClaim) {
        if (rolesClaim == null || rolesClaim.isBlank()) {
            return "UNKNOWN";
        }
        for (String value : rolesClaim.split(",")) {
            String role = value.trim();
            if (role.startsWith("ROLE_")) {
                return role.substring("ROLE_".length());
            }
        }
        String first = rolesClaim.split(",")[0].trim();
        return first.startsWith("ROLE_") ? first.substring("ROLE_".length()) : first;
    }

    private record AuthUser(Long userId, String role) {
    }
}

