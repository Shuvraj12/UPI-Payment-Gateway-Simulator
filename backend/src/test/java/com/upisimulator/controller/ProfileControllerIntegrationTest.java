package com.upisimulator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upisimulator.dto.ChangePasswordRequest;
import com.upisimulator.dto.DeleteAccountRequest;
import com.upisimulator.dto.RefreshRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.dto.UpdateProfileRequest;
import com.upisimulator.util.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetAccessToken(String email, String phone) throws Exception {
        RegisterRequest register = new RegisterRequest("Test User", email, "SecurePass123", phone);
        String body = mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    @Test
    void getProfileWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ApiPaths.BASE + "/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileWithValidTokenReturnsOwnData() throws Exception {
        String token = registerAndGetAccessToken("priya@example.com", "9812345670");

        mockMvc.perform(get(ApiPaths.BASE + "/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("priya@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"));
    }

    @Test
    void updateProfileChangesNameAndPhone() throws Exception {
        String token = registerAndGetAccessToken("rahul@example.com", "9812345671");
        UpdateProfileRequest update = new UpdateProfileRequest("Rahul Verma", "9812345699");

        mockMvc.perform(put(ApiPaths.BASE + "/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Rahul Verma"))
                .andExpect(jsonPath("$.data.phoneNumber").value("9812345699"));
    }

    @Test
    void changePasswordThenOldRefreshTokenNoLongerWorks() throws Exception {
        RegisterRequest register = new RegisterRequest("Sana Iyer", "sana@example.com", "SecurePass123", "9812345672");
        String registerBody = mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerBody);
        String accessToken = registerJson.at("/data/accessToken").asText();
        String refreshToken = registerJson.at("/data/refreshToken").asText();

        mockMvc.perform(put(ApiPaths.BASE + "/profile/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("SecurePass123", "NewSecurePass456"))))
                .andExpect(status().isOk());

        mockMvc.perform(post(ApiPaths.BASE + "/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadProfilePictureAcceptsJpegAndRejectsWrongType() throws Exception {
        String token = registerAndGetAccessToken("meera@example.com", "9812345673");

        MockMultipartFile validImage =
                new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes());

        mockMvc.perform(multipart(ApiPaths.BASE + "/profile/picture")
                        .file(validImage)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profilePictureUrl", startsWith("/uploads/profile-pictures/")));

        MockMultipartFile wrongType =
                new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart(ApiPaths.BASE + "/profile/picture")
                        .file(wrongType)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccountRequiresCorrectPassword() throws Exception {
        String token = registerAndGetAccessToken("delete-me@example.com", "9812345674");

        mockMvc.perform(delete(ApiPaths.BASE + "/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteAccountRequest("WrongPassword"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete(ApiPaths.BASE + "/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteAccountRequest("SecurePass123"))))
                .andExpect(status().isOk());
    }

}
