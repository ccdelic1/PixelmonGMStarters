package com.pixelmon.gamemachinestarters.client.reel;

import java.util.List;

import com.pixelmon.gamemachinestarters.client.ClientOfferState;
import com.pixelmon.gamemachinestarters.net.OfferSlot;
import com.pixelmon.gamemachinestarters.pool.RarityTier;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * Draws the three-reel slot machine: framed tiles with rarity fills scrolling vertically
 * with motion blur, decelerating into place; a black border around the slot window; a
 * metallic separator between reels; a center marker; a glow on high-tier landed tiles; a
 * highlight on the selected tile; and the explanatory text.
 *
 * <p>Once all reels settle, the off-center strip tiles and the slot chrome (black border,
 * separators, marker) fade out so only the three chosen tiles remain.</p>
 */
public final class ReelRenderer {

    private static final String INFO_TEXT =
        "All starters will have perfect IVs. The tile color indicates rarity, "
        + "derived from how high the Pokémon's base stat total is.";

    /** Real tier fill colors used to make filler tiles flash different rarities while spinning. */
    private static final int[] TIER_COLORS = {
        RarityTier.COMMON.getFillColorArgb(),
        RarityTier.UNCOMMON.getFillColorArgb(),
        RarityTier.RARE.getFillColorArgb(),
        RarityTier.EPIC.getFillColorArgb(),
        RarityTier.LEGENDARY.getFillColorArgb()
    };

    private static final int MARKER_COLOR = 0xFFFFE080;
    private static final int SELECT_COLOR = 0xFFFFF4B0;

    // ------------------------------------------------------------------
    // LOCKED SLOT-HOUSING GEOMETRY (proportional to the tile size T; at runtime T = 72).
    // The gambling_frame.png housing is drawn scaled to HOUSING_W x HOUSING_H, centered on the
    // reels; its three transparent 72x180 windows reveal the reel columns. Template values:
    //   overall housing      282 x 190
    //   reel window / tile    72 x 180
    //   column pitch         100  (center-to-center)
    // ------------------------------------------------------------------
    private static final float HOUSING_W_AT_72 = 282f;
    private static final float HOUSING_H_AT_72 = 190f;

    private ReelRenderer() {
    }

    /**
     * @param cx           per-reel center x (3 values)
     * @param cy           shared center y (the selection line)
     * @param tileSize     tile square size in px
     * @param clickedIndex currently-selected reel (0-2), or -1 for none
     */
    public static void render(GuiGraphics graphics, int[] cx, int cy, int tileSize,
                              int screenWidth, int screenHeight, int clickedIndex) {
        List<OfferSlot> slots = ClientOfferState.get();
        if (slots.size() != 3 || cx.length != 3) {
            return;
        }
        ReelSprites.ensureBuilt();

        int step = tileSize;
        int viewportHalf = (int) (tileSize * 1.25f);
        double reveal = ReelAnimation.revealAlpha();

        for (int i = 0; i < 3; i++) {
            drawReel(graphics, i, cx[i], cy, tileSize, step, viewportHalf, slots.get(i), reveal,
                clickedIndex == i);
        }

        // Decorative slot housing on top of the reels — its three transparent windows reveal
        // them. Fades out with the rest of the chrome once the reels settle.
        if (reveal > 0.01) {
            int housingW = Math.round(HOUSING_W_AT_72 * tileSize / 72f);
            int housingH = Math.round(HOUSING_H_AT_72 * tileSize / 72f);
            int housingLeft = (cx[0] + cx[2]) / 2 - housingW / 2;
            int housingTop = cy - housingH / 2;
            ScreenHelper.drawImageQuad(GMSTextures.GAMBLING_FRAME, graphics, housingLeft, housingTop,
                housingW, housingH, 0, 0, 1, 1, 1, 1, 1, (float) reveal, 0);
        }

        // The IV/rarity explainer shows only while nothing is selected.
        if (clickedIndex < 0) {
            drawInfoText(graphics, screenWidth, screenHeight);
        }
    }

    private static void drawReel(GuiGraphics graphics, int reel, int cx, int cy, int tileSize,
                                 int step, int viewportHalf, OfferSlot offer, double reveal, boolean selected) {
        double pos = ReelAnimation.position(reel);
        double speed = ReelAnimation.speed01(reel);
        boolean settled = ReelAnimation.settled(reel);
        int landing = ReelAnimation.LANDING[reel];

        // Glow behind a landed high-tier tile (drawn first so the tile fill sits on top of it).
        if (settled && isHighTier(offer.rarityTier())) {
            drawGlow(graphics, cx, cy, tileSize, offer.rarityTier().getFillColorArgb());
        }

        int half = tileSize / 2;
        graphics.enableScissor(cx - half - 2, cy - viewportHalf, cx + half + 2, cy + viewportHalf);
        int center = (int) Math.floor(pos);
        for (int k = center - 2; k <= center + 2; k++) {
            boolean isOffer = k == landing;
            // The landed (center) tile stays fully opaque; the strip tiles fade after settle.
            float tileAlpha = isOffer ? 1f : (float) reveal;
            if (!isOffer && tileAlpha <= 0.01f) {
                continue;
            }
            int yCenter = cy - (int) Math.round((pos - k) * step);
            ResourceLocation sprite = isOffer ? ReelSprites.offerSprite(reel) : ReelSprites.fillerSprite(hash(reel, k));
            int fill = isOffer ? offer.rarityTier().getFillColorArgb()
                : TIER_COLORS[Math.floorMod(hash(reel, k) * 7 + 3, TIER_COLORS.length)];
            drawTile(graphics, cx, yCenter, tileSize, fill, sprite, speed, tileAlpha);
        }
        graphics.disableScissor();

        // Center selection slot accent (fades with the chrome).
        if (reveal > 0.01) {
            drawMarkerSlot(graphics, cx, cy, tileSize, reveal);
        }

        // Highlight overlay on the currently-selected tile.
        if (selected) {
            drawSelectionHighlight(graphics, cx, cy, tileSize);
        }
    }

    /** Draws one tile: the rarity color filling the whole tile, with the sprite centered on it. */
    private static void drawTile(GuiGraphics graphics, int cx, int cyc, int size, int fillColor,
                                 ResourceLocation sprite, double speed, float alpha) {
        int x0 = Math.round(cx - size / 2f);
        int y0 = Math.round(cyc - size / 2f);

        // Rarity fill across the whole tile (this is now the tile — no separate frame texture).
        graphics.fill(x0, y0, x0 + size, y0 + size, applyAlpha(fillColor, alpha));

        // Sprite centered on the tile, leaving a thin margin of the rarity color as a border.
        if (sprite != null) {
            float sSide = size * 0.85f;
            float sx = cx - sSide / 2f;
            float sy = cyc - sSide / 2f;
            if (speed > 0.06) {
                int ghost = (int) (speed * size * 0.5f);
                float ga = (float) (0.22 * speed) * alpha;
                ScreenHelper.drawImageQuad(sprite, graphics, sx, sy - ghost, sSide, sSide, 0, 0, 1, 1, 1, 1, 1, ga, 0);
                ScreenHelper.drawImageQuad(sprite, graphics, sx, sy + ghost, sSide, sSide, 0, 0, 1, 1, 1, 1, 1, ga, 0);
            }
            float mainA = (float) (1.0 - 0.35 * speed) * alpha;
            ScreenHelper.drawImageQuad(sprite, graphics, sx, sy, sSide, sSide, 0, 0, 1, 1, 1, 1, 1, mainA, 0);
        }
    }

    private static void drawGlow(GuiGraphics graphics, int cx, int cy, int size, int tierColor) {
        long t = System.currentTimeMillis();
        float pulse = 0.35f + 0.25f * (float) Math.sin(t / 320.0);
        int a = (int) (pulse * 255) & 0xFF;
        int rgb = tierColor & 0x00FFFFFF;
        int g = size / 4;
        graphics.fill(cx - size / 2 - g, cy - size / 2 - g, cx + size / 2 + g, cy + size / 2 + g,
            rgb | ((a / 3) << 24));
        graphics.fill(cx - size / 2 - g / 2, cy - size / 2 - g / 2, cx + size / 2 + g / 2, cy + size / 2 + g / 2,
            rgb | ((a / 2) << 24));
    }

    /** Pulsing highlight ring + soft overlay on the selected tile. */
    private static void drawSelectionHighlight(GuiGraphics graphics, int cx, int cy, int size) {
        long t = System.currentTimeMillis();
        float pulse = 0.6f + 0.4f * (float) Math.sin(t / 250.0);
        int a = (int) (pulse * 255) & 0xFF;
        int ring = (SELECT_COLOR & 0x00FFFFFF) | (a << 24);
        int half = size / 2;
        drawOutline(graphics, cx - half - 1, cy - half - 1, cx + half + 1, cy + half + 1, 2, ring);
        // Faint inner sheen over the sprite.
        graphics.fill(cx - half + 2, cy - half + 2, cx + half - 2, cy + half - 2,
            (SELECT_COLOR & 0x00FFFFFF) | (0x22 << 24));
    }

    private static void drawMarkerSlot(GuiGraphics graphics, int cx, int cy, int size, double alpha) {
        int half = size / 2;
        int color = applyAlpha(MARKER_COLOR, alpha);
        graphics.fill(cx - half, cy - half - 2, cx + half, cy - half - 1, color);
        graphics.fill(cx - half, cy + half + 1, cx + half, cy + half + 2, color);
    }

    /** Draws a rectangular outline of the given thickness just outside [left,top,right,bottom]. */
    private static void drawOutline(GuiGraphics graphics, int left, int top, int right, int bottom, int t, int color) {
        graphics.fill(left - t, top - t, right + t, top, color);      // top
        graphics.fill(left - t, bottom, right + t, bottom + t, color); // bottom
        graphics.fill(left - t, top, left, bottom, color);            // left
        graphics.fill(right, top, right + t, bottom, color);          // right
    }

    private static void drawInfoText(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Font font = Minecraft.getInstance().font;
        int wrap = Math.min(320, (int) (screenWidth * 0.6f));
        List<FormattedCharSequence> lines = font.split(Component.literal(INFO_TEXT), wrap);
        int y = (int) (screenHeight * 0.12f);
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, screenWidth / 2, y, 0xFFFFFFFF);
            y += font.lineHeight + 1;
        }
    }

    private static boolean isHighTier(RarityTier tier) {
        return tier == RarityTier.EPIC || tier == RarityTier.LEGENDARY;
    }

    /** Multiplies a packed-ARGB color's alpha by {@code mult} (clamped). */
    private static int applyAlpha(int argb, double mult) {
        int a = (argb >>> 24) & 0xFF;
        a = (int) (a * mult);
        if (a < 0) {
            a = 0;
        } else if (a > 255) {
            a = 255;
        }
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    private static int hash(int reel, int k) {
        int h = (reel * 73856093) ^ (k * 19349663);
        return h & 0x7fffffff;
    }
}
