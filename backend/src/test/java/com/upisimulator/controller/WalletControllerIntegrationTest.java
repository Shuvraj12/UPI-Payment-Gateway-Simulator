package com.upisimulator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upisimulator.dto.DepositRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.util.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetAccessToken(String email, String phone) throws Exception {
        RegisterRequest register = new RegisterRequest("Wallet Test", email, "SecurePass123", phone);
        String body = mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    @Test
    void createWalletThenDepositUpdatesBalanceAndLedger() throws Exception {
        String token = registerAndGetAccessToken("wallet1@example.com", "9822200001");

        mockMvc.perform(post(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.frozen").value(false));

        mockMvc.perform(post(ApiPaths.BASE + "/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("500.00"), "test top-up"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(500.00));

        mockMvc.perform(get(ApiPaths.BASE + "/wallet/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$.data.content[0].balanceAfter").value(500.00))
                .andExpect(jsonPath("$.data.content[0].referenceNumber").isString())
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    void depositWithoutWalletReturns404() throws Exception {
        String token = registerAndGetAccessToken("wallet2@example.com", "9822200002");

        mockMvc.perform(post(ApiPaths.BASE + "/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("100.00"), null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void frozenWalletRejectsDeposit() throws Exception {
        String token = registerAndGetAccessToken("wallet3@example.com", "9822200003");

        mockMvc.perform(post(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        mockMvc.perform(put(ApiPaths.BASE + "/wallet/freeze").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.frozen").value(true));

        mockMvc.perform(post(ApiPaths.BASE + "/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("100.00"), null))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(ApiPaths.BASE + "/wallet/unfreeze").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.frozen").value(false));
    }

    @Test
    void depositOverSimulatedCapIsRejected() throws Exception {
        String token = registerAndGetAccessToken("wallet4@example.com", "9822200004");
        mockMvc.perform(post(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ApiPaths.BASE + "/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("100000.01"), null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creatingASecondWalletIsRejected() throws Exception {
        String token = registerAndGetAccessToken("wallet5@example.com", "9822200005");

        mockMvc.perform(post(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        mockMvc.perform(post(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

}
