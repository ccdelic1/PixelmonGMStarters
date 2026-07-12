package com.pixelmon.gamemachinestarters.pool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure pool-assembly logic. Given the default (bundled) entries, the config snapshot,
 * and a species info provider, produces the active selection pool.
 *
 * <p>Precedence (ComprehensivePlan.md §6.4): Whitelist Mode &gt; All Pokemon Mode &gt;
 * default bundled list. The blacklist applies subtractively to whichever pool is
 * active. Paradox Pokemon are excluded via the blacklist (pre-seeded and enabled by
 * default, see {@link ParadoxPokemon}), not by any hardcoded rule.</p>
 */
public final class PoolAssembler {

    /** Which source produced the active pool (drives gray-tile rendering + logging). */
    public enum PoolMode {
        /** Bundled list.json with BST rarity tiers. */
        DEFAULT,
        /** User whitelist; tiers disabled (gray). */
        WHITELIST,
        /** Whitelist Mode on but list empty; vanilla Pixelmon starters, tiers disabled (gray). */
        WHITELIST_FALLBACK_VANILLA,
        /** Every species Pixelmon knows, minus filters; tiers disabled (gray). */
        ALL_POKEMON
    }

    public record Result(PoolMode mode, List<PoolEntry> entries, List<String> skipped) {
    }

    private PoolAssembler() {
    }

    public static Result assemble(PoolSettings settings, List<PoolEntry> defaultEntries, SpeciesInfoProvider provider) {
        List<String> skipped = new ArrayList<>();
        PoolMode mode;
        List<PoolEntry> entries;

        if (settings.whitelistEnabled()) {
            List<PoolEntry> whitelisted = fromNames(settings.whitelist(), provider, skipped);
            if (whitelisted.isEmpty()) {
                mode = PoolMode.WHITELIST_FALLBACK_VANILLA;
                entries = fromNames(provider.vanillaStarterNames(), provider, skipped);
            } else {
                mode = PoolMode.WHITELIST;
                entries = whitelisted;
            }
        } else if (settings.allPokemonMode()) {
            mode = PoolMode.ALL_POKEMON;
            entries = new ArrayList<>();
            for (String name : provider.allSpeciesNames()) {
                if (settings.noLegendaries() && (provider.isLegendary(name) || provider.isMythical(name))) {
                    continue;
                }
                if (settings.noUltraBeasts() && provider.isUltraBeast(name)) {
                    continue;
                }
                EvolutionStage stage = provider.stageOf(name);
                if (settings.noFirstStage() && stage == EvolutionStage.FIRST) {
                    continue;
                }
                if (settings.noSecondStage() && stage == EvolutionStage.SECOND) {
                    continue;
                }
                if (settings.noThirdStage() && stage == EvolutionStage.THIRD) {
                    continue;
                }
                if (settings.noSingleStage() && stage == EvolutionStage.SINGLE) {
                    continue;
                }
                entries.add(new PoolEntry(name, 0, RarityTier.NONE));
            }
        } else {
            mode = PoolMode.DEFAULT;
            entries = new ArrayList<>();
            for (PoolEntry entry : defaultEntries) {
                if (!provider.exists(entry.name())) {
                    skipped.add(entry.name() + " (unknown species)");
                    continue;
                }
                entries.add(entry);
            }
        }

        if (settings.blacklistEnabled() && !settings.blacklist().isEmpty()) {
            Set<String> banned = new HashSet<>();
            for (String name : settings.blacklist()) {
                banned.add(Names.normalize(name));
            }
            entries.removeIf(entry -> banned.contains(entry.normalizedName()));
        }

        // Always drop Pokemon that exist but aren't implemented (placeholder "none" model,
        // "fallback warning" render). Hardcoded and invisible — applies to every mode.
        entries.removeIf(entry -> UnimplementedPokemon.isUnimplemented(entry.name()));

        return new Result(mode, entries, skipped);
    }

    /** Builds gray (tier-less) entries from raw names, skipping unknown species and duplicates. */
    private static List<PoolEntry> fromNames(List<String> names, SpeciesInfoProvider provider, List<String> skipped) {
        List<PoolEntry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            if (!seen.add(Names.normalize(name))) {
                continue;
            }
            if (!provider.exists(name)) {
                skipped.add(name + " (unknown species)");
                continue;
            }
            entries.add(new PoolEntry(name, 0, RarityTier.NONE));
        }
        return entries;
    }
}
