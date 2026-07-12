package com.pixelmon.gamemachinestarters.pool;

import java.util.Map;

/**
 * BST-based rarity tiers per rarityRangeBST.md. Boundaries are the rounded
 * percentiles of the 446-entry analysis dataset (P35/P65/P85/P95):
 * Common &le;300, Uncommon 301-340, Rare 341-470, Epic 471-504, Legendary &ge;505.
 *
 * <p>The original Legendary boundary was &ge;531, but with Paradox Pokemon banned that
 * left only 2 gold-tier Pokemon (Lapras, Duraludon). Per user request (2026-07-10) the
 * boundary was lowered to &ge;505, giving an 11-Pokemon gold tier.</p>
 *
 * <p>{@link #NONE} is used in Whitelist / All Pokemon modes where tiers are
 * disabled and every tile renders gray.</p>
 */
public enum RarityTier {
    COMMON(0xFF9E9E9E),      // grey
    UNCOMMON(0xFF4CAF50),    // green
    RARE(0xFF2196F3),        // blue
    EPIC(0xFF9C27B0),        // purple
    LEGENDARY(0xFFFFC107),   // gold
    NONE(0xFF9E9E9E);        // grey (tiers disabled)

    private final int fillColorArgb;

    RarityTier(int fillColorArgb) {
        this.fillColorArgb = fillColorArgb;
    }

    /** Tile fill color behind the sprite, ARGB. */
    public int getFillColorArgb() {
        return fillColorArgb;
    }

    /**
     * Manual tier overrides from rarityRangeBST.md "Known Quirks":
     * pseudo-legendary first-stagers land in Common/Uncommon by raw BST but their latent
     * strength warrants Epic; Wishiwashi's listed BST (175, Solo Form) undersells its
     * Schooling form, so it is bumped to Uncommon. Keys are normalized names.
     */
    private static final Map<String, RarityTier> OVERRIDES = Map.ofEntries(
        Map.entry("dratini", EPIC),
        Map.entry("larvitar", EPIC),
        Map.entry("bagon", EPIC),
        Map.entry("beldum", EPIC),
        Map.entry("gible", EPIC),
        Map.entry("deino", EPIC),
        Map.entry("goomy", EPIC),
        Map.entry("jangmoo", EPIC),
        Map.entry("dreepy", EPIC),
        Map.entry("frigibax", EPIC),
        Map.entry("wishiwashi", UNCOMMON)
    );

    /** Tier for a raw BST value (no overrides). */
    public static RarityTier fromBst(int bst) {
        if (bst <= 300) {
            return COMMON;
        }
        if (bst <= 340) {
            return UNCOMMON;
        }
        if (bst <= 470) {
            return RARE;
        }
        if (bst <= 504) {
            return EPIC;
        }
        return LEGENDARY;
    }

    /** Tier for a Pokemon, applying the manual overrides first, then BST boundaries. */
    public static RarityTier classify(String name, int bst) {
        RarityTier override = OVERRIDES.get(Names.normalize(name));
        return override != null ? override : fromBst(bst);
    }
}
