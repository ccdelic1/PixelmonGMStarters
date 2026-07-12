package com.pixelmon.gamemachinestarters.client.reel;

import com.pixelmon.gamemachinestarters.GameMachineStartersMod;

import net.minecraft.resources.ResourceLocation;

/** Client texture references for the slot machine. */
public final class GMSTextures {

    /** The decorative slot-machine housing (564x380, three transparent reel windows). */
    public static final ResourceLocation GAMBLING_FRAME =
        ResourceLocation.fromNamespaceAndPath(GameMachineStartersMod.MOD_ID, "textures/gui/gambling_frame.png");

    private GMSTextures() {
    }
}
