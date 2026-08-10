package com.upisimulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upisimulator.dto.AddBankAccountRequest;
import com.upisimulator.dto.CreateUpiIdRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UpiIdControllerIntegrationTest {

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
        RegisterRequest register = new RegisterRequest("UPI Test", email, "SecurePass123", phone);
        String body = mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    @Test
    void creatingUpiIdWithoutWalletOrBankAccountIsRejected() throws Exception {
        String token = registerAndGetAccessToken("nowallet@example.com", "9844400001");

        mockMvc.perform(post(ApiPaths.BASE + "/upi-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUpiIdRequest("nowallet"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullChainCreatesUpiIdAndBecomesDefault() throws Exception {
        String token = registerAndGetAccessToken("fullchain@example.com", "9844400002");

        mockMvc.perform(post(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // Without a verified bank account, creation should still fail even with a wallet.
        mockMvc.perform(post(ApiPaths.BASE + "/upi-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUpiIdRequest("fullchain"))))
                .andExpect(status().isBadRequest());

        String bankBody = mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddBankAccountRequest(
                                "UPI Test", BankName.HDFC_BANK, "555566667777", "HDFC0001234", AccountType.SAVINGS))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bankAccountId = objectMapper.readTree(bankBody).at("/data/id").asLong();

        mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts/" + bankAccountId + "/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));

        String upiBody = mockMvc.perform(post(ApiPaths.BASE + "/upi-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUpiIdRequest("fullchain"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.vpa").value("fullchain@upisim"))
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andReturn().getResponse().getContentAsString();
        long firstUpiId = objectMapper.readTree(upiBody).at("/data/id").asLong();

        // Second UPI ID for the same user should not be default automatically.
        String secondUpiBody = mockMvc.perform(post(ApiPaths.BASE + "/upi-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUpiIdRequest("fullchain2"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andReturn().getResponse().getContentAsString();
        long secondUpiId = objectMapper.readTree(secondUpiBody).at("/data/id").asLong();

        mockMvc.perform(put(ApiPaths.BASE + "/upi-ids/" + secondUpiId + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));

        // The first one, previously default, is no longer default now.
        mockMvc.perform(get(ApiPaths.BASE + "/upi-ids").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void availabilityCheckReflectsFormatAndUniqueness() throws Exception {
        String token = registerAndGetAccessToken("avail@example.com", "9844400003");

        mockMvc.perform(get(ApiPaths.BASE + "/upi-ids/availability")
                        .param("username", "ab")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));

        mockMvc.perform(get(ApiPaths.BASE + "/upi-ids/availability")
                        .param("username", "brandnewname123")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.vpa").value("brandnewname123@upisim"));
    }

}
