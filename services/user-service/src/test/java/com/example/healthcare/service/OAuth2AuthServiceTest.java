package com.example.healthcare.service;

import com.example.healthcare.dto.AuthResponseDTO;
import com.example.healthcare.entity.AuthProvider;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.exception.InvalidCredentialsException;
import com.example.healthcare.exception.UserBannedException;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.jwt.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @Spy @InjectMocks private OAuth2AuthService service;

    @Test
    void loginWithFacebook_whenEmailMissing_throws() {
        Map<String, Object> fb = new HashMap<>();
        fb.put("first_name", "A");
        doReturn(fb).when(service).verifyFacebookToken("token");

        assertThatThrownBy(() -> service.loginWithFacebook("token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must have an email");
    }

    @Test
    void loginWithFacebook_whenExistingUserInactive_throwsInvalidCredentials() {
        Map<String, Object> fb = Map.of("email", "a@b.com", "first_name", "A", "last_name", "B");
        doReturn(fb).when(service).verifyFacebookToken("token");

        User existing = new User();
        existing.setEmail("a@b.com");
        existing.setIsActive(false);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.loginWithFacebook("token"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithFacebook_whenExistingLocalUser_updatesProviderAndReturnsToken() {
        Map<String, Object> fb = Map.of("email", "a@b.com", "first_name", "A", "last_name", "B");
        doReturn(fb).when(service).verifyFacebookToken("token");

        User existing = new User();
        existing.setId(7L);
        existing.setEmail("a@b.com");
        existing.setIsActive(true);
        existing.setIsPermanentlyBanned(false);
        existing.setAuthProvider(AuthProvider.LOCAL);
        existing.setRole(Role.PATIENT);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));
        when(jwtTokenProvider.generateToken(eq("a@b.com"), eq("ROLE_PATIENT"), eq(7L))).thenReturn("JWT");

        AuthResponseDTO resp = service.loginWithFacebook("token");

        assertThat(resp.getAccessToken()).isEqualTo("JWT");
        verify(userRepository).save(existing);
        assertThat(existing.getAuthProvider()).isEqualTo(AuthProvider.FACEBOOK);
    }

    @Test
    void loginWithFacebook_whenPermanentlyBanned_throws() {
        Map<String, Object> fb = Map.of("email", "ban@b.com", "first_name", "A", "last_name", "B");
        doReturn(fb).when(service).verifyFacebookToken("t");

        User existing = new User();
        existing.setEmail("ban@b.com");
        existing.setIsActive(true);
        existing.setIsPermanentlyBanned(true);
        when(userRepository.findByEmail("ban@b.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.loginWithFacebook("t")).isInstanceOf(UserBannedException.class);
    }

    @Test
    void loginWithFacebook_whenNewUser_savesAndReturnsToken() {
        Map<String, Object> fb = Map.of("email", "newfb@b.com", "first_name", "N", "last_name", "U");
        doReturn(fb).when(service).verifyFacebookToken("t2");

        when(userRepository.findByEmail("newfb@b.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(jwtTokenProvider.generateToken(eq("newfb@b.com"), eq("ROLE_PATIENT"), eq(99L))).thenReturn("JWT99");

        AuthResponseDTO resp = service.loginWithFacebook("t2");

        assertThat(resp.getAccessToken()).isEqualTo("JWT99");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginWithGoogle_whenExistingGoogleUser_returnsToken() {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn("g@b.com");
        when(payload.get("given_name")).thenReturn("G");
        when(payload.get("family_name")).thenReturn("User");
        doReturn(payload).when(service).verifyGoogleToken("idtok");

        User existing = new User();
        existing.setId(3L);
        existing.setEmail("g@b.com");
        existing.setIsActive(true);
        existing.setIsPermanentlyBanned(false);
        existing.setAuthProvider(AuthProvider.GOOGLE);
        existing.setRole(Role.PATIENT);
        when(userRepository.findByEmail("g@b.com")).thenReturn(Optional.of(existing));
        when(jwtTokenProvider.generateToken(eq("g@b.com"), eq("ROLE_PATIENT"), eq(3L))).thenReturn("GJWT");

        AuthResponseDTO resp = service.loginWithGoogle("idtok");

        assertThat(resp.getAccessToken()).isEqualTo("GJWT");
    }

    @Test
    void loginWithGoogle_whenNewUser_usesEmailPrefixAsName() {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn("only@b.com");
        when(payload.get("given_name")).thenReturn(null);
        when(payload.get("family_name")).thenReturn(null);
        doReturn(payload).when(service).verifyGoogleToken("id2");

        when(userRepository.findByEmail("only@b.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(12L);
            return u;
        });
        when(jwtTokenProvider.generateToken(eq("only@b.com"), eq("ROLE_PATIENT"), eq(12L))).thenReturn("G2");

        AuthResponseDTO resp = service.loginWithGoogle("id2");

        assertThat(resp.getAccessToken()).isEqualTo("G2");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginWithGoogle_whenTempBanActive_throws() {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn("tb@b.com");
        doReturn(payload).when(service).verifyGoogleToken("x");

        User u = new User();
        u.setEmail("tb@b.com");
        u.setIsActive(true);
        u.setIsPermanentlyBanned(false);
        u.setBannedUntil(new Date(System.currentTimeMillis() + 3600_000L));
        when(userRepository.findByEmail("tb@b.com")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.loginWithGoogle("x")).isInstanceOf(UserBannedException.class);
    }
}

