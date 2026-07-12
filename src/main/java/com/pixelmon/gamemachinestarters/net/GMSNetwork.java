package com.pixelmon.gamemachinestarters.net;

import com.pixelmon.gamemachinestarters.client.ClientOfferState;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration and handlers.
 *
 * <p>Only the S2C offer is a custom payload. The client's choice is carried by Pixelmon's
 * own ChooseStarterPacket (Begin button) and redirected server-side in
 * {@code MixinStarterList}, so there is no custom C2S packet.</p>
 */
public final class GMSNetwork {

    private GMSNetwork() {
    }

    /** Registered on the MOD event bus. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(StarterOfferPayload.TYPE, StarterOfferPayload.STREAM_CODEC, GMSNetwork::handleOfferOnClient);
    }

    private static void handleOfferOnClient(StarterOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientOfferState.set(payload.slots(), payload.filler()));
    }
}
