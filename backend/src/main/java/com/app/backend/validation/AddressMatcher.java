package com.app.backend.validation;

import java.text.Normalizer;

/**
 * Detects duplicate/near-duplicate street addresses when a user tries to create
 * a second physical property at a location they already have.
 *
 * <p>Comparison is done on a {@link #normalize normalized} form (diacritics
 * folded, punctuation stripped, whitespace collapsed) so trivial hacks such as
 * "Elizabetes iela" vs "Elīzabetes iela." are treated as the same. An exact
 * normalized match is a hard duplicate; a small edit distance is a near match
 * that the caller can let the user confirm past.
 */
public final class AddressMatcher {

    /** Minimum Levenshtein similarity (0..1) for two addresses to count as a near match. */
    private static final double NEAR_MATCH_THRESHOLD = 0.72;

    private AddressMatcher() {
    }

    public static String normalize(String address) {
        String folded = Normalizer.normalize(address, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return folded.toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    /** True when the two addresses are identical after normalization. */
    public static boolean isDuplicate(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    /**
     * True when the addresses are similar but not identical — a likely typo or a
     * disguised copy (e.g. an added house number). Returns false for exact
     * duplicates so callers can treat the two cases differently.
     */
    public static boolean isNearMatch(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.equals(nb) || na.isEmpty() || nb.isEmpty()) {
            return false;
        }
        int distance = levenshtein(na, nb);
        double similarity = 1.0 - (double) distance / Math.max(na.length(), nb.length());
        return similarity >= NEAR_MATCH_THRESHOLD;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
