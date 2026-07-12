package com.pixelmon.gamemachinestarters.config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pixelmon.gamemachinestarters.pool.PoolSettings;
import com.pixelmonmod.pixelmon.api.config.api.data.ConfigPath;
import com.pixelmonmod.pixelmon.api.config.api.yaml.AbstractYamlConfig;

import info.pixelmon.repack.org.spongepowered.CommentedConfigurationNode;
import info.pixelmon.repack.org.spongepowered.ConfigurationOptions;
import info.pixelmon.repack.org.spongepowered.loader.HeaderMode;
import info.pixelmon.repack.org.spongepowered.reference.ConfigurationReference;
import info.pixelmon.repack.org.spongepowered.reference.ValueReference;
import info.pixelmon.repack.org.spongepowered.yaml.NodeStyle;
import info.pixelmon.repack.org.spongepowered.yaml.YamlConfigurationLoader;

/**
 * Loads, holds and exposes the {@link GMSConfig} instance.
 *
 * <p>Uses Pixelmon's repackaged SpongePowered YAML config library directly (same
 * pattern as the Ability Randomizer's ConfigProxy) so the generated file has no
 * PixelmonMod branding header.</p>
 */
public final class ConfigProxy {

    private static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");

    private static GMSConfig config;

    private ConfigProxy() {
    }

    /** Load (or create) the config YAML. */
    public static void reload() {
        try {
            ConfigPath annotation = GMSConfig.class.getAnnotation(ConfigPath.class);
            if (annotation == null) {
                throw new IOException("GMSConfig is missing @ConfigPath");
            }

            Path configFile = Paths.get(annotation.value());
            if (!configFile.toFile().exists()) {
                configFile.getParent().toFile().mkdirs();
                configFile.toFile().createNewFile();
            }

            ConfigurationReference<CommentedConfigurationNode> base =
                ConfigurationReference.fixed(
                    YamlConfigurationLoader.builder()
                        .headerMode(HeaderMode.PRESERVE)
                        .nodeStyle(NodeStyle.BLOCK)
                        .commentsEnabled(true)
                        .defaultOptions(
                            ConfigurationOptions.defaults()
                                .header("")
                                .nativeTypes(nativeTypeSet())
                        )
                        .defaultOptions(opts -> opts.shouldCopyDefaults(true))
                        .path(configFile.toAbsolutePath())
                        .build()
                );

            if (base == null) {
                throw new IOException("Config loaded as null");
            }

            ValueReference<GMSConfig, CommentedConfigurationNode> reference =
                base.referenceTo(GMSConfig.class);

            GMSConfig instance = reference.get();
            if (instance == null) {
                throw new IOException("Config instance is null");
            }

            // Wire the internal references via reflection (same pattern as YamlConfigFactory,
            // but we are in a different package so we cannot access protected fields directly).
            Field baseField = AbstractYamlConfig.class.getDeclaredField("base");
            baseField.setAccessible(true);
            baseField.set(instance, base);

            Field configField = AbstractYamlConfig.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(instance, reference);

            instance.save();

            config = instance;
            LOGGER.info("[GameMachineStarters] Configuration loaded successfully");
        } catch (Exception e) {
            LOGGER.error("[GameMachineStarters] Failed to load configuration; falling back to defaults", e);
            config = new GMSConfig();
        }
    }

    private static Set<Class<?>> nativeTypeSet() {
        Set<Class<?>> types = new HashSet<>();
        types.add(String.class);
        types.add(Integer.class);
        types.add(Byte.class);
        types.add(Double.class);
        types.add(Boolean.class);
        types.add(Long.class);
        types.add(Map.class);
        types.add(List.class);
        return types;
    }

    public static GMSConfig get() {
        if (config == null) {
            config = new GMSConfig();
        }
        return config;
    }

    public static boolean isLoaded() {
        return config != null;
    }

    public static boolean isDebug() {
        return config != null && config.isEnableDebugLogs();
    }

    /** Snapshot of the pool-relevant config values, for the (pure, testable) pool assembler. */
    public static PoolSettings poolSettings() {
        GMSConfig c = get();
        return new PoolSettings(
            c.isBlacklistMode(), c.getBlacklist(),
            c.isWhitelistMode(), c.getWhitelist(),
            c.isAllPokemonMode(),
            c.isNoLegendariesMode(), c.isNoUltraBeastsMode(),
            c.isNoFirstStageEvolutionsMode(), c.isNoSecondStageEvolutionsMode(),
            c.isNoThirdStageEvolutionsMode(), c.isNoSingleStageEvolutionsMode()
        );
    }
}
