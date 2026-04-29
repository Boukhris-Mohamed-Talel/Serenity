package com.example.healthcare.service.impl;

import com.example.healthcare.dto.AuthResponseDTO;
import com.example.healthcare.dto.LoginRequestDTO;
import com.example.healthcare.dto.ProfileUpdateDTO;
import com.example.healthcare.dto.UserLookupDTO;
import com.example.healthcare.dto.UserRequestDTO;
import com.example.healthcare.dto.UserResponseDTO;
import com.example.healthcare.entity.BanDuration;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
import com.example.healthcare.exception.EmailAlreadyExistsException;
import com.example.healthcare.exception.InvalidCredentialsException;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.exception.UserBannedException;
import com.example.healthcare.mapper.UserMapper;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private UserServiceImpl userService;

    @AfterEach
    void resetReflectionFields() {
        ReflectionTestUtils.setField(userService, "uploadDir", "uploads");
        ReflectionTestUtils.setField(userService, "publicBaseUrl", "http://localhost:8081");
    }

    @Test
    void registerUser_whenEmailAlreadyExists_throws() {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail("a@b.com");
        request.setPassword("pw");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void registerUser_whenRoleBlank_defaultsToPatient() {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail("a@b.com");
        request.setPassword("pw");
        request.setRole("  ");

        User mapped = new User();
        mapped.setEmail("a@b.com");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(passwordEncoder.encode("pw")).thenReturn("ENC");

        Authentication auth = new UsernamePasswordAuthenticationToken("a@b.com", "pw");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("JWT");

        AuthResponseDTO response = userService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.PATIENT);
        assertThat(userCaptor.getValue().getProfile()).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("JWT");
    }

    @Test
    void login_whenBadCredentials_throwsInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("a@b.com");
        request.setPassword("wrong");

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setIsPermanentlyBanned(false);
        user.setBannedUntil(null);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenUserPermanentlyBanned_throwsUserBanned() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("a@b.com");
        request.setPassword("pw");

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setIsPermanentlyBanned(true);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserBannedException.class);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void uploadAvatar_whenFileMissing_throwsBadRequest() {
        assertThatThrownBy(() -> userService.uploadAvatar("a@b.com", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadAvatar_whenNotImage_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> userService.uploadAvatar("a@b.com", file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadAvatar_whenValidImage_savesAndUpdatesProfile(@TempDir Path tmp) throws IOException {
        ReflectionTestUtils.setField(userService, "uploadDir", tmp.toString());
        ReflectionTestUtils.setField(userService, "publicBaseUrl", "http://localhost:8081/");

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        User user = new User();
        user.setId(10L);
        user.setEmail("a@b.com");
        user.setProfile(UserProfile.builder().user(user).build());

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        UserResponseDTO dto = new UserResponseDTO();
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(dto);

        UserResponseDTO result = userService.uploadAvatar("a@b.com", file);

        assertThat(result).isSameAs(dto);
        verify(userRepository).save(user);
        assertThat(user.getProfile().getAvatar()).contains("/uploads/avatars/10/");

        // Ensure file was written somewhere under tmp/avatars/10/
        Path avatarsDir = tmp.resolve("avatars").resolve("10");
        assertThat(Files.exists(avatarsDir)).isTrue();
        assertThat(Files.list(avatarsDir).count()).isEqualTo(1);
    }

    @Test
    void registerUser_whenRoleProvided_setsRole() {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail("doc@b.com");
        request.setPassword("pw");
        request.setRole("DOCTOR");

        User mapped = new User();
        mapped.setEmail("doc@b.com");
        when(userRepository.existsByEmail("doc@b.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(passwordEncoder.encode("pw")).thenReturn("ENC");

        Authentication auth = new UsernamePasswordAuthenticationToken("doc@b.com", "pw");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("JWT");

        userService.registerUser(request);

        ArgumentCaptor<User> c = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(c.capture());
        assertThat(c.getValue().getRole()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void updateUserRole_whenPharmacistSelfAssign_throws() {
        User u = new User();
        u.setEmail("x@b.com");
        when(userRepository.findByEmail("x@b.com")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> userService.updateUserRole("x@b.com", "PHARMACIST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PHARMACIST");
    }

    @Test
    void updateUserRole_whenValid_updatesAndReturnsDto() {
        User u = new User();
        u.setEmail("x@b.com");
        when(userRepository.findByEmail("x@b.com")).thenReturn(Optional.of(u));
        UserResponseDTO dto = new UserResponseDTO();
        when(userMapper.toResponseDTO(u)).thenReturn(dto);

        UserResponseDTO out = userService.updateUserRole("x@b.com", "DOCTOR");

        assertThat(out).isSameAs(dto);
        assertThat(u.getRole()).isEqualTo(Role.DOCTOR);
        verify(userRepository).save(u);
    }

    @Test
    void assignRoleInternally_updatesRole() {
        User u = new User();
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));
        when(userMapper.toResponseDTO(u)).thenReturn(new UserResponseDTO());

        userService.assignRoleInternally(3L, "PATIENT");

        assertThat(u.getRole()).isEqualTo(Role.PATIENT);
        verify(userRepository).save(u);
    }

    @Test
    void assignRoleInternally_invalidRole_throws() {
        User u = new User();
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> userService.assignRoleInternally(3L, "NOT_A_ROLE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    void login_whenSuccess_returnsToken() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("ok@b.com");
        req.setPassword("pw");

        User user = new User();
        user.setId(1L);
        user.setEmail("ok@b.com");
        user.setRole(Role.PATIENT);
        user.setIsActive(true);
        user.setIsPermanentlyBanned(false);
        user.setBannedUntil(null);

        when(userRepository.findByEmail("ok@b.com")).thenReturn(Optional.of(user));
        Authentication auth = new UsernamePasswordAuthenticationToken("ok@b.com", "pw");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("TOK");

        AuthResponseDTO res = userService.login(req);

        assertThat(res.getAccessToken()).isEqualTo("TOK");
        assertThat(res.getUserId()).isEqualTo(1L);
    }

    @Test
    void login_whenDisabled_throwsInvalidCredentials() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("d@b.com");
        req.setPassword("pw");
        User user = new User();
        user.setEmail("d@b.com");
        user.setIsActive(true);
        user.setIsPermanentlyBanned(false);
        when(userRepository.findByEmail("d@b.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("x"));

        assertThatThrownBy(() -> userService.login(req)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenLocked_throwsUserBanned() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("l@b.com");
        req.setPassword("pw");
        User user = new User();
        user.setEmail("l@b.com");
        user.setIsActive(true);
        user.setIsPermanentlyBanned(false);
        when(userRepository.findByEmail("l@b.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("x"));

        assertThatThrownBy(() -> userService.login(req)).isInstanceOf(UserBannedException.class);
    }

    @Test
    void login_whenTempBanActive_throwsUserBanned() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("b@b.com");
        req.setPassword("pw");
        User user = new User();
        user.setEmail("b@b.com");
        user.setIsActive(true);
        user.setIsPermanentlyBanned(false);
        user.setBannedUntil(new Date(System.currentTimeMillis() + 86_400_000L));
        when(userRepository.findByEmail("b@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(req)).isInstanceOf(UserBannedException.class);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_whenTempBanExpired_clearsBanAndAuthenticates() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("e@b.com");
        req.setPassword("pw");
        User user = new User();
        user.setId(9L);
        user.setEmail("e@b.com");
        user.setRole(Role.PATIENT);
        user.setIsActive(true);
        user.setIsPermanentlyBanned(false);
        user.setBannedUntil(new Date(System.currentTimeMillis() - 86_400_000L));
        when(userRepository.findByEmail("e@b.com")).thenReturn(Optional.of(user));
        Authentication auth = new UsernamePasswordAuthenticationToken("e@b.com", "pw");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("JWT");

        userService.login(req);

        verify(userRepository, atLeastOnce()).save(user);
        assertThat(user.getBannedUntil()).isNull();
        assertThat(user.getIsPermanentlyBanned()).isFalse();
    }

    @Test
    void getAllUsers_delegatesToMapper() {
        User u = new User();
        when(userRepository.findAll()).thenReturn(List.of(u));
        when(userMapper.toResponseDTOList(List.of(u))).thenReturn(List.of(new UserResponseDTO()));

        assertThat(userService.getAllUsers()).hasSize(1);
    }

    @Test
    void getUserById_whenMissing_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserByEmail_whenMissing_throws() {
        when(userRepository.findByEmail("n@b.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserByEmail("n@b.com")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_updatesFieldsAndCreatesProfile() {
        User user = new User();
        user.setEmail("p@b.com");
        user.setProfile(null);
        when(userRepository.findByEmail("p@b.com")).thenReturn(Optional.of(user));
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setFirstName("Ann");
        dto.setBio("bio text here");
        when(userMapper.toResponseDTO(user)).thenReturn(new UserResponseDTO());

        userService.updateProfile("p@b.com", dto);

        assertThat(user.getFirstName()).isEqualTo("Ann");
        assertThat(user.getProfile()).isNotNull();
        assertThat(user.getProfile().getBio()).isEqualTo("bio text here");
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_setsPasswordWhenProvided() {
        User user = new User();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        UserRequestDTO req = new UserRequestDTO();
        req.setPassword("newsecret");
        req.setRole("PATIENT");
        when(passwordEncoder.encode("newsecret")).thenReturn("ENC2");
        when(userMapper.toResponseDTO(user)).thenReturn(new UserResponseDTO());

        userService.updateUser(5L, req);

        assertThat(user.getPassword()).isEqualTo("ENC2");
        verify(userMapper).updateEntityFromDTO(req, user);
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_setsInactive() {
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        userService.deactivateUser(1L);
        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void activateUser_setsActive() {
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        userService.activateUser(1L);
        assertThat(user.getIsActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void banUser_whenNullDuration_throws() {
        assertThatThrownBy(() -> userService.banUser(1L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void banUser_permanent_setsFlags() {
        User user = new User();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        userService.banUser(2L, BanDuration.PERMANENT);
        assertThat(user.getIsPermanentlyBanned()).isTrue();
        assertThat(user.getBannedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void unbanUser_clearsBan() {
        User user = new User();
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        userService.unbanUser(3L);
        assertThat(user.getIsPermanentlyBanned()).isFalse();
        assertThat(user.getBannedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_removes() {
        User user = new User();
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        userService.deleteUser(4L);
        verify(userRepository).delete(user);
    }

    @Test
    void searchUsers_mapsToDtos() {
        User u = new User();
        u.setId(1L);
        u.setFirstName("A");
        u.setLastName("B");
        when(userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("ab", "ab"))
                .thenReturn(List.of(u));
        assertThat(userService.searchUsers("ab")).hasSize(1);
    }

    @Test
    void getUsersNamesByIds_returnsDtos() {
        User u = new User();
        u.setId(1L);
        u.setFirstName("A");
        u.setLastName("B");
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(u));
        assertThat(userService.getUsersNamesByIds(List.of(1L))).hasSize(1);
    }

    @Test
    void getDoctors_and_getPatients_delegate() {
        when(userRepository.findByRole(Role.DOCTOR)).thenReturn(List.of());
        when(userMapper.toResponseDTOList(any())).thenReturn(List.of());
        userService.getDoctors();
        verify(userRepository).findByRole(Role.DOCTOR);

        when(userRepository.findByRole(Role.PATIENT)).thenReturn(List.of());
        userService.getPatients();
        verify(userRepository).findByRole(Role.PATIENT);
    }

    @Test
    void lookupDoctors_returnsDtos() {
        User d = new User();
        d.setId(1L);
        d.setFirstName("D");
        d.setLastName("Doc");
        d.setEmail("d@b.com");
        when(userRepository.findByRoleAndIsActiveTrueOrderByLastNameAscFirstNameAsc(Role.DOCTOR)).thenReturn(List.of(d));
        List<UserLookupDTO> list = userService.lookupDoctors();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getEmail()).isEqualTo("d@b.com");
    }

    @Test
    void lookupPatients_filtersByName() {
        User p1 = new User();
        p1.setId(1L);
        p1.setFirstName("Alice");
        p1.setLastName("Smith");
        p1.setEmail("a@s.com");
        User p2 = new User();
        p2.setId(2L);
        p2.setFirstName("Bob");
        p2.setLastName("Jones");
        p2.setEmail("b@s.com");
        when(userRepository.findByRoleAndIsActiveTrueOrderByLastNameAscFirstNameAsc(Role.PATIENT))
                .thenReturn(List.of(p1, p2));

        List<UserLookupDTO> list = userService.lookupPatients("ali", null);
        assertThat(list).extracting(UserLookupDTO::getId).containsExactly(1L);
    }

    @Test
    void lookupUsersByIds_empty_returnsEmpty() {
        assertThat(userService.lookupUsersByIds(List.of())).isEmpty();
    }

    @Test
    void lookupUsersByIds_preservesOrder() {
        User u2 = new User();
        u2.setId(2L);
        u2.setFirstName("B");
        u2.setLastName("B");
        u2.setEmail("b@b.com");
        User u1 = new User();
        u1.setId(1L);
        u1.setFirstName("A");
        u1.setLastName("A");
        u1.setEmail("a@b.com");
        when(userRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(u1, u2));

        List<UserLookupDTO> list = userService.lookupUsersByIds(List.of(2L, 1L));
        assertThat(list).extracting(UserLookupDTO::getId).containsExactly(2L, 1L);
    }
}

