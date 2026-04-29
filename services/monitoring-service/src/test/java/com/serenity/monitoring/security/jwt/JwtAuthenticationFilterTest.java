package com.serenity.monitoring.security.jwt;

import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.security.userdetails.CustomUserDetails;
import com.serenity.monitoring.security.userdetails.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_setsAuthenticationWhenBearerTokenValid() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good-token");
        request.setRequestURI("/api/monitoring/mood");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setRole("DOCTOR");
        user.setEmail("doc@test.com");
        user.setIsActive(true);
        CustomUserDetails details = new CustomUserDetails(user);
        when(tokenProvider.validateToken("good-token")).thenReturn(true);
        when(tokenProvider.getEmailFromToken("good-token")).thenReturn("doc@test.com");
        when(userDetailsService.loadUserByUsername("doc@test.com")).thenReturn(details);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_readsTokenFromSseQueryParam() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/monitoring/alerts/stream/8");
        request.setParameter("token", "sse-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        UserAccount user = new UserAccount();
        user.setId(8L);
        user.setRole("DOCTOR");
        user.setEmail("doc@test.com");
        user.setIsActive(true);
        CustomUserDetails details = new CustomUserDetails(user);
        when(tokenProvider.validateToken("sse-token")).thenReturn(true);
        when(tokenProvider.getEmailFromToken("sse-token")).thenReturn("doc@test.com");
        when(userDetailsService.loadUserByUsername("doc@test.com")).thenReturn(details);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_keepsAuthenticationNullWhenTokenInvalid() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");
        request.setRequestURI("/api/monitoring/mood");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(tokenProvider.validateToken("bad")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
