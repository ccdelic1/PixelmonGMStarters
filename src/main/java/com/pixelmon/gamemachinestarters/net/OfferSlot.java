package com.pixelmon.gamemachinestarters.net;

import com.pixelmon.gamemachinestarters.pool.RarityTier;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One reel column's offered Pokemon, as shared with the client.
 *
 * @param name    Pixelmon species name (e.g. "Bulbasaur").
 * @param ability pre-rolled ability registry name, so the displayed ability matches the
 *                granted Pokemon exactly; empty = let creation roll it.
 * @param tier    rarity tier ordinal ({@link RarityTier}).
 */
public record OfferSlot(String name, String ability, int tier) {

    public RarityTier rarityTier() {
        RarityTier[] values = RarityTier.values();
        return tier >= 0 && tier < values.length ? values[tier] : RarityTier.NONE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUtf(ability);
        buf.writeVarInt(tier);
    }

    public static OfferSlot read(FriendlyByteBuf buf) {
        return new OfferSlot(buf.readUtf(), buf.readUtf(), buf.readVarInt());
    }
}
