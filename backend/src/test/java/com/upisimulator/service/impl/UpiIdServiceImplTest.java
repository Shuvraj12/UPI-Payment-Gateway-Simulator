package com.upisimulator.service.impl;

import com.upisimulator.dto.CreateUpiIdRequest;
import com.upisimulator.entity.User;
import com.upisimulator.entity.UpiId;
import com.upisimulator.entity.VerificationStatus;
import com.upisimulator.entity.Wallet;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.repository.BankAccountRepository;
import com.upisimulator.repository.UpiIdRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpiIdServiceImplTest {

    @Mock
    private UpiIdRepository upiIdRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;

    private UpiIdServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UpiIdServiceImpl(upiIdRepository, userRepository, walletRepository, bankAccountRepository);
    }

    @Test
    void createThrowsWhenVpaAlreadyTaken() {
        when(upiIdRepository.existsByVpa("priya@upisim")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.createUpiId(1L, new CreateUpiIdRequest("priya")));
        verify(upiIdRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenNoWalletExists() {
        when(upiIdRepository.existsByVpa(any())).thenReturn(false);
        when(upiIdRepository.countByUserId(1L)).thenReturn(0L);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class,
                () -> service.createUpiId(1L, new CreateUpiIdRequest("priya")));
    }

    @Test
    void createThrowsWhenNoVerifiedBankAccount() {
        when(upiIdRepository.existsByVpa(any())).thenReturn(false);
        when(upiIdRepository.countByUserId(1L)).thenReturn(0L);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(new Wallet()));
        when(bankAccountRepository.existsByUserIdAndVerificationStatus(1L, VerificationStatus.VERIFIED))
                .thenReturn(false);

        assertThrows(InvalidRequestException.class,
                () -> service.createUpiId(1L, new CreateUpiIdRequest("priya")));
        verify(upiIdRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenAtTheCap() {
        when(upiIdRepository.existsByVpa(any())).thenReturn(false);
        when(upiIdRepository.countByUserId(1L)).thenReturn(3L);

        assertThrows(InvalidRequestException.class,
                () -> service.createUpiId(1L, new CreateUpiIdRequest("priya")));
        verify(walletRepository, never()).findByUserId(anyLong());
    }

    @Test
    void firstUpiIdBecomesDefaultAndVpaIsLowercased() {
        when(upiIdRepository.existsByVpa("priya123@upisim")).thenReturn(false);
        when(upiIdRepository.countByUserId(1L)).thenReturn(0L);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(new Wallet()));
        when(bankAccountRepository.existsByUserIdAndVerificationStatus(1L, VerificationStatus.VERIFIED))
                .thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(upiIdRepository.save(any(UpiId.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createUpiId(1L, new CreateUpiIdRequest("Priya123"));

        assertEquals("priya123@upisim", response.vpa());
        assertTrue(response.isDefault());
    }

    @Test
    void checkAvailabilityRejectsBadFormatWithoutHittingTheDatabase() {
        var response = service.checkAvailability("1notallowed");

        assertFalse(response.available());
        assertNull(response.vpa());
        verify(upiIdRepository, never()).existsByVpa(any());
    }

    @Test
    void checkAvailabilityReportsTakenVpas() {
        when(upiIdRepository.existsByVpa("rahul@upisim")).thenReturn(true);

        var response = service.checkAvailability("rahul");

        assertFalse(response.available());
        assertEquals("rahul@upisim", response.vpa());
    }

    @Test
    void setDefaultSwapsOffThePreviousDefault() {
        UpiId previousDefault = new UpiId();
        previousDefault.setId(9L);
        previousDefault.setPrimary(true);

        UpiId target = new UpiId();
        target.setId(5L);
        target.setPrimary(false);

        when(upiIdRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(target));
        when(upiIdRepository.findByUserIdAndPrimaryTrue(1L)).thenReturn(Optional.of(previousDefault));
        when(upiIdRepository.save(any(UpiId.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setDefault(1L, 5L);

        assertFalse(previousDefault.isPrimary());
        assertTrue(target.isPrimary());
    }

}
