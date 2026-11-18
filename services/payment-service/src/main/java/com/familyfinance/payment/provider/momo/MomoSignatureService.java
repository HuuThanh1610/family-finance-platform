package com.familyfinance.payment.provider.momo;

import com.familyfinance.payment.dto.MomoIpnRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class MomoSignatureService {

    private final String secretKey;

    public MomoSignatureService(String secretKey) {
        this.secretKey = secretKey == null ? "" : secretKey;
    }

    /** Signature for POST /v2/gateway/api/create */
    public String signCreatePayment(String accessKey, long amount, String extraData, String ipnUrl,
                                    String orderId, String orderInfo, String partnerCode,
                                    String redirectUrl, String requestId, String requestType) {
        String raw = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + nullToEmpty(extraData)
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
        return hmacSha256Hex(raw);
    }

    /** AIOv2 IPN verify signature payload */
    public String signIpn(MomoIpnRequest ipn) {
        String raw = "accessKey=" + ipn.accessKey()
                + "&amount=" + ipn.amount()
                + "&extraData=" + nullToEmpty(ipn.extraData())
                + "&message=" + nullToEmpty(ipn.message())
                + "&orderId=" + ipn.orderId()
                + "&orderInfo=" + nullToEmpty(ipn.orderInfo())
                + "&orderType=" + nullToEmpty(ipn.orderType())
                + "&partnerCode=" + ipn.partnerCode()
                + "&payType=" + nullToEmpty(ipn.payType())
                + "&requestId=" + ipn.requestId()
                + "&responseTime=" + ipn.responseTime()
                + "&resultCode=" + ipn.resultCode()
                + "&transId=" + ipn.transId();
        return hmacSha256Hex(raw);
    }

    public boolean verifyIpn(MomoIpnRequest ipn) {
        if (ipn.signature() == null || ipn.signature().isBlank()) {
            return false;
        }
        String expected = signIpn(ipn);
        return constantTimeEquals(expected, ipn.signature());
    }

    public String hmacSha256Hex(String raw) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}