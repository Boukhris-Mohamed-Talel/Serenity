package com.example.healthcare.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateDTO {

    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    private String phone;

    private Date dateOfBirth;

    @Size(max = 1000)
    private String bio;

    private String avatar;

    private String preferredLanguage;

    private Boolean isAnonymous;
}
