package com.pixelmon.gamemachinestarters.pool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hardcoded, invisible blacklist of Pokemon that exist in Pixelmon's registry but are not
 * actually implemented yet — they have a placeholder ("none") model and render as a
 * "fallback warning" instead of a real Pokemon. These must never appear as a starter.
 *
 * <p>Unlike the config blacklist, this list is not user-visible or configurable: an
 * unimplemented Pokemon is broken, not a balance choice. The list was derived by scanning
 * the Pixelmon 9.3.16 species data for a base-form model of {@code "none"}; this mod is
 * version-locked to 9.3.16, so the list is exact for that build.</p>
 */
public final class UnimplementedPokemon {

    private static final List<String> NAMES = List.of(
        "Poltchageist",
        "Sinistcha",
        "Okidogi",
        "Munkidori",
        "Fezandipiti",
        "Ogerpon",
        "Archaludon",
        "Terapagos",
        "Pecharunt"
    );

    private static final Set<String> NORMALIZED_NAMES = buildNormalized();

    private UnimplementedPokemon() {
    }

    private static Set<String> buildNormalized() {
        Set<String> set = new HashSet<>();
        for (String name : NAMES) {
            set.add(Names.normalize(name));
        }
        return set;
    }

    public static boolean isUnimplemented(String name) {
        return NORMALIZED_NAMES.contains(Names.normalize(name));
    }
}
