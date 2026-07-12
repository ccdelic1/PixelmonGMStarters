package com.pixelmon.gamemachinestarters.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pixelmon.gamemachinestarters.config.ConfigProxy;
import com.pixelmon.gamemachinestarters.server.StarterOfferManager;
import com.pixelmonmod.pixelmon.config.starter.StarterList;

import net.minecraft.server.level.ServerPlayer;

/**
 * Redirects the vanilla starter-granting path to our own. Pixelmon's ChooseStarterPacket
 * (sent by the screen's Begin button) carries the clicked column index; we treat it as
 * the reel slot (0-2) and grant from the player's stored Game Machine Starters offer
 * instead of from Pixelmon's starters.json. This also means a stray or forged vanilla
 * pick can never grant a classic starter — it can only ever redeem the player's own offer.
 */
@Mixin(value = StarterList.class, remap = false)
public abstract class MixinStarterList {

    private static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");

    @Inject(method = "pick", at = @At("HEAD"), cancellable = true)
    private static void gms$redirectVanillaPick(ServerPlayer player, int index, boolean[] options,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (ConfigProxy.isDebug()) {
            LOGGER.info("[GameMachineStarters] Redirecting vanilla starter pick (index {}) from {} to Game Machine Starters offer",
                index, player.getName().getString());
        }
        boolean granted = StarterOfferManager.handleChoice(player, index);
        cir.setReturnValue(granted);
    }
}
