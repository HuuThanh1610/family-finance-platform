package com.familyfinance.family.support;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "family";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = slug.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            return "family";
        }
        return slug.length() > 120 ? slug.substring(0, 120) : slug;
    }
}