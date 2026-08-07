package com.upisimulator.controller;

import com.upisimulator.dto.ApiResponse;
import com.upisimulator.dto.ChangePasswordRequest;
import com.upisimulator.dto.DeleteAccountRequest;
import com.upisimulator.dto.ProfileResponse;
import com.upisimulator.dto.UpdateProfileRequest;
import com.upisimulator.entity.User;
import com.upisimulator.service.ProfileService;
import com.upisimulator.util.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Every method here takes {@code @AuthenticationPrincipal User currentUser}
 * and only ever acts on {@code currentUser.getId()} - there's no path
 * parameter like {@code /profile/{id}} because there's nothing to scope:
 * this controller only ever touches the caller's own account.
 */
@RestController
@RequestMapping(ApiPaths.BASE + "/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "View and manage the authenticated user's own profile")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get the current user's profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal User currentUser) {
        ProfileResponse response = profileService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", response));
    }

    @PutMapping
    @Operation(summary = "Update full name and phone number (email cannot be changed here)")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = profileService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password - revokes every other active session")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed. Other sessions have been signed out.", null));
    }

    @PostMapping(value = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a profile picture (JPEG/PNG, max 2MB)")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfilePicture(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {
        ProfileResponse response = profileService.updateProfilePicture(currentUser.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated", response));
    }

    @DeleteMapping
    @Operation(summary = "Delete (soft) the current account - requires password confirmation")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DeleteAccountRequest request) {
        profileService.deleteAccount(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Account deleted", null));
    }

}
