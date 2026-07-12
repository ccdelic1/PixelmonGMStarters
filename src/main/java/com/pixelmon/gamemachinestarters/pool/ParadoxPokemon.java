package com.pixelmon.gamemachinestarters.pool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Paradox Pokemon (VitalConsiderations.md wanted them out of the pool). Rather than a
 * hardcoded ban, these ship as the <em>default</em> contents of the config blacklist with
 * blacklist mode enabled by default — so they are excluded out of the box, but removing a
 * name from the blacklist (or disabling blacklist mode) lets it back into the pool.
 *
 * <p>This class is just the canonical name list; the actual filtering happens through the
 * normal blacklist path in {@link PoolAssembler}.</p>
 */
public final class ParadoxPokemon {

    /** Readable names seeded into the config blacklist by default. */
    public static final List<String> DEFAULT_BLACKLIST = List.of(
        "Great Tusk",
        "Scream Tail",
        "Brute Bonnet",
        "Flutter Mane",
        "Slither Wing",
        "Sandy Shocks",
        "Roaring Moon",
        "Walking Wake",
        "Gouging Fire",
        "Raging Bolt",
        "Iron Treads",
        "Iron Bundle",
        "Iron Hands",
        "Iron Jugulis",
        "Iron Moth",
        "Iron Thorns",
        "Iron Valiant",
        "Iron Leaves",
        "Iron Boulder",
        "Iron Crown",
        "Koraidon",
        "Miraidon"
    );

    private static final Set<String> NORMALIZED_NAMES = buildNormalized();

    private ParadoxPokemon() {
    }

    private static Set<String> buildNormalized() {
        Set<String> set = new HashSet<>();
        for (String name : DEFAULT_BLACKLIST) {
            set.add(Names.normalize(name));
        }
        return set;
    }

    /** Whether a name is one of the Paradox Pokemon (used by tests and diagnostics). */
    public static boolean isParadox(String name) {
        return NORMALIZED_NAMES.contains(Names.normalize(name));
    }
}
