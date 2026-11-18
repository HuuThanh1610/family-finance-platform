package com.familyfinance.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "momo")
public class MomoProperties {

    private String environment = "";
    private String partnerCode = "";
    private String accessKey = "";
    private String secretKey = "";
    private String baseUrl = "https://test-payment.momo.vn";
    private String redirectUrl = "";
    private String ipnUrl = "";

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getIpnUrl() {
        return ipnUrl;
    }

    public void setIpnUrl(String ipnUrl) {
        this.ipnUrl = ipnUrl;
    }

    public void requireConfigured() {
        if (partnerCode == null || partnerCode.isBlank()
                || accessKey == null || accessKey.isBlank()
                || secretKey == null || secretKey.isBlank()) {
            throw new com.familyfinance.payment.exception.PaymentConfigurationException(
                    "MoMo sandbox is not configured. Set MOMO_PARTNER_CODE, MOMO_ACCESS_KEY, and MOMO_SECRET_KEY.");
        }
        if (redirectUrl == null || redirectUrl.isBlank() || ipnUrl == null || ipnUrl.isBlank()) {
            throw new com.familyfinance.payment.exception.PaymentConfigurationException(
                    "MoMo redirect/IPN URLs missing. Set MOMO_REDIRECT_URL and MOMO_IPN_URL.");
        }
    }
}