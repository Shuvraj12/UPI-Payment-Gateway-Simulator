package com.upisimulator.service.impl;

import com.upisimulator.dto.AuthResponse;
import com.upisimulator.dto.LoginRequest;
import com.upisimulator.dto.RefreshRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.entity.RefreshToken;
import com.upisimulator.entity.Role;
import com.upisimulator.entity.User;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidCredentialsException;
import com.upisimulator.exception.InvalidTokenException;
import com.upisimulator.repository.RefreshTokenRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.security.JwtProperties;
import com.upisimulator.security.JwtUtil;
import com.upisimulator.service.AuthService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException("An account with this phone number already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(Role.USER);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        log.info("Registered new user id={}", saved.getId());

        return issueTokens(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authResult;
        try {
            authResult = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Incorrect email or password");
        } catch (DisabledException ex) {
            throw new InvalidCredentialsException("This account has been disabled");
        }

        User user = (User) authResult.getPrincipal();
        log.info("Login succeeded for user id={}", user.getId());
        return issueTokens(user);
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        String jti = extractJtiOrThrow(request.refreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not recognized"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token is no longer valid, please log in again");
        }

        // Rotation: this token is single-use. A stolen-but-unused refresh
        // token becomes worthless the moment the real owner refreshes once.
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return issueTokens(storedToken.getUser());
    }

    @Override
    public void logout(RefreshRequest request) {
        String jti = extractJtiOrThrow(request.refreshToken());
        refreshTokenRepository.findByJti(jti).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String extractJtiOrThrow(String refreshToken) {
        try {
            return jwtUtil.extractJti(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Refresh token is malformed or expired");
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateToken(user.getEmail());
        JwtUtil.TokenPair refreshPair = jwtUtil.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setJti(refreshPair.jti());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.refreshExpiration())));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshPair.token(),
                "Bearer",
                jwtProperties.expiration() / 1000,
                new AuthResponse.UserSummary(user.getId(), user.getFullName(), user.getEmail())
        );
    }

}
