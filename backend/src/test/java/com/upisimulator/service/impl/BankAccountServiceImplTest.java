package com.upisimulator.service.impl;

import com.upisimulator.dto.AddBankAccountRequest;
import com.upisimulator.entity.AccountType;
import com.upisimulator.entity.BankAccount;
import com.upisimulator.entity.BankName;
import com.upisimulator.entity.User;
import com.upisimulator.entity.VerificationStatus;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.repository.BankAccountRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.service.SimulatedOutcomeSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SimulatedOutcomeSource outcomeSource;

    private BankAccountServiceImpl service;

    private static final AddBankAccountRequest REQUEST = new AddBankAccountRequest(
            "Test User", BankName.HDFC_BANK, "123456789012", "HDFC0001234", AccountType.SAVINGS);

    @BeforeEach
    void setUp() {
        service = new BankAccountServiceImpl(bankAccountRepository, userRepository, outcomeSource);
    }

    @Test
    void firstAccountForAUserBecomesPrimaryAutomatically() {
        when(bankAccountRepository.existsByUserIdAndBankNameAndAccountNumber(any(), any(), any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(bankAccountRepository.countByUserId(1L)).thenReturn(0L);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.addBankAccount(1L, REQUEST);

        assertTrue(response.primary());
        assertEquals(VerificationStatus.PENDING, response.verificationStatus());
    }

    @Test
    void secondAccountForAUserIsNotPrimaryByDefault() {
        when(bankAccountRepository.existsByUserIdAndBankNameAndAccountNumber(any(), any(), any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(bankAccountRepository.countByUserId(1L)).thenReturn(1L);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.addBankAccount(1L, REQUEST);

        assertFalse(response.primary());
    }

    @Test
    void addingADuplicateAccountThrows() {
        when(bankAccountRepository.existsByUserIdAndBankNameAndAccountNumber(
                1L, BankName.HDFC_BANK, "123456789012")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.addBankAccount(1L, REQUEST));
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    void accountNumberIsMaskedInResponses() {
        when(bankAccountRepository.existsByUserIdAndBankNameAndAccountNumber(any(), any(), any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(bankAccountRepository.countByUserId(1L)).thenReturn(0L);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.addBankAccount(1L, REQUEST);

        assertEquals("XXXXXXXX9012", response.maskedAccountNumber());
    }

    @Test
    void verifySucceedsWhenOutcomeSourceReturnsTrue() {
        BankAccount account = pendingAccount();
        when(bankAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));
        when(outcomeSource.succeeds(anyDouble())).thenReturn(true);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.verifyBankAccount(1L, 5L);

        assertEquals(VerificationStatus.VERIFIED, response.verificationStatus());
        assertNotNull(response.verifiedAt());
    }

    @Test
    void verifyFailsWhenOutcomeSourceReturnsFalse() {
        BankAccount account = pendingAccount();
        when(bankAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));
        when(outcomeSource.succeeds(anyDouble())).thenReturn(false);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.verifyBankAccount(1L, 5L);

        assertEquals(VerificationStatus.FAILED, response.verificationStatus());
        assertNull(response.verifiedAt());
    }

    @Test
    void verifyingAnAlreadyVerifiedAccountThrows() {
        BankAccount account = pendingAccount();
        account.setVerificationStatus(VerificationStatus.VERIFIED);
        when(bankAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));

        assertThrows(InvalidRequestException.class, () -> service.verifyBankAccount(1L, 5L));
    }

    @Test
    void setPrimaryThrowsWhenAccountNotVerified() {
        BankAccount account = pendingAccount();
        when(bankAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));

        assertThrows(InvalidRequestException.class, () -> service.setPrimary(1L, 5L));
    }

    @Test
    void setPrimarySwapsOffThePreviousPrimaryAccount() {
        BankAccount previousPrimary = pendingAccount();
        previousPrimary.setId(9L);
        previousPrimary.setPrimary(true);
        previousPrimary.setVerificationStatus(VerificationStatus.VERIFIED);

        BankAccount target = pendingAccount();
        target.setId(5L);
        target.setVerificationStatus(VerificationStatus.VERIFIED);

        when(bankAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(target));
        when(bankAccountRepository.findByUserIdAndPrimaryTrue(1L)).thenReturn(Optional.of(previousPrimary));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setPrimary(1L, 5L);

        assertFalse(previousPrimary.isPrimary());
        assertTrue(target.isPrimary());
    }

    @Test
    void deletingThePrimaryAccountIsBlockedWhenOthersExist() {
        BankAccount account = pendingAccount();
        account.setPrimary(true);
        when(bankAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.countByUserId(1L)).thenReturn(2L);

        assertThrows(InvalidRequestException.class, () -> service.deleteBankAccount(1L, 5L));
        verify(bankAccountRepository, never()).delete(any());
    }

    @Test
    void deletingTheSolePrimaryAccountIsAllowed() {
        BankAccount account = pendingAccount();
        account.setPrimary(true);
        when(bankAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.countByUserId(1L)).thenReturn(1L);

        service.deleteBankAccount(1L, 5L);

        verify(bankAccountRepository).delete(account);
    }

    private BankAccount pendingAccount() {
        BankAccount account = new BankAccount();
        account.setId(5L);
        account.setAccountHolderName("Test User");
        account.setBankName(BankName.HDFC_BANK);
        account.setAccountNumber("123456789012");
        account.setIfscCode("HDFC0001234");
        account.setAccountType(AccountType.SAVINGS);
        account.setVerificationStatus(VerificationStatus.PENDING);
        return account;
    }

}
