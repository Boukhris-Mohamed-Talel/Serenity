package com.serenity.monitoring.security.userdetails;

import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_returnsUserDetails() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setEmail("doc@test.com");
        user.setRole("DOCTOR");
        user.setIsActive(true);
        when(userAccountRepository.findByEmail("doc@test.com")).thenReturn(Optional.of(user));

        CustomUserDetails details = (CustomUserDetails) service.loadUserByUsername("doc@test.com");

        assertEquals(1L, details.getId());
        assertEquals("doc@test.com", details.getUsername());
        assertEquals(true, details.isEnabled());
    }

    @Test
    void loadUserByUsername_throwsWhenMissing() {
        when(userAccountRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@test.com"));
    }
}
