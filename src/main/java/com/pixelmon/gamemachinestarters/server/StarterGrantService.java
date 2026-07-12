package com.pixelmon.gamemachinestarters.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pixelmon.gamemachinestarters.config.ConfigProxy;
import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.TickHandler;
import com.pixelmonmod.pixelmon.api.events.PokemonReceivedEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;

import net.minecraft.server.level.ServerPlayer;

/**
 * Creates and grants the final starter Pokemon. Mirrors the vanilla
 * {@code StarterList.pick} semantics (starterPicked flag, PokemonReceivedEvent with
 * receive type "Starter", party add, starter-queue deregistration) but builds the
 * Pokemon from our own spec: species + pre-rolled ability + level 5 + optional max IVs.
 */
public final class StarterGrantService {

    private static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");

    /** Starters are always granted at level 5 (9.3.16 has no starter-level config). */
    private static final int STARTER_LEVEL = 5;

    private StarterGrantService() {
    }

    /**
     * Grants the given species to the player as their starter.
     *
     * @param abilityName pre-rolled ability registry name, or empty to let creation roll one.
     * @return true if the Pokemon was granted.
     */
    public static boolean grant(ServerPlayer player, String speciesName, String abilityName) {
        PlayerPartyStorage storage = StorageProxy.getPartyNow(player);
        if (storage == null) {
            LOGGER.warn("[GameMachineStarters] No party storage for {}; cannot grant starter", player.getName().getString());
            return false;
        }
        if (storage.starterPicked) {
            LOGGER.warn("[GameMachineStarters] {} already picked a starter; ignoring grant", player.getName().getString());
            return false;
        }

        Pokemon pokemon = create(speciesName, abilityName);
        if (pokemon == null) {
            return false;
        }
        if (ConfigProxy.get().isMaxIvsMode()) {
            pokemon.getIVs().maximizeIVs();
        }

        storage.starterPicked = true;
        if (((PokemonReceivedEvent) Pixelmon.EVENT_BUS.post(new PokemonReceivedEvent(player, pokemon, "Starter"))).isCanceled()) {
            LOGGER.info("[GameMachineStarters] PokemonReceivedEvent canceled starter grant for {}", player.getName().getString());
            return false;
        }
        storage.add(pokemon);
        TickHandler.deregisterStarterList(player);
        LOGGER.info("[GameMachineStarters] Granted starter {} (ability {}) to {}",
            speciesName, pokemon.getAbility().getName(), player.getName().getString());
        return true;
    }

    /** Builds the starter from a spec string, falling back to species-only if the full spec fails. */
    private static Pokemon create(String speciesName, String abilityName) {
        String fullSpec = speciesName
            + (abilityName == null || abilityName.isBlank() ? "" : " ability:" + abilityName)
            + " lvl:" + STARTER_LEVEL;
        try {
            return createFromSpec(fullSpec);
        } catch (Exception e) {
            LOGGER.warn("[GameMachineStarters] Spec '{}' failed ({}); retrying species-only", fullSpec, e.toString());
        }
        try {
            Pokemon pokemon = createFromSpec(speciesName + " lvl:" + STARTER_LEVEL);
            return pokemon;
        } catch (Exception e) {
            LOGGER.error("[GameMachineStarters] Could not create starter '{}'", speciesName, e);
            return null;
        }
    }

    private static Pokemon createFromSpec(String spec) {
        PokemonSpecification specification = PokemonSpecificationProxy.create(spec).get();
        return specification.create();
    }
}
