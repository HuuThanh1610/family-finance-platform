package com.familyfinance.payment.provider;

import com.familyfinance.payment.dto.InitiatePaymentRequest;
import com.familyfinance.payment.dto.InitiatePaymentResponse;

/**
 * Abstraction over PSP integrations (e.g. MoMo). No signing logic in scaffold.
 */
public interface PaymentProvider {

    String providerCode();

    /**
     * TODO: Initiate provider payment session — reserved learning topic (MoMo signing, no fake credentials).
     */
    InitiatePaymentResponse initiate(InitiatePaymentRequest request);
}
