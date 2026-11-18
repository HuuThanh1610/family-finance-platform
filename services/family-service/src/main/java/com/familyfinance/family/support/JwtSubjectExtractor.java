package com.familyfinance.family.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Parses JWT payload without signature verification (placeholder until Keycloak integration).
 */
public final class JwtSubjectExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtSubjectExtractor() {
    }

    public static Optional<UUID> extractUserProfileId(String bearerToken) {
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
            if (sub == null || sub.isNull()) {
                return Optional.empty();
            }
            return tryParseUuid(sub.asText());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<UUID> tryParseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
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