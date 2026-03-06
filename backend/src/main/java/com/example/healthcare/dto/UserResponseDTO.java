package com.example.healthcare.dto;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Date dateOfBirth;
    private Boolean isActive;
    private Date createdAt;
    private String role;
    private UserProfileDTO profile;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserProfileDTO {
        private Long id;
        private String avatar;
        private String bio;
        private Boolean isAnonymous;
        private String preferredLanguage;
    }
}
