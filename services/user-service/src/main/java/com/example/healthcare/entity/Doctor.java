package com.example.healthcare.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    private Long id; // same as User's id

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @OneToOne
    @MapsId // use User's id as Doctor id
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore // prevent infinite JSON loop
    private User user;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    @JsonIgnore // ignore profile in JSON if you don’t want it
    private UserProfile profile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
}