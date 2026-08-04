package com.upisimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the UPI Payment Gateway Simulator backend.
 * <p>
 * {@link ConfigurationPropertiesScan} lets any {@code @ConfigurationProperties}
 * class/record anywhere under this package (see {@link com.upisimulator.security.JwtProperties})
 * register itself as a bean without needing a matching {@code @Component} or an
 * explicit {@code @EnableConfigurationProperties} entry.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class UpiSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpiSimulatorApplication.class, args);
    }

}
