package com.upisimulator.service;

import com.upisimulator.dto.AddBankAccountRequest;
import com.upisimulator.dto.BankAccountResponse;

import java.util.List;

public interface BankAccountService {

    BankAccountResponse addBankAccount(Long userId, AddBankAccountRequest request);

    List<BankAccountResponse> getBankAccounts(Long userId);

    BankAccountResponse verifyBankAccount(Long userId, Long accountId);

    BankAccountResponse setPrimary(Long userId, Long accountId);

    void deleteBankAccount(Long userId, Long accountId);

}
