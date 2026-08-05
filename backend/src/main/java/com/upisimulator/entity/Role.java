package com.upisimulator.entity;

/**
 * USER is granted on every registration. ADMIN isn't self-assignable through
 * any endpoint - Phase 12 (Admin dashboard) is the first thing that actually
 * checks for it, and until then it can only be set directly in the database.
 */
public enum Role {
    USER,
    ADMIN
}
