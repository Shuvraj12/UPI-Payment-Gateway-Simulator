package com.upisimulator.service;

import com.upisimulator.dto.DepositRequest;
import com.upisimulator.dto.TransactionResponse;
import com.upisimulator.dto.WalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {

    WalletResponse createWallet(Long userId);

    WalletResponse getWallet(Long userId);

    WalletResponse deposit(Long userId, DepositRequest request);

    WalletResponse freeze(Long userId);

    WalletResponse unfreeze(Long userId);

    Page<TransactionResponse> getTransactions(Long userId, Pageable pageable);

}
