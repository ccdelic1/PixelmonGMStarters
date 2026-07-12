package com.pixelmon.gamemachinestarters.pool;

import java.util.Locale;

/** Species-name normalization shared by every list/lookup in the mod. */
public final class Names {

    private Names() {
    }

    /**
     * Normalizes a species name for matching: lowercase, all non-alphanumerics stripped.
     * "Mr. Mime", "mrmime" and "MR MIME" all normalize to "mrmime";
     * "Jangmo-o" to "jangmoo"; "Nidoranfemale" stays "nidoranfemale".
     */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
