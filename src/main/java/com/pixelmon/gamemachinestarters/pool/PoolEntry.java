package com.pixelmon.gamemachinestarters.pool;

/**
 * One Pokemon in the active selection pool.
 *
 * @param name Pixelmon species name as written in the source list (e.g. "Nidoranfemale").
 * @param bst  base stat total; 0 when unknown (whitelist / All Pokemon modes, where it is unused).
 * @param tier rarity tier driving the tile fill color; {@link RarityTier#NONE} when tiers are disabled.
 */
public record PoolEntry(String name, int bst, RarityTier tier) {

    /** Normalized name for matching. */
    public String normalizedName() {
        return Names.normalize(name);
    }
}
