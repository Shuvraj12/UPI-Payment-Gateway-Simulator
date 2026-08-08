package com.upisimulator.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

/**
 * Common id + audit timestamp columns for every entity. Every future entity
 * (Wallet, Transaction, ...) extends this instead of redeclaring id/createdAt/
 * updatedAt, so it's set up once here rather than re-solved per phase.
 * <p>
 * Requires {@code @EnableJpaAuditing}, wired up in
 * {@link com.upisimulator.config.JpaAuditingConfig}.
 * <p>
 * {@code @Setter} here exists for building test fixtures (a Mockito-mocked
 * repository returning a "saved" entity needs an id on it) - normal
 * application code should never call {@code setId}/{@code setCreatedAt}/
 * {@code setUpdatedAt} directly and let the database and JPA auditing own
 * those instead.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
