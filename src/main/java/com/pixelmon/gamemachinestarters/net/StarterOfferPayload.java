package com.pixelmon.gamemachinestarters.net;

import java.util.ArrayList;
import java.util.List;

import com.pixelmon.gamemachinestarters.GameMachineStartersMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: the player's three (server-decided) starter offers, plus a set of extra pool
 * species names used purely as cosmetic filler in the spinning reels.
 */
public record StarterOfferPayload(List<OfferSlot> slots, List<String> filler) implements CustomPacketPayload {

    public static final Type<StarterOfferPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(GameMachineStartersMod.MOD_ID, "starter_offer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StarterOfferPayload> STREAM_CODEC =
        StreamCodec.of(StarterOfferPayload::write, StarterOfferPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, StarterOfferPayload payload) {
        buf.writeVarInt(payload.slots.size());
        for (OfferSlot slot : payload.slots) {
            slot.write(buf);
        }
        buf.writeVarInt(payload.filler.size());
        for (String name : payload.filler) {
            buf.writeUtf(name);
        }
    }

    private static StarterOfferPayload read(RegistryFriendlyByteBuf buf) {
        int slotCount = buf.readVarInt();
        List<OfferSlot> slots = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            slots.add(OfferSlot.read(buf));
        }
        int fillerCount = buf.readVarInt();
        List<String> filler = new ArrayList<>(fillerCount);
        for (int i = 0; i < fillerCount; i++) {
            filler.add(buf.readUtf());
        }
        return new StarterOfferPayload(slots, filler);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
