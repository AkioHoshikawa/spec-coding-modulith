package com.example.modulith.poc;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * 各モジュールの依存性がアーキテクチャの設計に従っていることをテストする
 */
class ModuleDependencyTest {

    private final ApplicationModules modules = ApplicationModules.of(PocApplication.class);

    @Test
    void shouldVerifyWithDefaultOptions() {
        modules.verify();
    }
}
