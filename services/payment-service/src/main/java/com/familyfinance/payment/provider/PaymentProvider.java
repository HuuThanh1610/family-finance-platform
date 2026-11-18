package com.familyfinance.payment.provider;

import com.familyfinance.payment.entity.PaymentAttempt;
import com.familyfinance.payment.entity.PaymentOrder;

public interface PaymentProvider {

    String providerCode();

    ProviderInitiateResult initiate(PaymentOrder order, PaymentAttempt attempt);
}