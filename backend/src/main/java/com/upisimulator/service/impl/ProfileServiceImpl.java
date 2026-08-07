package com.upisimulator.service.impl;

import com.upisimulator.dto.ChangePasswordRequest;
import com.upisimulator.dto.DeleteAccountRequest;
import com.upisimulator.dto.ProfileResponse;
import com.upisimulator.dto.UpdateProfileRequest;
import com.upisimulator.entity.User;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidCredentialsException;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.exception.ResourceNotFoundException;
import com.upisimulator.repository.RefreshTokenRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.service.FileStorageService;
import com.upisimulator.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return toResponse(findUserOrThrow(userId));
    }

    @Override
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        boolean phoneChanged = !request.phoneNumber().equals(user.getPhoneNumber());
        if (phoneChanged && userRepository.existsByPhoneNumberAndIdNot(request.phoneNumber(), userId)) {
            throw new DuplicateResourceException("An account with this phone number already exists");
        }

        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        User saved = userRepository.save(user);
        log.info("Profile updated for user id={}", userId);
        return toResponse(saved);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Changing your password should sign out every other session - otherwise
        // an attacker who already has a refresh token keeps their access even
        // after the real owner "secures" the account.
        refreshTokenRepository.revokeAllActiveByUserId(userId);
        log.info("Password changed for user id={}; all sessions revoked", userId);
    }

    @Override
    public ProfileResponse updateProfilePicture(Long userId, MultipartFile file) {
        User user = findUserOrThrow(userId);

        String previousUrl = user.getProfilePictureUrl();
        String newUrl = fileStorageService.store(file);

        user.setProfilePictureUrl(newUrl);
        User saved = userRepository.save(user);

        if (previousUrl != null) {
            fileStorageService.delete(previousUrl);
        }

        log.info("Profile picture updated for user id={}", userId);
        return toResponse(saved);
    }

    @Override
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Password is incorrect");
        }

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenRepository.revokeAllActiveByUserId(userId);
        log.info("Account soft-deleted for user id={}", userId);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getProfilePictureUrl(),
                user.getCreatedAt()
        );
    }

}
