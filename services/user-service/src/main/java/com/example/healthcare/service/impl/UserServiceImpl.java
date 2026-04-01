package com.example.healthcare.service.impl;

import com.example.healthcare.dto.*;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
import com.example.healthcare.exception.EmailAlreadyExistsException;
import com.example.healthcare.exception.InvalidCredentialsException;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.mapper.UserMapper;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.jwt.JwtTokenProvider;
import com.example.healthcare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    @Override
    public AuthResponseDTO registerUser(UserRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(Role.valueOf(request.getRole()));
        } else {
            user.setRole(Role.PATIENT);
        }

        UserProfile profile = UserProfile.builder()
                .user(user)
                .preferredLanguage("en")
                .isAnonymous(false)
                .build();
        user.setProfile(profile);

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        return AuthResponseDTO.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public UserResponseDTO updateUserRole(String email, String role) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setRole(Role.valueOf(role.toUpperCase()));
        userRepository.save(user);

        return userMapper.toResponseDTO(user);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(InvalidCredentialsException::new);

            String token = jwtTokenProvider.generateToken(authentication);

            return AuthResponseDTO.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .build();
        } catch (DisabledException e) {
            throw new InvalidCredentialsException("Your account has been deactivated. Please contact an administrator.");
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userMapper.toResponseDTOList(userRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateProfile(String email, ProfileUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().user(user).build();
            user.setProfile(profile);
        }
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getAvatar() != null) profile.setAvatar(request.getAvatar());
        if (request.getPreferredLanguage() != null) profile.setPreferredLanguage(request.getPreferredLanguage());
        if (request.getIsAnonymous() != null) profile.setIsAnonymous(request.getIsAnonymous());

        userRepository.save(user);
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO uploadAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is required");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are allowed");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String original = file.getOriginalFilename();
        String safeName = StringUtils.hasText(original)
                ? original.replace("\\", "_").replace("/", "_").replace("..", "_")
                : "avatar";
        String filename = UUID.randomUUID() + "_" + safeName;

        Path dir = Paths.get(uploadDir, "avatars", String.valueOf(user.getId()));
        Path target = dir.resolve(filename).normalize();
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toAbsolutePath().toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save avatar");
        }

        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String avatarUrl = base + "/uploads/avatars/" + user.getId() + "/" + filename;

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().user(user).build();
            user.setProfile(profile);
        }
        profile.setAvatar(avatarUrl);
        userRepository.save(user);
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        userMapper.updateEntityFromDTO(request, user);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(Role.valueOf(request.getRole()));
        }

        userRepository.save(user);
        return userMapper.toResponseDTO(user);
    }

    @Override
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> searchUsers(String query) {
        return userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query)
                .stream()
                .map(user -> new UserDTO(user.getId(), user.getFirstName(), user.getLastName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersNamesByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .map(user -> new UserDTO(user.getId(), user.getFirstName(), user.getLastName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLookupDTO> lookupDoctors() {
        return userRepository.findByRoleAndIsActiveTrueOrderByLastNameAscFirstNameAsc(Role.DOCTOR).stream()
                .map(this::toLookupDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLookupDTO> lookupPatients(String firstName, String lastName) {
        String fn = firstName != null ? firstName.trim() : "";
        String ln = lastName != null ? lastName.trim() : "";

        if (fn.isEmpty() && ln.isEmpty()) {
            return userRepository.findByRoleAndIsActiveTrueOrderByLastNameAscFirstNameAsc(Role.PATIENT).stream()
                    .map(this::toLookupDto)
                    .toList();
        }

        if (fn.length() < 2 && ln.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter at least 2 characters in first name and/or last name to search.");
        }
        Specification<User> spec = (root, query, cb) -> {
            var p = cb.and(
                    cb.equal(root.get("role"), Role.PATIENT),
                    cb.isTrue(root.get("isActive"))
            );
            if (fn.length() >= 2) {
                p = cb.and(p, cb.like(cb.lower(root.get("firstName")), "%" + fn.toLowerCase() + "%"));
            }
            if (ln.length() >= 2) {
                p = cb.and(p, cb.like(cb.lower(root.get("lastName")), "%" + ln.toLowerCase() + "%"));
            }
            return p;
        };
        return userRepository.findAll(spec).stream()
                .map(this::toLookupDto)
                .sorted(Comparator.comparing(UserLookupDTO::getLastName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(UserLookupDTO::getFirstName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getDoctors() {
        List<User> doctors = userRepository.findByRole(Role.DOCTOR);
        return userMapper.toResponseDTOList(doctors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getPatients() {
        List<User> patients = userRepository.findByRole(Role.PATIENT);
        return userMapper.toResponseDTOList(patients);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLookupDTO> lookupUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most 100 user ids per request.");
        }
        return ids.stream()
                .distinct()
                .map(userRepository::findById)
                .flatMap(Optional::stream)
                .map(this::toLookupDto)
                .toList();
    }

    private UserLookupDTO toLookupDto(User user) {
        return UserLookupDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }
}
