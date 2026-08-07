package com.upisimulator.service;

import com.upisimulator.dto.ChangePasswordRequest;
import com.upisimulator.dto.DeleteAccountRequest;
import com.upisimulator.dto.ProfileResponse;
import com.upisimulator.dto.UpdateProfileRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * All methods take the authenticated user's id, extracted by the controller
 * from {@code @AuthenticationPrincipal}, rather than the {@code User} object
 * itself - that principal was loaded outside any transaction by
 * {@code JwtAuthenticationFilter} and is effectively detached, so each
 * method here re-fetches a properly managed entity instead of mutating a
 * stale one. Since the id always comes from the verified token (never a
 * client-supplied parameter), a plain {@code findById} is safe here - there's
 * no other user's id a caller could substitute in.
 */
public interface ProfileService {

    ProfileResponse getProfile(Long userId);

    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    ProfileResponse updateProfilePicture(Long userId, MultipartFile file);

    void deleteAccount(Long userId, DeleteAccountRequest request);

}
