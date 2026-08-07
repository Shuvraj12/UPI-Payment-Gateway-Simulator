package com.upisimulator.service.impl;

import com.upisimulator.dto.ChangePasswordRequest;
import com.upisimulator.dto.DeleteAccountRequest;
import com.upisimulator.dto.UpdateProfileRequest;
import com.upisimulator.entity.User;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidCredentialsException;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.repository.RefreshTokenRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private FileStorageService fileStorageService;

    private ProfileServiceImpl profileService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(userRepository, refreshTokenRepository, passwordEncoder, fileStorageService);

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setFullName("Original Name");
        existingUser.setEmail("user@example.com");
        existingUser.setPassword("hashed-current-password");
        existingUser.setPhoneNumber("9876543210");
    }

    @Test
    void updateProfileSavesNewNameAndPhone() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "9111111111");
        var response = profileService.updateProfile(1L, request);

        assertEquals("New Name", response.fullName());
        assertEquals("9111111111", response.phoneNumber());
    }

    @Test
    void updateProfileThrowsWhenPhoneBelongsToAnotherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByPhoneNumberAndIdNot("9111111111", 1L)).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "9111111111");

        assertThrows(DuplicateResourceException.class, () -> profileService.updateProfile(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRevokesAllSessionsOnSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correct-current", "hashed-current-password")).thenReturn(true);
        when(passwordEncoder.matches("new-password-123", "hashed-current-password")).thenReturn(false);
        when(passwordEncoder.encode("new-password-123")).thenReturn("hashed-new-password");

        profileService.changePassword(1L, new ChangePasswordRequest("correct-current", "new-password-123"));

        assertEquals("hashed-new-password", existingUser.getPassword());
        verify(refreshTokenRepository).revokeAllActiveByUserId(1L);
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordWrong() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-current", "hashed-current-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> profileService.changePassword(1L, new ChangePasswordRequest("wrong-current", "new-password-123")));
        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(anyLong());
    }

    @Test
    void changePasswordThrowsWhenNewPasswordSameAsCurrent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(eq("correct-current"), any())).thenReturn(true);

        assertThrows(InvalidRequestException.class,
                () -> profileService.changePassword(1L, new ChangePasswordRequest("correct-current", "correct-current")));
    }

    @Test
    void deleteAccountSoftDeletesAndRevokesSessions() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correct-current", "hashed-current-password")).thenReturn(true);

        profileService.deleteAccount(1L, new DeleteAccountRequest("correct-current"));

        assertTrue(existingUser.isDeleted());
        assertFalse(existingUser.isEnabled());
        verify(refreshTokenRepository).revokeAllActiveByUserId(1L);
    }

    @Test
    void deleteAccountThrowsWhenPasswordWrong() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong", "hashed-current-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> profileService.deleteAccount(1L, new DeleteAccountRequest("wrong")));
        verify(userRepository, never()).save(any());
    }

}
