package com.upisimulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI metadata, available from /swagger-ui.html once the app is
 * running. Wired in from Phase 1 so every controller added in later phases
 * is documented automatically as it's written, rather than bolted on later.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI upiSimulatorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("UPI Payment Gateway Simulator API")
                        .description("Simulated UPI-style payments backend, built phase by phase as a portfolio project.")
                        .version("v1")
                        .license(new License().name("MIT")));
    }

}
