package com.serenity.monitoring.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.service.CrisisAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/monitoring/alerts")
@RequiredArgsConstructor
public class CrisisAlertController {

    private final CrisisAlertService crisisAlertService;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/stream/{doctorId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    public SseEmitter subscribe(@PathVariable Long doctorId,
                                @RequestParam(value = "token", required = false) String queryToken,
                                @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthUser authUser = extractCurrentUser(queryToken, authorizationHeader);
        if (!"DOCTOR".equals(authUser.role())) {
            throw new org.springframework.security.access.AccessDeniedException("Only doctors can subscribe to crisis alerts");
        }
        if (!doctorId.equals(authUser.userId())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only subscribe to your own alert stream");
        }
        return crisisAlertService.subscribe(doctorId);
    }

    private AuthUser extractCurrentUser(String queryToken, String authorizationHeader) {
        try {
            String token = resolveToken(queryToken, authorizationHeader);
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new org.springframework.security.access.AccessDeniedException("Invalid token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payloadJson = objectMapper.readTree(payload);

            String email = payloadJson.path("sub").asText(null);
            if (email == null || email.isBlank()) {
                throw new org.springframework.security.access.AccessDeniedException("Token subject is missing");
            }

            String rolesClaim = payloadJson.path("roles").asText("");
            String role = parseRole(rolesClaim);

            UserAccount user = userAccountRepository.findByEmail(email)
                    .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("User not found for token subject"));

            return new AuthUser(user.getId(), role);
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new org.springframework.security.access.AccessDeniedException("Unable to resolve user from token");
        }
    }

    private String resolveToken(String queryToken, String authorizationHeader) {
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }

        throw new org.springframework.security.access.AccessDeniedException("Missing authentication token");
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

