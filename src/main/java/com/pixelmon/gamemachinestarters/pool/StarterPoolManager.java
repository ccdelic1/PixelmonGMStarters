package com.pixelmon.gamemachinestarters.pool;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.pixelmon.gamemachinestarters.config.ConfigProxy;

/**
 * Loads the bundled default pool (list.json), assembles the active pool from the
 * config, and provides random selection. Server-side authoritative.
 */
public final class StarterPoolManager {

    private static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");
    private static final String BUNDLED_LIST = "/assets/gamemachinestarters/data/list.json";
    private static final Path DUMP_PATH = Paths.get("config/PixelmonGameMachineStarters-pool-dump.json");

    private static List<PoolEntry> defaultEntries = List.of();
    private static List<PoolEntry> activePool = List.of();
    private static PoolAssembler.PoolMode activeMode = PoolAssembler.PoolMode.DEFAULT;

    private StarterPoolManager() {
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    /** Parses the bundled list.json into tiered default entries. Call once at mod construction. */
    public static void loadBundledList() {
        try (InputStream in = StarterPoolManager.class.getResourceAsStream(BUNDLED_LIST)) {
            if (in == null) {
                throw new IllegalStateException("Bundled pool list not found: " + BUNDLED_LIST);
            }
            defaultEntries = parseList(new InputStreamReader(in, StandardCharsets.UTF_8));
            LOGGER.info("[GameMachineStarters] Loaded bundled starter pool: {} entries", defaultEntries.size());
        } catch (Exception e) {
            LOGGER.error("[GameMachineStarters] Failed to load bundled starter pool", e);
            defaultEntries = List.of();
        }
    }

    /**
     * Parses a list.json-shaped array ({@code [{"pokemon_name": "...", "bst": N}, ...]})
     * into tier-classified entries. Package-private for tests.
     */
    static List<PoolEntry> parseList(java.io.Reader reader) {
        List<PoolEntry> entries = new ArrayList<>();
        JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String name = object.get("pokemon_name").getAsString();
            int bst = object.get("bst").getAsInt();
            entries.add(new PoolEntry(name, bst, RarityTier.classify(name, bst)));
        }
        return entries;
    }

    // ------------------------------------------------------------------
    // Assembly
    // ------------------------------------------------------------------

    /** Rebuilds the active pool from the current config. Call on server start (and config reload). */
    public static void rebuild(SpeciesInfoProvider provider) {
        PoolAssembler.Result result = PoolAssembler.assemble(ConfigProxy.poolSettings(), defaultEntries, provider);
        activePool = result.entries();
        activeMode = result.mode();

        LOGGER.info("[GameMachineStarters] Active starter pool assembled: mode={}, {} entries",
            activeMode, activePool.size());
        for (String skippedEntry : result.skipped()) {
            LOGGER.warn("[GameMachineStarters] Skipped pool entry: {}", skippedEntry);
        }
        if (activePool.isEmpty()) {
            LOGGER.error("[GameMachineStarters] Active starter pool is EMPTY — falling back to vanilla starter species");
            activePool = new ArrayList<>();
            for (String name : provider.vanillaStarterNames()) {
                if (provider.exists(name)) {
                    activePool.add(new PoolEntry(name, 0, RarityTier.NONE));
                }
            }
            activeMode = PoolAssembler.PoolMode.WHITELIST_FALLBACK_VANILLA;
            LOGGER.info("[GameMachineStarters] Fallback pool: {} entries", activePool.size());
        }

        dumpIfEnabled();
    }

    private static void dumpIfEnabled() {
        if (!ConfigProxy.get().isEnableDumpList()) {
            return;
        }
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonArray array = new JsonArray();
            for (PoolEntry entry : activePool) {
                JsonObject object = new JsonObject();
                object.addProperty("pokemon_name", entry.name());
                object.addProperty("bst", entry.bst());
                object.addProperty("tier", entry.tier().name());
                array.add(object);
            }
            Files.createDirectories(DUMP_PATH.getParent() == null ? Paths.get(".") : DUMP_PATH.getParent());
            Files.writeString(DUMP_PATH, gson.toJson(array), StandardCharsets.UTF_8);
            LOGGER.info("[GameMachineStarters] Pool dump written to {}", DUMP_PATH.toAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("[GameMachineStarters] Failed to write pool dump", e);
        }
    }

    // ------------------------------------------------------------------
    // Access & selection
    // ------------------------------------------------------------------

    public static List<PoolEntry> getActivePool() {
        return Collections.unmodifiableList(activePool);
    }

    public static PoolAssembler.PoolMode getActiveMode() {
        return activeMode;
    }

    static List<PoolEntry> getDefaultEntries() {
        return Collections.unmodifiableList(defaultEntries);
    }

    /**
     * Returns up to {@code count} random distinct species names from the active pool,
     * for cosmetic reel filler on the client. Falls back to whatever the pool has.
     */
    public static List<String> sampleNames(Random random, int count) {
        if (activePool.isEmpty()) {
            return List.of();
        }
        List<PoolEntry> copy = new ArrayList<>(activePool);
        Collections.shuffle(copy, random);
        List<String> names = new ArrayList<>(Math.min(count, copy.size()));
        for (int i = 0; i < count && i < copy.size(); i++) {
            names.add(copy.get(i).name());
        }
        return names;
    }

    /**
     * Picks {@code count} random entries, distinct when the pool is large enough
     * (duplicates are allowed only when the pool has fewer than {@code count} entries).
     * Uniform over the pool — the rarity pyramid emerges from pool composition.
     */
    public static List<PoolEntry> pickOffers(Random random, int count) {
        if (activePool.isEmpty()) {
            return List.of();
        }
        List<PoolEntry> picks = new ArrayList<>(count);
        if (activePool.size() < count) {
            for (int i = 0; i < count; i++) {
                picks.add(activePool.get(random.nextInt(activePool.size())));
            }
            return picks;
        }
        List<PoolEntry> copy = new ArrayList<>(activePool);
        for (int i = 0; i < count; i++) {
            picks.add(copy.remove(random.nextInt(copy.size())));
        }
        return picks;
    }
}
