package com.upisimulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upisimulator.dto.AddBankAccountRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.entity.AccountType;
import com.upisimulator.entity.BankName;
import com.upisimulator.service.SimulatedOutcomeSource;
import com.upisimulator.util.ApiPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @MockitoBean} replaces the real {@link SimulatedOutcomeSource} bean
 * for this test class, forced to always succeed - without it, "verify then
 * set primary" would be flaky by design (the real bean fails ~15% of the
 * time on purpose). {@link com.upisimulator.service.impl.BankAccountServiceImplTest}
 * covers the failure branch deterministically at the unit level instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BankAccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SimulatedOutcomeSource outcomeSource;

    @BeforeEach
    void alwaysSucceed() {
        when(outcomeSource.succeeds(anyDouble())).thenReturn(true);
    }

    private String registerAndGetAccessToken(String email, String phone) throws Exception {
        RegisterRequest register = new RegisterRequest("Bank Test", email, "SecurePass123", phone);
        String body = mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    private AddBankAccountRequest sampleAccount(String accountNumber) {
        return new AddBankAccountRequest("Bank Test", BankName.HDFC_BANK, accountNumber, "HDFC0001234", AccountType.SAVINGS);
    }

    @Test
    void addFirstAccountVerifyThenSetPrimary() throws Exception {
        String token = registerAndGetAccessToken("bank1@example.com", "9833300001");

        String addBody = mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAccount("111122223333"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primary").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.maskedAccountNumber").value("XXXXXXXX3333"))
                .andReturn().getResponse().getContentAsString();

        long accountId = objectMapper.readTree(addBody).at("/data/id").asLong();

        mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts/" + accountId + "/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));

        // Already primary (it was the first account) - setting it again is a no-op error, not a new state.
        mockMvc.perform(put(ApiPaths.BASE + "/bank-accounts/" + accountId + "/primary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void settingUnverifiedAccountAsPrimaryIsRejected() throws Exception {
        String token = registerAndGetAccessToken("bank2@example.com", "9833300002");

        String firstBody = mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAccount("444455556666"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        objectMapper.readTree(firstBody).at("/data/id").asLong();

        String secondBody = mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAccount("777788889999"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primary").value(false))
                .andReturn().getResponse().getContentAsString();
        long secondId = objectMapper.readTree(secondBody).at("/data/id").asLong();

        // Second account is still PENDING - never verified - so it can't become primary.
        mockMvc.perform(put(ApiPaths.BASE + "/bank-accounts/" + secondId + "/primary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingPrimaryAccountIsBlockedWhileOthersExist() throws Exception {
        String token = registerAndGetAccessToken("bank3@example.com", "9833300003");

        String firstBody = mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAccount("101010101010"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long firstId = objectMapper.readTree(firstBody).at("/data/id").asLong();

        mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAccount("202020202020"))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(ApiPaths.BASE + "/bank-accounts/" + firstId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addingTheSameAccountTwiceIsRejected() throws Exception {
        String token = registerAndGetAccessToken("bank4@example.com", "9833300004");
        AddBankAccountRequest request = sampleAccount("333344445555");

        mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidIfscCodeIsRejected() throws Exception {
        String token = registerAndGetAccessToken("bank5@example.com", "9833300005");
        AddBankAccountRequest badRequest = new AddBankAccountRequest(
                "Bank Test", BankName.HDFC_BANK, "123456789012", "not-an-ifsc", AccountType.SAVINGS);

        mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

}
