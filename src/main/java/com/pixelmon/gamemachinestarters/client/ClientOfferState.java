package com.pixelmon.gamemachinestarters.client;

import java.util.List;

import com.pixelmon.gamemachinestarters.net.OfferSlot;

/**
 * Client-side holder for the latest starter offer received from the server.
 * Deliberately free of client-only Minecraft imports so it is safe to reference
 * from common network-registration code on a dedicated server.
 */
public final class ClientOfferState {

    private static volatile List<OfferSlot> slots = List.of();
    private static volatile List<String> filler = List.of();

    private ClientOfferState() {
    }

    public static void set(List<OfferSlot> newSlots, List<String> newFiller) {
        slots = List.copyOf(newSlots);
        filler = List.copyOf(newFiller);
    }

    public static List<OfferSlot> get() {
        return slots;
    }

    public static List<String> getFiller() {
        return filler;
    }

    public static boolean hasOffer() {
        return slots.size() == 3;
    }

    public static void clear() {
        slots = List.of();
        filler = List.of();
    }
}
