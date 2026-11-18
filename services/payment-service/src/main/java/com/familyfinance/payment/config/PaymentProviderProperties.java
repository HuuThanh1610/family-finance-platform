package com.familyfinance.payment.config;

import com.familyfinance.payment.provider.FakePaymentAdapter;
import com.familyfinance.payment.provider.MoMoSandboxAdapter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "familyfinance.payment.provider")
public class PaymentProviderProperties {

  /**
   * Active default provider: {@code fake} → {@link FakePaymentAdapter#PROVIDER_CODE},
   * {@code momo} → {@link MoMoSandboxAdapter#PROVIDER_CODE}.
   */
  private String active = "fake";

  public String getActive() {
    return active;
  }

  public void setActive(String active) {
    this.active = active;
  }

  public String activeProviderCode() {
    if (active == null || active.isBlank()) {
      return FakePaymentAdapter.PROVIDER_CODE;
    }
    return switch (active.trim().toLowerCase()) {
      case "momo", "momo_sandbox", "momo-sandbox" -> MoMoSandboxAdapter.PROVIDER_CODE;
      case "fake", "fake_payment", "fake-payment" -> FakePaymentAdapter.PROVIDER_CODE;
      default -> active.trim();
    };
  }
}