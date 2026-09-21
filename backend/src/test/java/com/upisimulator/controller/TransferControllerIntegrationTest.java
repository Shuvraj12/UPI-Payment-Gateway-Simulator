package com.upisimulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upisimulator.dto.AddBankAccountRequest;
import com.upisimulator.dto.CreateUpiIdRequest;
import com.upisimulator.dto.DepositRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.dto.TransferRequest;
import com.upisimulator.entity.AccountType;
import com.upisimulator.entity.BankName;
import com.upisimulator.service.SimulatedOutcomeSource;
import com.upisimulator.util.ApiPaths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransferControllerIntegrationTest {

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

    /** Registers, creates a wallet, links + verifies a bank account, and creates a UPI ID. Returns the access token. */
    private String onboard(String email, String phone, String upiUsername) throws Exception {
        RegisterRequest register = new RegisterRequest("Transfer Test", email, "SecurePass123", phone);
        String body = mockMvc.perform(post(ApiPaths.BASE + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).at("/data/accessToken").asText();

        mockMvc.perform(post(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        String bankBody = mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddBankAccountRequest(
                                "Transfer Test", BankName.HDFC_BANK, phone + "0011", "HDFC0001234", AccountType.SAVINGS))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bankAccountId = objectMapper.readTree(bankBody).at("/data/id").asLong();

        mockMvc.perform(post(ApiPaths.BASE + "/bank-accounts/" + bankAccountId + "/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post(ApiPaths.BASE + "/upi-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUpiIdRequest(upiUsername))))
                .andExpect(status().isCreated());

        return token;
    }

    @Test
    void resolveRecipientReturnsNameForAnExistingVpa() throws Exception {
        onboard("resolvetarget@example.com", "9866600001", "resolvetarget");

        mockMvc.perform(get(ApiPaths.BASE + "/transfers/resolve").param("vpa", "resolvetarget@upisim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientName").value("Transfer Test"));
    }

    @Test
    void resolveRecipientReturns404ForUnknownVpa() throws Exception {
        mockMvc.perform(get(ApiPaths.BASE + "/transfers/resolve").param("vpa", "doesnotexist@upisim"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullTransferMovesMoneyAndAppearsInBothLedgers() throws Exception {
        String senderToken = onboard("sender@example.com", "9866600002", "sendertransfer");
        String receiverToken = onboard("receiver@example.com", "9866600003", "receivertransfer");

        mockMvc.perform(post(ApiPaths.BASE + "/wallet/deposit")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("500.00"), "seed"))))
                .andExpect(status().isOk());

        TransferRequest transfer = new TransferRequest(
                "receivertransfer@upisim", new BigDecimal("120.00"), "lunch", UUID.randomUUID().toString());

        mockMvc.perform(post(ApiPaths.BASE + "/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.senderBalanceAfter").value(380.00))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(get(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(380.00));

        mockMvc.perform(get(ApiPaths.BASE + "/wallet/transactions").header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.data.content[0].counterpartyVpa").value("receivertransfer@upisim"));

        mockMvc.perform(get(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(120.00));

        mockMvc.perform(get(ApiPaths.BASE + "/wallet/transactions").header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$.data.content[0].counterpartyVpa").value("sendertransfer@upisim"));
    }

    @Test
    void sameIdempotencyKeySubmittedTwiceOnlyMovesMoneyOnce() throws Exception {
        String senderToken = onboard("idemsender@example.com", "9866600004", "idemsender");
        onboard("idemreceiver@example.com", "9866600005", "idemreceiver");

        mockMvc.perform(post(ApiPaths.BASE + "/wallet/deposit")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("500.00"), "seed"))))
                .andExpect(status().isOk());

        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequest transfer = new TransferRequest(
                "idemreceiver@upisim", new BigDecimal("100.00"), "test", idempotencyKey);
        String body = objectMapper.writeValueAsString(transfer);

        String firstBody = mockMvc.perform(post(ApiPaths.BASE + "/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstRef = objectMapper.readTree(firstBody).at("/data/referenceNumber").asText();

        String secondBody = mockMvc.perform(post(ApiPaths.BASE + "/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondRef = objectMapper.readTree(secondBody).at("/data/referenceNumber").asText();

        Assertions.assertEquals(firstRef, secondRef);

        mockMvc.perform(get(ApiPaths.BASE + "/wallet").header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(400.00)); // debited once, not twice
    }

    @Test
    void selfTransferIsRejected() throws Exception {
        String token = onboard("selfpay@example.com", "9866600006", "selfpay");

        mockMvc.perform(post(ApiPaths.BASE + "/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(
                                "selfpay@upisim", new BigDecimal("10.00"), null, UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientBalanceReturns400() throws Exception {
        String senderToken = onboard("poorsender@example.com", "9866600007", "poorsender");
        onboard("richreceiver@example.com", "9866600008", "richreceiver");

        mockMvc.perform(post(ApiPaths.BASE + "/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(
                                "richreceiver@upisim", new BigDecimal("50.00"), null, UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest());
    }

}
