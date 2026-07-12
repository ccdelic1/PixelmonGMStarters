package com.pixelmon.gamemachinestarters.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-persisted per-player starter offers (overworld data storage). Persisting the
 * offer prevents the slot-machine reroll exploit: relogging re-serves the same three
 * Pokemon until a choice is made.
 */
public class OfferSavedData extends SavedData {

    private static final String DATA_NAME = "gamemachinestarters_offers";

    private final Map<UUID, StarterOffer> offers = new HashMap<>();

    public static OfferSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new Factory<>(OfferSavedData::new, OfferSavedData::load, null),
            DATA_NAME
        );
    }

    private static OfferSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        OfferSavedData data = new OfferSavedData();
        for (String key : tag.getAllKeys()) {
            try {
                data.offers.put(UUID.fromString(key), StarterOffer.fromNbt(tag.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
                // not a UUID key; skip
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<UUID, StarterOffer> entry : offers.entrySet()) {
            tag.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        return tag;
    }

    public StarterOffer getOffer(UUID player) {
        return offers.get(player);
    }

    public void setOffer(UUID player, StarterOffer offer) {
        offers.put(player, offer);
        setDirty();
    }

    public void clearOffer(UUID player) {
        if (offers.remove(player) != null) {
            setDirty();
        }
    }
}
