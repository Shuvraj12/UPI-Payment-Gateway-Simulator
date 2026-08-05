package com.upisimulator.service;

import com.upisimulator.dto.AuthResponse;
import com.upisimulator.dto.LoginRequest;
import com.upisimulator.dto.RefreshRequest;
import com.upisimulator.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(RefreshRequest request);

}
