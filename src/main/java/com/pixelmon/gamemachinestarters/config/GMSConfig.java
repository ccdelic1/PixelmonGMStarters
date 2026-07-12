package com.pixelmon.gamemachinestarters.config;

import java.util.ArrayList;
import java.util.List;

import com.pixelmon.gamemachinestarters.pool.ParadoxPokemon;
import com.pixelmonmod.pixelmon.api.config.api.data.ConfigPath;
import com.pixelmonmod.pixelmon.api.config.api.yaml.AbstractYamlConfig;

import info.pixelmon.repack.org.spongepowered.objectmapping.ConfigSerializable;
import info.pixelmon.repack.org.spongepowered.objectmapping.meta.Comment;

/**
 * Configuration for Pixelmon: Game Machine Starters.
 *
 * <p>Generated at {@code config/PixelmonGameMachineStarters.yaml} the first time the
 * mod loads. Uses Pixelmon's own YAML config system (same engine as the reference
 * configs in configReferences/, e.g. PixelmonAbilityRandomizer.yaml) so structure,
 * comments and kebab-case key formatting match them exactly.</p>
 */
@ConfigSerializable
@ConfigPath("config/PixelmonGameMachineStarters.yaml")
public class GMSConfig extends AbstractYamlConfig {

    @Comment("Default mode options")
    private DefaultModeSection defaultMode = new DefaultModeSection();

    @Comment("All Pokemon mode options (pool = every Pokemon Pixelmon knows)")
    private AllPokemonSection allPokemon = new AllPokemonSection();

    @Comment("Debug / troubleshooting options")
    private DebugSection debug = new DebugSection();

    @ConfigSerializable
    public static class DefaultModeSection {
        @Comment(
            "No Choices mode (default false).\n"
            + "If enabled, the starter screen is not shown at all. The player is simply given\n"
            + "one random Pokemon from the pool as soon as they join the world/server.\n"
            + "No choice, no screen - it's just in their party at level 5."
        )
        private boolean noChoicesMode = false;

        @Comment(
            "Max IVs mode (default true).\n"
            + "If enabled, Pokemon given/chosen as a starter always have max IVs (31 in every stat)."
        )
        private boolean maxIvsMode = true;

        @Comment(
            "Blacklist mode (default true).\n"
            + "Enables the 'blacklist' list below. Enabled by default because the blacklist is\n"
            + "pre-seeded with the Paradox Pokemon (see below)."
        )
        private boolean blacklistMode = true;

        @Comment(
            "Pokemon that may never be given as a starter (only enforced when blacklistMode is true).\n"
            + "Write plain species names, one per list item. Capitalization, spaces and punctuation\n"
            + "are ignored ('Mr. Mime', 'mrmime' and 'MR MIME' all match the same species).\n"
            + "This defaults to the Paradox Pokemon so they are excluded out of the box. To allow a\n"
            + "Paradox Pokemon into the pool, delete its line here (or disable blacklistMode)."
        )
        private List<String> blacklist = new ArrayList<>(ParadoxPokemon.DEFAULT_BLACKLIST);

        @Comment(
            "Whitelist mode (default false).\n"
            + "Enables the 'whitelist' list below."
        )
        private boolean whitelistMode = false;

        @Comment(
            "If whitelistMode is enabled, ONLY Pokemon in this list are in the random selection pool.\n"
            + "No rarity tiers apply - all tile backgrounds are gray (avoids base stat calculation).\n"
            + "If whitelistMode is enabled and this list is empty, the pool falls back to the vanilla\n"
            + "Pixelmon starters, still presented through the randomized selector."
        )
        private List<String> whitelist = new ArrayList<>();
    }

    @ConfigSerializable
    public static class AllPokemonSection {
        @Comment(
            "All Pokemon mode (default false).\n"
            + "Any Pokemon can be given as a starter. No rarity tiers apply - all tile backgrounds\n"
            + "are gray (avoids base stat calculation for the full dex)."
        )
        private boolean allPokemonMode = false;

        @Comment("Only if All Pokemon mode is enabled: remove legendaries (and mythicals) from the pool.")
        private boolean noLegendariesMode = false;

        @Comment("Only if All Pokemon mode is enabled: remove Ultra Beasts from the pool.")
        private boolean noUltraBeastsMode = false;

        @Comment("Only if All Pokemon mode is enabled: remove first-stage evolution Pokemon from the pool.")
        private boolean noFirstStageEvolutionsMode = false;

        @Comment("Only if All Pokemon mode is enabled: remove second-stage evolution Pokemon from the pool.")
        private boolean noSecondStageEvolutionsMode = false;

        @Comment("Only if All Pokemon mode is enabled: remove third-stage evolution Pokemon from the pool.")
        private boolean noThirdStageEvolutionsMode = false;

        @Comment("Only if All Pokemon mode is enabled: remove single-stage (no evolution line) Pokemon from the pool.")
        private boolean noSingleStageEvolutionsMode = false;
    }

    @ConfigSerializable
    public static class DebugSection {
        @Comment(
            "Enable dump list (default false).\n"
            + "If enabled, the resolved Pokemon pool ('name' + 'base stat' JSON) is saved to\n"
            + "config/PixelmonGameMachineStarters-pool-dump.json on next game launch so you can\n"
            + "inspect exactly what is in play."
        )
        private boolean enableDumpList = false;

        @Comment("Enable debug logs (default false). Verbose debug logging printed to console.")
        private boolean enableDebugLogs = false;
    }

    // ------------------------------------------------------------------
    // Null-safe getters
    // ------------------------------------------------------------------

    public boolean isNoChoicesMode() {
        return defaultMode != null && defaultMode.noChoicesMode;
    }

    public boolean isMaxIvsMode() {
        return defaultMode == null || defaultMode.maxIvsMode;
    }

    public boolean isBlacklistMode() {
        return defaultMode != null && defaultMode.blacklistMode;
    }

    public List<String> getBlacklist() {
        return defaultMode == null || defaultMode.blacklist == null ? new ArrayList<>() : defaultMode.blacklist;
    }

    public boolean isWhitelistMode() {
        return defaultMode != null && defaultMode.whitelistMode;
    }

    public List<String> getWhitelist() {
        return defaultMode == null || defaultMode.whitelist == null ? new ArrayList<>() : defaultMode.whitelist;
    }

    public boolean isAllPokemonMode() {
        return allPokemon != null && allPokemon.allPokemonMode;
    }

    public boolean isNoLegendariesMode() {
        return allPokemon != null && allPokemon.noLegendariesMode;
    }

    public boolean isNoUltraBeastsMode() {
        return allPokemon != null && allPokemon.noUltraBeastsMode;
    }

    public boolean isNoFirstStageEvolutionsMode() {
        return allPokemon != null && allPokemon.noFirstStageEvolutionsMode;
    }

    public boolean isNoSecondStageEvolutionsMode() {
        return allPokemon != null && allPokemon.noSecondStageEvolutionsMode;
    }

    public boolean isNoThirdStageEvolutionsMode() {
        return allPokemon != null && allPokemon.noThirdStageEvolutionsMode;
    }

    public boolean isNoSingleStageEvolutionsMode() {
        return allPokemon != null && allPokemon.noSingleStageEvolutionsMode;
    }

    public boolean isEnableDumpList() {
        return debug != null && debug.enableDumpList;
    }

    public boolean isEnableDebugLogs() {
        return debug != null && debug.enableDebugLogs;
    }
}
