package com.app.backend.messaging;

import java.util.ArrayList;
import java.util.List;

/**
 * Naming for the resized derivatives of an uploaded photo. A variant URL/key is the original with a
 * suffix inserted before the extension ({@code …/foo.jpg} → {@code …/foo_card.jpg}) — a transform the
 * frontend reproduces to request the right size, so no variant URLs need to be stored.
 */
public final class MediaVariants {

    public record Variant(String suffix, int maxWidth) {}

    public static final Variant THUMB = new Variant("thumb", 400);
    public static final Variant CARD = new Variant("card", 800);
    public static final List<Variant> ALL = List.of(THUMB, CARD);

    private MediaVariants() {}

    public static String derive(String urlOrKey, String suffix) {
        int slash = urlOrKey.lastIndexOf('/');
        int dot = urlOrKey.lastIndexOf('.');
        return dot > slash
                ? urlOrKey.substring(0, dot) + "_" + suffix + urlOrKey.substring(dot)
                : urlOrKey + "_" + suffix;
    }

    /** Each URL plus its variant URLs, so a deleted photo takes its derivatives with it. */
    public static List<String> withVariants(List<String> urls) {
        List<String> out = new ArrayList<>(urls.size() * (ALL.size() + 1));
        for (String url : urls) {
            out.add(url);
            for (Variant v : ALL) {
                out.add(derive(url, v.suffix()));
            }
        }
        return out;
    }
}
