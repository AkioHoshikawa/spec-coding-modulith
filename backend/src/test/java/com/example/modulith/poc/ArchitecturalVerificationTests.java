package com.example.modulith.poc;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Advanced architectural verification tests.
 * These tests verify architectural patterns and best practices.
 */
class ArchitecturalVerificationTests {

    private final ApplicationModules modules = ApplicationModules.of(PocApplication.class);

    @Test
    void shouldVerifyWithDefaultOptions() {
        // Verify with default verification options
        modules.verify();
    }
}
