package com.familyfinance.wallet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI familyFinanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet Service")
                        .description("Family Finance platform — read-focused scaffold")
                        .version("0.0.1"));
    }
}
