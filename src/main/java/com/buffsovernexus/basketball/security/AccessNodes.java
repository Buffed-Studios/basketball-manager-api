package com.buffsovernexus.basketball.security;

/**
 * Central registry of all permission node strings used in @PreAuthorize checks.
 * Format: {domain}.{action}.{resource}
 */
public final class AccessNodes {

    private AccessNodes() {}

    // ── Patch ──────────────────────────────────────────────────────────────────
    public static final String PATCH_CREATE = "admin.create.patch";

}

