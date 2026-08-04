package com.upisimulator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UpiSimulatorApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: fails if the application context can't start
        // (bad bean wiring, unresolvable properties, datasource misconfig, ...).
    }

}
