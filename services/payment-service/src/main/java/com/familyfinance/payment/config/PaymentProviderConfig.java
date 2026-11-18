package com.familyfinance.payment.config;

import com.familyfinance.payment.provider.PaymentProvider;
import com.familyfinance.payment.provider.momo.MomoSignatureService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(MomoProperties.class)
public class PaymentProviderConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    MomoSignatureService momoSignatureService(MomoProperties properties) {
        return new MomoSignatureService(properties.getSecretKey());
    }

    @Bean
    Map<String, PaymentProvider> paymentProviders(List<PaymentProvider> providers) {
        return providers.stream().collect(Collectors.toMap(PaymentProvider::providerCode, Function.identity()));
    }
}