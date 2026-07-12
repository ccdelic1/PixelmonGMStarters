package com.pixelmon.gamemachinestarters;

import com.pixelmon.gamemachinestarters.pool.PixelmonSpeciesProvider;
import com.pixelmon.gamemachinestarters.pool.StarterPoolManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Game-bus event handlers. */
public class GameMachineStartersEvents {

    /**
     * Pool assembly needs Pixelmon's species registry and starter config, both of which
     * are fully loaded by the time the server has started.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        StarterPoolManager.rebuild(new PixelmonSpeciesProvider());
    }
}
