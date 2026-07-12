package com.pixelmon.gamemachinestarters.pool;

import java.util.List;

/**
 * Immutable snapshot of the pool-relevant config values, so the pool assembler is a
 * pure function that unit tests can drive without loading the YAML config or Pixelmon.
 */
public record PoolSettings(
    boolean blacklistEnabled,
    List<String> blacklist,
    boolean whitelistEnabled,
    List<String> whitelist,
    boolean allPokemonMode,
    boolean noLegendaries,
    boolean noUltraBeasts,
    boolean noFirstStage,
    boolean noSecondStage,
    boolean noThirdStage,
    boolean noSingleStage
) {

    /** Settings with everything off — the default bundled-list pool. */
    public static PoolSettings defaults() {
        return new PoolSettings(false, List.of(), false, List.of(), false,
            false, false, false, false, false, false);
    }
}
