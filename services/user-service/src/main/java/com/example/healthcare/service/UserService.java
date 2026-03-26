package com.example.healthcare.service;

import com.example.healthcare.dto.*;

import java.util.List;

public interface UserService {

    AuthResponseDTO registerUser(UserRequestDTO request);

    UserResponseDTO updateUserRole(String email, String role);

    AuthResponseDTO login(LoginRequestDTO request);

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(Long id);

    UserResponseDTO getUserByEmail(String email);

    UserResponseDTO updateProfile(String email, ProfileUpdateDTO request);

    UserResponseDTO updateUser(Long id, UserRequestDTO request);

    void deactivateUser(Long id);

    void activateUser(Long id);

    void deleteUser(Long id);

    List<UserDTO> searchUsers(String query);
}
