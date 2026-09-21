package com.upisimulator.repository;

import com.upisimulator.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    /**
     * Takes a {@code SELECT ... FOR UPDATE} row lock on the wallet for the
     * rest of the transaction. Use this - never {@link #findByUserId} -
     * anywhere the balance is about to change.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Same lock, looked up by the wallet's own id instead of its owner's.
     * A transfer needs to lock two specific wallets (sender + recipient) in
     * a caller-chosen order - see {@code TransferServiceImpl} for why that
     * order is by ascending wallet id, not by sender/recipient role.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId")
    Optional<Wallet> findByIdForUpdate(@Param("walletId") Long walletId);

}
