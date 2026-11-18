package com.familyfinance.payment.provider.momo;

import com.familyfinance.payment.dto.MomoIpnRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MomoSignatureServiceTest {

    private static final String SECRET = "s3cr3tK3y";

  @Test
  void signCreatePayment_matchesKnownHmac() {
    MomoSignatureService service = new MomoSignatureService(SECRET);
    String signature = service.signCreatePayment(
        "access01",
        150_000L,
        "",
        "https://example.com/ipn",
        "ord-001",
        "TOPUP",
        "MOMO",
        "https://example.com/ok",
        "req-001",
        "captureWallet"
    );
    assertEquals(
        "b7a48dde845e7552268b8d45cc89b875c13ab3e25fbc07d17c51426cb9e66e80",
        signature
    );
  }

    @Test
    void verifyIpn_rejectsBadSignature() {
        MomoSignatureService service = new MomoSignatureService(SECRET);
        MomoIpnRequest ipn = sampleIpn("totally-wrong-signature");
        assertFalse(service.verifyIpn(ipn));
    }

    @Test
    void signQuery_matchesMoMoFieldOrder() {
        MomoSignatureService service = new MomoSignatureService(SECRET);
        String sig1 = service.signQuery("access01", "order-1", "MOMO", "req-q1");
        String sig2 = service.signQuery("access01", "order-1", "MOMO", "req-q1");
        assertEquals(sig1, sig2);
    }

    @Test
    void verifyIpn_acceptsValidSignature() {
        MomoSignatureService service = new MomoSignatureService(SECRET);
        MomoIpnRequest base = sampleIpn(null);
        String signature = service.signIpn(base);
        MomoIpnRequest ipn = new MomoIpnRequest(
                base.partnerCode(), base.accessKey(), base.amount(), base.orderId(),
                base.orderInfo(), base.orderType(), base.transId(), base.resultCode(),
                base.message(), base.payType(), base.responseTime(), base.requestId(),
                base.extraData(), signature
        );
        assertTrue(service.verifyIpn(ipn));
    }

    @Test
    void signCreatePayment_isDeterministic() {
        MomoSignatureService service = new MomoSignatureService(SECRET);
        String a = service.signCreatePayment("k", 1L, "", "i", "o", "info", "p", "r", "req", "captureWallet");
        String b = service.signCreatePayment("k", 1L, "", "i", "o", "info", "p", "r", "req", "captureWallet");
        assertEquals(a, b);
        assertEquals(64, a.length());
    }

    private static MomoIpnRequest sampleIpn(String signature) {
        return new MomoIpnRequest(
                "MOMO",
                "access01",
                150_000L,
                "11000000-0000-4000-8000-000000000099",
                "TOPUP",
                "momo_wallet",
                "999888777",
                0L,
                "Success",
                "qr",
                "1700000000000",
                "req-ipn-1",
                "",
                signature
        );
    }
}