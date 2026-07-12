package com.pixelmon.gamemachinestarters.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pixelmon.gamemachinestarters.config.ConfigProxy;
import com.pixelmon.gamemachinestarters.net.OfferSlot;
import com.pixelmon.gamemachinestarters.net.StarterOfferPayload;
import com.pixelmon.gamemachinestarters.pool.PoolEntry;
import com.pixelmon.gamemachinestarters.pool.StarterPoolManager;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.TickHandler;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-authoritative starter offers: generation when Pixelmon queues a player for the
 * starter screen, persistence across relogs (anti-reroll), payload sync to the client,
 * choice validation and granting, and the No Choices instant-grant mode.
 */
public final class StarterOfferManager {

    private static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");
    private static final Random RANDOM = new Random();
    private static final int OFFER_COUNT = 3;
    /** Extra pool species sent for cosmetic reel filler on the client. */
    private static final int FILLER_COUNT = 16;

    private StarterOfferManager() {
    }

    /**
     * Called (via mixin) whenever Pixelmon registers a player for the starter screen —
     * covers both the login flow and commands like /starter and /pokerestart. May be
     * invoked off-thread (storage futures), so all work is queued onto the server thread.
     */
    public static void onStarterQueueRegistered(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            if (ConfigProxy.get().isNoChoicesMode()) {
                grantRandomNoChoice(player);
                return;
            }

            OfferSavedData data = OfferSavedData.get(server);
            StarterOffer offer = data.getOffer(player.getUUID());
            if (offer == null || offer.slots().size() != OFFER_COUNT) {
                offer = generateOffer();
                if (offer == null) {
                    LOGGER.error("[GameMachineStarters] Could not generate a starter offer (empty pool?)");
                    return;
                }
                data.setOffer(player.getUUID(), offer);
                if (ConfigProxy.isDebug()) {
                    LOGGER.info("[GameMachineStarters] Generated offer for {}: {}",
                        player.getName().getString(), offer.slots());
                }
            }
            PacketDistributor.sendToPlayer(player,
                new StarterOfferPayload(offer.slots(), StarterPoolManager.sampleNames(RANDOM, FILLER_COUNT)));
        });
    }

    /**
     * Handles the player's slot choice, redirected from Pixelmon's ChooseStarterPacket
     * (its index carries the clicked reel column, 0-2). Runs on the server thread.
     *
     * @return true if a starter was granted.
     */
    public static boolean handleChoice(ServerPlayer player, int slot) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        if (slot < 0 || slot >= OFFER_COUNT) {
            LOGGER.warn("[GameMachineStarters] {} sent invalid starter slot {}", player.getName().getString(), slot);
            return false;
        }
        OfferSavedData data = OfferSavedData.get(server);
        StarterOffer offer = data.getOffer(player.getUUID());
        if (offer == null || offer.slots().size() != OFFER_COUNT) {
            LOGGER.warn("[GameMachineStarters] {} sent a starter choice but has no offer", player.getName().getString());
            return false;
        }
        PlayerPartyStorage storage = StorageProxy.getPartyNow(player);
        if (storage == null || storage.starterPicked) {
            LOGGER.warn("[GameMachineStarters] {} sent a starter choice but cannot receive a starter", player.getName().getString());
            return false;
        }

        OfferSlot chosen = offer.slots().get(slot);
        if (StarterGrantService.grant(player, chosen.name(), chosen.ability())) {
            data.clearOffer(player.getUUID());
            return true;
        }
        return false;
    }

    /** No Choices Mode: one random pool Pokemon straight into the party, no screen. */
    private static void grantRandomNoChoice(ServerPlayer player) {
        List<PoolEntry> pick = StarterPoolManager.pickOffers(RANDOM, 1);
        if (pick.isEmpty()) {
            LOGGER.error("[GameMachineStarters] No Choices Mode: pool is empty, cannot grant a starter");
            return;
        }
        if (StarterGrantService.grant(player, pick.get(0).name(), "")) {
            TickHandler.deregisterStarterList(player);
        }
    }

    /** Three distinct uniform picks, each with a pre-rolled ability for display fidelity. */
    private static StarterOffer generateOffer() {
        List<PoolEntry> picks = StarterPoolManager.pickOffers(RANDOM, OFFER_COUNT);
        if (picks.isEmpty()) {
            return null;
        }
        while (picks.size() < OFFER_COUNT) {
            picks = new ArrayList<>(picks);
            picks.add(picks.get(RANDOM.nextInt(picks.size())));
        }
        List<OfferSlot> slots = new ArrayList<>(OFFER_COUNT);
        for (PoolEntry pick : picks) {
            slots.add(new OfferSlot(pick.name(), rollAbility(pick.name()), pick.tier().ordinal()));
        }
        return new StarterOffer(slots);
    }

    /**
     * Rolls an ability by creating a throwaway Pokemon from the bare species spec, exactly
     * as the screen's display path would. Locking the rolled ability into the offer keeps
     * the ability shown on screen identical to the ability of the granted Pokemon.
     */
    private static String rollAbility(String speciesName) {
        try {
            Pokemon rolled = PokemonSpecificationProxy.create(speciesName).get().create();
            return rolled.getAbility().getName();
        } catch (Exception e) {
            LOGGER.warn("[GameMachineStarters] Could not pre-roll ability for {}: {}", speciesName, e.toString());
            return "";
        }
    }
}
