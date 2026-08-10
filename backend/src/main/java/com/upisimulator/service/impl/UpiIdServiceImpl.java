package com.upisimulator.service.impl;

import com.upisimulator.dto.CreateUpiIdRequest;
import com.upisimulator.dto.UpiIdAvailabilityResponse;
import com.upisimulator.dto.UpiIdResponse;
import com.upisimulator.entity.User;
import com.upisimulator.entity.UpiId;
import com.upisimulator.entity.VerificationStatus;
import com.upisimulator.entity.Wallet;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.exception.ResourceNotFoundException;
import com.upisimulator.repository.BankAccountRepository;
import com.upisimulator.repository.UpiIdRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.repository.WalletRepository;
import com.upisimulator.service.UpiIdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpiIdServiceImpl implements UpiIdService {

    private static final String HANDLE = "@upisim";
    private static final int MAX_UPI_IDS_PER_USER = 3;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._-]{2,29}$");

    private final UpiIdRepository upiIdRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    public UpiIdResponse createUpiId(Long userId, CreateUpiIdRequest request) {
        String vpa = buildVpa(request.username());

        if (upiIdRepository.existsByVpa(vpa)) {
            throw new DuplicateResourceException("This UPI ID is already taken");
        }
        if (upiIdRepository.countByUserId(userId) >= MAX_UPI_IDS_PER_USER) {
            throw new InvalidRequestException("You can have at most " + MAX_UPI_IDS_PER_USER + " UPI IDs");
        }

        // Both prerequisites mirror real UPI onboarding: a VPA has to resolve
        // to a funded, bank-verified identity, not just an app account.
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidRequestException("Create a wallet before adding a UPI ID"));
        if (!bankAccountRepository.existsByUserIdAndVerificationStatus(userId, VerificationStatus.VERIFIED)) {
            throw new InvalidRequestException("Link and verify a bank account before creating a UPI ID");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isFirst = upiIdRepository.countByUserId(userId) == 0;

        UpiId upiId = new UpiId();
        upiId.setUser(user);
        upiId.setWallet(wallet);
        upiId.setVpa(vpa);
        upiId.setPrimary(isFirst);

        UpiId saved = upiIdRepository.save(upiId);
        log.info("UPI ID created for user id={}: {}", userId, vpa);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpiIdResponse> getUpiIds(Long userId) {
        return upiIdRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UpiIdAvailabilityResponse checkAvailability(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return new UpiIdAvailabilityResponse(null, false,
                    "Must start with a letter, 3-30 characters (letters, numbers, dots, underscores, hyphens only)");
        }
        String vpa = buildVpa(username);
        boolean taken = upiIdRepository.existsByVpa(vpa);
        return new UpiIdAvailabilityResponse(vpa, !taken, taken ? "This UPI ID is already taken" : null);
    }

    @Override
    public UpiIdResponse setDefault(Long userId, Long upiIdId) {
        UpiId target = findOwnedOrThrow(upiIdId, userId);

        if (target.isPrimary()) {
            throw new InvalidRequestException("This UPI ID is already your default");
        }

        upiIdRepository.findByUserIdAndPrimaryTrue(userId).ifPresent(current -> {
            current.setPrimary(false);
            upiIdRepository.save(current);
        });

        target.setPrimary(true);
        UpiId saved = upiIdRepository.save(target);
        log.info("UPI ID id={} set as default for user id={}", upiIdId, userId);
        return toResponse(saved);
    }

    private UpiId findOwnedOrThrow(Long upiIdId, Long userId) {
        return upiIdRepository.findByIdAndUserId(upiIdId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("UPI ID not found"));
    }

    /** Lowercased so "Priya123@upisim" and "priya123@upisim" are treated as the same VPA, not two different ones. */
    private String buildVpa(String username) {
        return username.toLowerCase() + HANDLE;
    }

    private UpiIdResponse toResponse(UpiId upiId) {
        return new UpiIdResponse(upiId.getId(), upiId.getVpa(), upiId.isPrimary(), upiId.getCreatedAt());
    }

}
