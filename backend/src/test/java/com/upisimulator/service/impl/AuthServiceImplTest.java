package com.upisimulator.service.impl;

import com.upisimulator.dto.LoginRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.entity.Role;
import com.upisimulator.entity.User;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidCredentialsException;
import com.upisimulator.repository.RefreshTokenRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.security.JwtProperties;
import com.upisimulator.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("unit-test-secret-value-not-real", 900000, 604800000);
        authService = new AuthServiceImpl(
                userRepository, refreshTokenRepository, passwordEncoder, authenticationManager, jwtUtil, jwtProperties
        );
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123", "9876543210");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerThrowsWhenPhoneAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123", "9876543210");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerSavesUserWithHashedPasswordAndDefaultRole() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123", "9876543210");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn(new JwtUtil.TokenPair("refresh-token", "jti-123"));

        var response = authService.register(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("test@example.com", response.user().email());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hashed-password", userCaptor.getValue().getPassword());
        assertEquals(Role.USER, userCaptor.getValue().getRole());
    }

    @Test
    void loginThrowsInvalidCredentialsOnBadPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(refreshTokenRepository, never()).save(any());
    }

}
