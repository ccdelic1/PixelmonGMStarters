package com.pixelmon.gamemachinestarters.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pixelmon.gamemachinestarters.client.ClientOfferState;
import com.pixelmonmod.pixelmon.client.gui.starter.StarterButton;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Suppresses the vanilla starter-tile sprite so the Game Machine Starters reels can draw
 * the tiles instead. The button still exists for click/hover handling (see
 * {@code MixinChooseStarterScreen}); only its rendering is cancelled.
 *
 * <p>{@code remap = false}: Pixelmon class.</p>
 */
@Mixin(value = StarterButton.class, remap = false)
public abstract class MixinStarterButton {

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void gms$suppressVanillaTile(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ClientOfferState.hasOffer()) {
            ci.cancel();
        }
    }
}
