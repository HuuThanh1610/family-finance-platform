package com.familyfinance.profile.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Optional;

/**
 * Parses JWT payload without signature verification (placeholder until Keycloak integration).
 */
public final class JwtSubjectExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtSubjectExtractor() {
    }

    public static Optional<String> extractSubject(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return Optional.empty();
        }
        String[] parts = bearerToken.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            JsonNode root = MAPPER.readTree(payload);
            JsonNode sub = root.get("sub");
            if (sub == null || sub.isNull() || sub.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(sub.asText());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static String padBase64(String segment) {
        int mod = segment.length() % 4;
        if (mod == 0) {
            return segment;
        }
        return segment + "====".substring(mod);
    }
}