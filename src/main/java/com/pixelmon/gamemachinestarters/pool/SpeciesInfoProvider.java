package com.pixelmon.gamemachinestarters.pool;

import java.util.List;

/**
 * Abstraction over Pixelmon's species registry so pool assembly is testable without a
 * running game. The runtime implementation is {@code PixelmonSpeciesProvider}; tests
 * use a fake.
 */
public interface SpeciesInfoProvider {

    /** Whether a species with this (user-typed or list) name exists. */
    boolean exists(String name);

    /** Every registered species name (excluding MissingNo and other placeholders). */
    List<String> allSpeciesNames();

    boolean isLegendary(String name);

    boolean isUltraBeast(String name);

    /** Legendaries and mythicals both count as "legendary" for the No Legendaries filter. */
    boolean isMythical(String name);

    EvolutionStage stageOf(String name);

    /**
     * The species names of the vanilla Pixelmon starters (from Pixelmon's starters.json),
     * used as the fallback pool when Whitelist Mode is on but the whitelist is empty.
     */
    List<String> vanillaStarterNames();
}
