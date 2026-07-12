package com.pixelmon.gamemachinestarters;

import com.pixelmon.gamemachinestarters.config.ConfigProxy;
import com.pixelmon.gamemachinestarters.net.GMSNetwork;
import com.pixelmon.gamemachinestarters.pool.StarterPoolManager;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Pixelmon: Game Machine Starters — replaces Pixelmon's starter selection screen
 * with a slot-machine style randomized starter picker.
 */
@Mod(GameMachineStartersMod.MOD_ID)
public class GameMachineStartersMod {

    public static final String MOD_ID = "gamemachinestarters";
    public static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");

    public GameMachineStartersMod(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("Pixelmon: Game Machine Starters loading (version {})",
            container.getModInfo().getVersion());

        ConfigProxy.reload();
        StarterPoolManager.loadBundledList();
        modEventBus.addListener(GMSNetwork::register);
        NeoForge.EVENT_BUS.register(new GameMachineStartersEvents());
    }
}
