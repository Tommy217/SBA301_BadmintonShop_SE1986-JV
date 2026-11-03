package fu.se.badmintonshop.modules.catalog.service;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugHelper {
    private SlugHelper() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String pattern = "\\p{InCombiningDiacriticalMarks}+";
        return normalized.replaceAll(pattern, "")
                .replace('đ', 'd').replace('Đ', 'd')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
