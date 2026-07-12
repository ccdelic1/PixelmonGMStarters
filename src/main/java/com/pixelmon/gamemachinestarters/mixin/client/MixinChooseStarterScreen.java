package com.pixelmon.gamemachinestarters.mixin.client;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pixelmon.gamemachinestarters.client.ClientOfferState;
import com.pixelmon.gamemachinestarters.client.ClientStarterInjector;
import com.pixelmon.gamemachinestarters.client.reel.ReelAnimation;
import com.pixelmon.gamemachinestarters.client.reel.ReelRenderer;
import com.pixelmon.gamemachinestarters.net.OfferSlot;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import com.pixelmonmod.pixelmon.client.gui.starter.StarterButton;
import com.pixelmonmod.pixelmon.client.gui.starter.StarterScreenPhase;
import com.pixelmonmod.pixelmon.config.starter.StarterList;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Turns Pixelmon's starter screen into the Game Machine Starters slot machine: injects the
 * three offers into the client StarterList (so the ability mod can read them and clicks map
 * to the right index), drives the reel animation, draws the reels on top, and nudges the
 * selected-name text and Begin button below the reels.
 *
 * <p>The vanilla starter buttons are used only as click/hover anchors — hidden during the
 * spin, made interactive once settled; their sprites are suppressed in
 * {@code MixinStarterButton}. {@code remap = false}: Pixelmon classes are not obfuscated.</p>
 */
@Mixin(value = ChooseStarterScreen.class, remap = false)
public abstract class MixinChooseStarterScreen {

    @Shadow
    private List<StarterButton> starterButtons;

    @Shadow
    private int clickedIndex;

    @Shadow
    private StarterScreenPhase currentScreen;

    @Shadow
    private int ticksToChange;

    /** Safety cap (ticks) on holding the credits screen if an offer never arrives (~30s). */
    private static final int GMS_MAX_HOLD_TICKS = 600;

    /** How long the credits phase has been held waiting for the offer. */
    @Unique
    private int gms$heldTicks = 0;

    /** Accessor for Pixelmon's private button-layout method, so we can rebuild after a late offer. */
    @Invoker("initButtons")
    abstract void gms$invokeInitButtons();

    /**
     * Holds the Pixelmon credits/copyright screen at the brink of expiring until our starter
     * offer has arrived, so the vanilla starter screen is never shown for even a frame. A
     * generous safety cap prevents an indefinite freeze if an offer somehow never comes.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void gms$holdCreditsForOffer(CallbackInfo ci) {
        if (currentScreen != StarterScreenPhase.Copyright || ticksToChange > 1) {
            return;
        }
        if (ClientOfferState.hasOffer() || gms$heldTicks >= GMS_MAX_HOLD_TICKS) {
            ticksToChange = 1; // let vanilla decrement to 0 and advance this tick
        } else {
            gms$heldTicks++;
            ticksToChange = 2; // stays > 0 after vanilla's decrement — keep showing credits
        }
    }

    /** While the credits are held (no offer yet), clicking must not skip to the vanilla screen. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void gms$blockClickWhileHolding(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        if (currentScreen == StarterScreenPhase.Copyright && !ClientOfferState.hasOffer()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "initButtons", at = @At("HEAD"))
    private void gms$injectBeforeButtons(CallbackInfo ci) {
        ClientStarterInjector.ensureInjected();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void gms$injectOnRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ClientStarterInjector.ensureInjected();
        if (!ClientOfferState.hasOffer()) {
            return;
        }
        // Race fix: if the copyright screen timed out and built its buttons BEFORE our offer
        // arrived, those buttons reflect the vanilla starters (27, all in the left column).
        // Once our offers are injected, resync the buttons so the reels anchor to them.
        if (starterButtons != null && !starterButtons.isEmpty()
                && starterButtons.size() != StarterList.getStarters().size()) {
            gms$invokeInitButtons();
        }
        if (!gms$active()) {
            return;
        }
        List<String> names = new ArrayList<>();
        for (OfferSlot slot : ClientOfferState.get()) {
            names.add(slot.name());
        }
        ReelAnimation.ensureStarted(names);
        // Position and size the (click-only) buttons to match the scaled reel tiles, and make
        // them interactive/hoverable only once the reels have settled. The buttons are hidden
        // (MixinStarterButton suppresses their sprite) — they exist purely for hit-testing.
        boolean settled = ReelAnimation.allSettled();
        int t = gms$tileSize();
        int pitch = gms$pitch();
        int cxCenter = gms$centerX();
        int cy = gms$centerY();
        for (int i = 0; i < 3; i++) {
            StarterButton button = starterButtons.get(i);
            int center = cxCenter + (i - 1) * pitch;
            button.setX(center - t / 2);
            button.setY(cy - t / 2);
            button.setWidth(t);
            button.setHeight(t);
            button.active = settled;
            button.visible = settled;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void gms$drawReels(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!gms$active()) {
            return;
        }
        Screen self = (Screen) (Object) this;
        int pitch = gms$pitch();
        int cxCenter = gms$centerX();
        int[] cx = {cxCenter - pitch, cxCenter, cxCenter + pitch};
        ReelRenderer.render(graphics, cx, gms$centerY(), gms$tileSize(), self.width, self.height, clickedIndex);
    }

    /** Nudge the selected-Pokemon name below the reels. */
    @ModifyArg(
        method = "render",
        at = @At(value = "INVOKE",
            target = "Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;drawCenteredString(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;FFI)V"),
        index = 3,
        remap = false)
    private float gms$moveNameLower(float y) {
        return gms$active() ? gms$reelBottom() + 8f : y;
    }

    /** Nudge the "Begin your adventure" button below the reels/name. */
    @ModifyArg(
        method = "initButtons",
        at = @At(value = "INVOKE",
            target = "Lcom/pixelmonmod/pixelmon/client/gui/widgets/SWSHButton;<init>(DDDDDLjava/lang/String;Ljava/util/function/Consumer;)V",
            ordinal = 0),
        index = 1,
        remap = false)
    private double gms$moveBeginLower(double y) {
        return gms$active() ? gms$reelBottom() + 24.0 : y;
    }

    /**
     * Suppresses the vanilla Pokemon description ("wiki bio") that Pixelmon draws when a
     * starter tile is hovered — we never want it on the slot-machine screen.
     */
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE",
            target = "Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;drawCenteredSplitString(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;FFIIZ)V"),
        remap = false)
    private void gms$hideDescription(GuiGraphics graphics, String text, float x, float y, int maxLength, int color, boolean dropShadow) {
        if (!gms$active()) {
            ScreenHelper.drawCenteredSplitString(graphics, text, x, y, maxLength, color, dropShadow);
        }
        // active => draw nothing (description hidden)
    }

    private boolean gms$active() {
        // Exactly 3 => the buttons are our injected offers, not the vanilla starter set.
        return ClientOfferState.hasOffer() && starterButtons != null && starterButtons.size() == 3;
    }

    // ------------------------------------------------------------------
    // Slot-machine layout. Everything is a fixed multiple of the base sizes times GMS_SCALE,
    // and centered on the screen, so the whole machine (columns, tiles, sprites, housing)
    // scales together. Base values (at scale 1): 72px tile, 100px column pitch — which give
    // the 282x190 housing template. Change GMS_SCALE to resize the entire machine uniformly.
    // ------------------------------------------------------------------
    private static final float GMS_SCALE = 0.65f;
    private static final int GMS_BASE_TILE = 72;
    private static final int GMS_BASE_PITCH = 100;

    private int gms$tileSize() {
        return Math.round(GMS_BASE_TILE * GMS_SCALE);
    }

    private int gms$pitch() {
        return Math.round(GMS_BASE_PITCH * GMS_SCALE);
    }

    private int gms$centerX() {
        return ((Screen) (Object) this).width / 2;
    }

    private int gms$centerY() {
        return ((Screen) (Object) this).height / 2;
    }

    private float gms$reelBottom() {
        return gms$centerY() + gms$tileSize() * 1.25f;
    }
}
