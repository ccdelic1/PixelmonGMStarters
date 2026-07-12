package com.pixelmon.gamemachinestarters.client.reel;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pixelmon.gamemachinestarters.client.ClientOfferState;
import com.pixelmon.gamemachinestarters.net.OfferSlot;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;

import net.minecraft.resources.ResourceLocation;

/**
 * Builds and caches the sprite {@link ResourceLocation}s used by the reels: one per offer
 * plus a set of filler sprites (extra pool species) for visual variety while spinning.
 * Rebuilt only when the active offer changes.
 */
public final class ReelSprites {

    private static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");

    private static List<String> signature = List.of();
    private static ResourceLocation[] offerSprites = new ResourceLocation[0];
    private static ResourceLocation[] fillerSprites = new ResourceLocation[0];

    private ReelSprites() {
    }

    /** Ensures sprites are built for the current offer. Cheap no-op when unchanged. */
    public static void ensureBuilt() {
        List<OfferSlot> slots = ClientOfferState.get();
        if (slots.size() != 3) {
            return;
        }
        List<String> sig = new ArrayList<>();
        for (OfferSlot slot : slots) {
            sig.add(slot.name());
        }
        sig.addAll(ClientOfferState.getFiller());
        if (sig.equals(signature) && offerSprites.length == 3) {
            return;
        }
        signature = sig;

        offerSprites = new ResourceLocation[3];
        for (int i = 0; i < 3; i++) {
            offerSprites[i] = spriteFor(slots.get(i).name());
        }

        List<ResourceLocation> filler = new ArrayList<>();
        for (String name : ClientOfferState.getFiller()) {
            ResourceLocation sprite = spriteFor(name);
            if (sprite != null) {
                filler.add(sprite);
            }
        }
        // Guarantee non-empty filler so the spin always has something to show.
        if (filler.isEmpty()) {
            for (ResourceLocation sprite : offerSprites) {
                if (sprite != null) {
                    filler.add(sprite);
                }
            }
        }
        fillerSprites = filler.toArray(new ResourceLocation[0]);
    }

    private static ResourceLocation spriteFor(String speciesName) {
        try {
            Pokemon pokemon = PokemonSpecificationProxy.create(speciesName).get().create();
            return pokemon.getSprite();
        } catch (Exception e) {
            LOGGER.debug("[GameMachineStarters] Could not build reel sprite for {}: {}", speciesName, e.toString());
            return null;
        }
    }

    public static ResourceLocation offerSprite(int reel) {
        return reel >= 0 && reel < offerSprites.length ? offerSprites[reel] : null;
    }

    public static ResourceLocation fillerSprite(int index) {
        if (fillerSprites.length == 0) {
            return null;
        }
        return fillerSprites[Math.floorMod(index, fillerSprites.length)];
    }

    public static int fillerCount() {
        return fillerSprites.length;
    }
}
