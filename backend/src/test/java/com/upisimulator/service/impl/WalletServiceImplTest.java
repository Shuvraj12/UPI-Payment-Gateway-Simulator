package com.upisimulator.service.impl;

import com.upisimulator.dto.DepositRequest;
import com.upisimulator.entity.User;
import com.upisimulator.entity.Wallet;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.exception.ResourceNotFoundException;
import com.upisimulator.repository.TransactionRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    private WalletServiceImpl walletService;

    private Wallet existingWallet;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(walletRepository, transactionRepository, userRepository);

        User user = new User();
        user.setId(1L);

        existingWallet = new Wallet();
        existingWallet.setId(10L);
        existingWallet.setUser(user);
        existingWallet.setBalance(new BigDecimal("500.00"));
        existingWallet.setFrozen(false);
    }

    @Test
    void createWalletThrowsWhenOneAlreadyExists() {
        when(walletRepository.existsByUserId(1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> walletService.createWallet(1L));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void createWalletThrowsWhenUserMissing() {
        when(walletRepository.existsByUserId(1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.createWallet(1L));
    }

    @Test
    void depositUsesThePessimisticLockingLookup() {
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.deposit(1L, new DepositRequest(new BigDecimal("250.00"), "test"));

        // The critical assertion: balance mutations must go through the
        // locking lookup, never the plain findByUserId.
        verify(walletRepository, times(1)).findByUserIdForUpdate(1L);
        verify(walletRepository, never()).findByUserId(any());
    }

    @Test
    void depositAddsToBalanceAndRecordsATransaction() {
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = walletService.deposit(1L, new DepositRequest(new BigDecimal("250.50"), "test"));

        assertEquals(0, new BigDecimal("750.50").compareTo(response.balance()));
        verify(transactionRepository).save(any());
    }

    @Test
    void depositThrowsWhenWalletFrozen() {
        existingWallet.setFrozen(true);
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(existingWallet));

        assertThrows(InvalidRequestException.class,
                () -> walletService.deposit(1L, new DepositRequest(new BigDecimal("10.00"), null)));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositThrowsWhenOverTheSimulatedCap() {
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(existingWallet));

        assertThrows(InvalidRequestException.class,
                () -> walletService.deposit(1L, new DepositRequest(new BigDecimal("100000.01"), null)));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void freezeThrowsWhenAlreadyFrozen() {
        existingWallet.setFrozen(true);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(existingWallet));

        assertThrows(InvalidRequestException.class, () -> walletService.freeze(1L));
    }

    @Test
    void unfreezeClearsFrozenAtTimestamp() {
        existingWallet.setFrozen(true);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = walletService.unfreeze(1L);

        assertEquals(false, response.frozen());
    }

}
