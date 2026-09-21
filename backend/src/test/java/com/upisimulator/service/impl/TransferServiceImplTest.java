package com.upisimulator.service.impl;

import com.upisimulator.dto.TransferRequest;
import com.upisimulator.entity.Transaction;
import com.upisimulator.entity.User;
import com.upisimulator.entity.UpiId;
import com.upisimulator.entity.Wallet;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.exception.ResourceNotFoundException;
import com.upisimulator.repository.TransactionRepository;
import com.upisimulator.repository.UpiIdRepository;
import com.upisimulator.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UpiIdRepository upiIdRepository;

    private TransferServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransferServiceImpl(walletRepository, transactionRepository, upiIdRepository);
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
    }

    private Wallet wallet(Long id, BigDecimal balance, boolean frozen) {
        Wallet wallet = new Wallet();
        wallet.setId(id);
        wallet.setBalance(balance);
        wallet.setFrozen(frozen);
        return wallet;
    }

    private UpiId upiId(String vpa, Wallet wallet) {
        UpiId upiId = new UpiId();
        upiId.setVpa(vpa);
        upiId.setWallet(wallet);
        User user = new User();
        user.setFullName("Recipient Name");
        upiId.setUser(user);
        return upiId;
    }

    private TransferRequest request(String vpa, String amount) {
        return new TransferRequest(vpa, new BigDecimal(amount), "note", UUID.randomUUID().toString());
    }

    @Test
    void idempotentReplayReturnsExistingResultWithoutTouchingWallets() {
        Transaction existing = new Transaction();
        existing.setReferenceNumber("123456789012");
        existing.setCounterpartyVpa("priya@upisim");
        existing.setAmount(new BigDecimal("50.00"));
        existing.setBalanceAfter(new BigDecimal("450.00"));
        when(transactionRepository.findByIdempotencyKey("dupe-key")).thenReturn(Optional.of(existing));

        var response = service.transfer(1L,
                new TransferRequest("priya@upisim", new BigDecimal("50.00"), "note", "dupe-key"));

        assertEquals("123456789012", response.referenceNumber());
        verify(walletRepository, never()).findByIdForUpdate(anyLong());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void selfTransferIsRejected() {
        Wallet sameWallet = wallet(10L, new BigDecimal("500.00"), false);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sameWallet));
        when(upiIdRepository.findByVpa("priya@upisim")).thenReturn(Optional.of(upiId("priya@upisim", sameWallet)));

        assertThrows(InvalidRequestException.class,
                () -> service.transfer(1L, request("priya@upisim", "50.00")));
        verify(walletRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void recipientVpaNotFoundThrows404Style() {
        when(upiIdRepository.findByVpa("nobody@upisim")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.transfer(1L, request("nobody@upisim", "50.00")));
    }

    @Test
    void locksWalletsInAscendingIdOrderWhenSenderIdIsHigher() {
        Wallet senderWallet = wallet(20L, new BigDecimal("500.00"), false);
        Wallet recipientWallet = wallet(5L, BigDecimal.ZERO, false);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(upiIdRepository.findByVpa("priya@upisim")).thenReturn(Optional.of(upiId("priya@upisim", recipientWallet)));
        when(upiIdRepository.findByUserIdAndPrimaryTrue(1L))
                .thenReturn(Optional.of(upiId("me@upisim", senderWallet)));
        when(walletRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.sumAmountSince(any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.transfer(1L, request("priya@upisim", "50.00"));

        InOrder order = inOrder(walletRepository);
        order.verify(walletRepository).findByIdForUpdate(5L);
        order.verify(walletRepository).findByIdForUpdate(20L);
    }

    @Test
    void locksWalletsInAscendingIdOrderWhenRecipientIdIsHigher() {
        Wallet senderWallet = wallet(5L, new BigDecimal("500.00"), false);
        Wallet recipientWallet = wallet(20L, BigDecimal.ZERO, false);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(upiIdRepository.findByVpa("priya@upisim")).thenReturn(Optional.of(upiId("priya@upisim", recipientWallet)));
        when(upiIdRepository.findByUserIdAndPrimaryTrue(1L))
                .thenReturn(Optional.of(upiId("me@upisim", senderWallet)));
        when(walletRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(recipientWallet));
        when(transactionRepository.sumAmountSince(any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.transfer(1L, request("priya@upisim", "50.00"));

        InOrder order = inOrder(walletRepository);
        order.verify(walletRepository).findByIdForUpdate(5L);
        order.verify(walletRepository).findByIdForUpdate(20L);
    }

    @Test
    void insufficientBalanceIsRejected() {
        Wallet senderWallet = wallet(5L, new BigDecimal("10.00"), false);
        Wallet recipientWallet = wallet(20L, BigDecimal.ZERO, false);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(upiIdRepository.findByVpa("priya@upisim")).thenReturn(Optional.of(upiId("priya@upisim", recipientWallet)));
        when(upiIdRepository.findByUserIdAndPrimaryTrue(1L))
                .thenReturn(Optional.of(upiId("me@upisim", senderWallet)));
        when(walletRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(recipientWallet));

        assertThrows(InvalidRequestException.class,
                () -> service.transfer(1L, request("priya@upisim", "50.00")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void frozenRecipientWalletBlocksTransfer() {
        Wallet senderWallet = wallet(5L, new BigDecimal("500.00"), false);
        Wallet recipientWallet = wallet(20L, BigDecimal.ZERO, true);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(upiIdRepository.findByVpa("priya@upisim")).thenReturn(Optional.of(upiId("priya@upisim", recipientWallet)));
        when(upiIdRepository.findByUserIdAndPrimaryTrue(1L))
                .thenReturn(Optional.of(upiId("me@upisim", senderWallet)));
        when(walletRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(recipientWallet));

        assertThrows(InvalidRequestException.class,
                () -> service.transfer(1L, request("priya@upisim", "50.00")));
    }

    @Test
    void successfulTransferUpdatesBothBalancesAndCreatesTwoLinkedEntries() {
        Wallet senderWallet = wallet(5L, new BigDecimal("500.00"), false);
        Wallet recipientWallet = wallet(20L, new BigDecimal("100.00"), false);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(upiIdRepository.findByVpa("priya@upisim")).thenReturn(Optional.of(upiId("priya@upisim", recipientWallet)));
        when(upiIdRepository.findByUserIdAndPrimaryTrue(1L))
                .thenReturn(Optional.of(upiId("me@upisim", senderWallet)));
        when(walletRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(recipientWallet));
        when(transactionRepository.sumAmountSince(any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.transfer(1L, request("priya@upisim", "50.00"));

        assertEquals(0, new BigDecimal("450.00").compareTo(senderWallet.getBalance()));
        assertEquals(0, new BigDecimal("150.00").compareTo(recipientWallet.getBalance()));
        assertEquals(0, new BigDecimal("450.00").compareTo(response.senderBalanceAfter()));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

}
