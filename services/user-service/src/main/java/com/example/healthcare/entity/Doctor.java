package com.example.healthcare.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Doctor extends User {

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;
}