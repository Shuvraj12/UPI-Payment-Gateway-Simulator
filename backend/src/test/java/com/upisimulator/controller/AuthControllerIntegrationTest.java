package com.upisimulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upisimulator.dto.LoginRequest;
import com.upisimulator.dto.RefreshRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.util.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @Transactional} rolls back every test's DB writes, so each method
 * can register its own user without cleaning up or worrying about
 * ordering/pollution from the others sharing one H2 instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenLoginSucceeds() throws Exception {
        RegisterRequest register = new RegisterRequest("Asha Rao", "asha@example.com", "SecurePass123", "9876543210");

        mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("asha@example.com"));

        LoginRequest login = new LoginRequest("asha@example.com", "SecurePass123");

        mockMvc.perform(post(ApiPaths.BASE + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        RegisterRequest register = new RegisterRequest("Vikram Shah", "vikram@example.com", "SecurePass123", "9876500000");
        mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = new LoginRequest("vikram@example.com", "WrongPassword1");

        mockMvc.perform(post(ApiPaths.BASE + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        RegisterRequest register = new RegisterRequest("Neha Kapoor", "neha@example.com", "SecurePass123", "9876511111");

        mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isConflict());
    }

    @Test
    void refreshRotatesTokenAndOldTokenCannotBeReused() throws Exception {
        RegisterRequest register = new RegisterRequest("Farah Khan", "farah@example.com", "SecurePass123", "9876522222");

        String body = mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String originalRefreshToken = objectMapper.readTree(body).at("/data/refreshToken").asText();
        RefreshRequest refreshRequest = new RefreshRequest(originalRefreshToken);

        mockMvc.perform(post(ApiPaths.BASE + "/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").value(org.hamcrest.Matchers.not(originalRefreshToken)));

        // The same (now-rotated) refresh token must not work a second time.
        mockMvc.perform(post(ApiPaths.BASE + "/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequestToProtectedPathReturnsConsistentErrorBody() throws Exception {
        mockMvc.perform(get(ApiPaths.BASE + "/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

}
