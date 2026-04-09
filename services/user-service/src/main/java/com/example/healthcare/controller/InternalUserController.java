package com.example.healthcare.controller;

import com.example.healthcare.dto.LookupIdsRequest;
import com.example.healthcare.dto.UserEmailResponse;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Service-to-service API (protected by {@link com.example.healthcare.security.InternalApiKeyFilter}).
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @PostMapping("/emails-by-ids")
    public ResponseEntity<List<UserEmailResponse>> emailsByIds(@Valid @RequestBody LookupIdsRequest request) {
        List<UserEmailResponse> out = new ArrayList<>();
        for (Long id : request.getIds()) {
            userRepository.findById(id).ifPresent(u -> out.add(toDto(u)));
        }
        return ResponseEntity.ok(out);
    }

    private static UserEmailResponse toDto(User u) {
        return UserEmailResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .build();
    }
}
