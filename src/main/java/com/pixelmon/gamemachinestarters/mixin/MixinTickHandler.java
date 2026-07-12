package com.pixelmon.gamemachinestarters.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pixelmon.gamemachinestarters.server.StarterOfferManager;
import com.pixelmonmod.pixelmon.TickHandler;

import net.minecraft.server.level.ServerPlayer;

/**
 * Hooks Pixelmon queueing a player for the starter screen — the single funnel through
 * which both the login flow (PixelmonStorageManager) and commands (/starter,
 * /pokerestart) request a starter. We piggyback to generate/re-send our offer, or to
 * grant instantly in No Choices Mode.
 *
 * <p>{@code remap = false}: Pixelmon classes are not obfuscated.</p>
 */
@Mixin(value = TickHandler.class, remap = false)
public abstract class MixinTickHandler {

    @Inject(method = "registerStarterList", at = @At("TAIL"))
    private static void gms$onRegisterStarterList(ServerPlayer player, CallbackInfo ci) {
        StarterOfferManager.onStarterQueueRegistered(player);
    }
}
