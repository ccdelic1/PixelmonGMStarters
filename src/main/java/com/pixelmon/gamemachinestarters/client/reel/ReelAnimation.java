package com.pixelmon.gamemachinestarters.client.reel;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot-machine timing for the three reels. Each reel travels a fixed number of tiles and
 * eases to a stop with a quintic ease-out (fast, sustained, then a sharp settle). The
 * reels stop left -&gt; middle -&gt; right because their durations increase.
 *
 * <p>Single starter screen at a time, so this is a static singleton keyed to the current
 * offer signature; it restarts whenever the offer changes.</p>
 */
public final class ReelAnimation {

    /** Tiles each reel travels before landing (larger = faster/longer spin). */
    static final int[] LANDING = {26, 34, 42};
    /** Spin duration per reel in ms (left stops first). */
    private static final long[] DURATION_MS = {1800, 2500, 3200};
    /** Small settle wobble after a reel stops, for the "click into place" feel. */
    private static final long SETTLE_WOBBLE_MS = 220;
    /** After all reels stop, how long the strip/chrome takes to fade away. */
    private static final long FADE_MS = 450;

    private static long startMillis = -1L;
    private static List<String> signature = List.of();

    private ReelAnimation() {
    }

    /** Starts (or restarts on a new offer) the animation clock. */
    public static void ensureStarted(List<String> offerNames) {
        if (!offerNames.equals(signature)) {
            signature = new ArrayList<>(offerNames);
            startMillis = System.currentTimeMillis();
        }
    }

    public static void reset() {
        startMillis = -1L;
        signature = List.of();
    }

    private static long elapsed() {
        return startMillis < 0 ? 0 : System.currentTimeMillis() - startMillis;
    }

    /** Fractional tile position (0..LANDING) of reel {@code i} right now. */
    public static double position(int i) {
        if (startMillis < 0) {
            return ReelAnimation.LANDING[i];
        }
        double x = clamp01(elapsed() / (double) DURATION_MS[i]);
        double eased = easeOutQuint(x);
        double base = LANDING[i] * eased;
        // Tiny damped wobble right after the stop for a tactile "tick".
        long over = elapsed() - DURATION_MS[i];
        if (over >= 0 && over < SETTLE_WOBBLE_MS) {
            double p = over / (double) SETTLE_WOBBLE_MS;
            base += Math.sin(p * Math.PI * 3) * 0.06 * (1 - p);
        }
        return base;
    }

    /** Normalized spin speed of reel {@code i} in ~[0,1], for motion blur. */
    public static double speed01(int i) {
        if (startMillis < 0) {
            return 0;
        }
        double x = clamp01(elapsed() / (double) DURATION_MS[i]);
        double d = Math.pow(1 - x, 4); // derivative of easeOutQuint, normalized
        return clamp01(d);
    }

    public static boolean settled(int i) {
        return startMillis >= 0 && elapsed() >= DURATION_MS[i];
    }

    public static boolean allSettled() {
        return settled(2);
    }

    /**
     * Alpha (1..0) for the transient slot-machine chrome — the off-center strip tiles, the
     * black slots border, separators and center marker. Stays 1 while any reel spins, then
     * fades to 0 once all reels have settled so only the three chosen tiles remain.
     */
    public static double revealAlpha() {
        if (startMillis < 0) {
            return 0;
        }
        long allSettledAt = DURATION_MS[2];
        long e = elapsed();
        if (e < allSettledAt) {
            return 1;
        }
        double f = (e - allSettledAt) / (double) FADE_MS;
        return f >= 1 ? 0 : 1 - f;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    private static double easeOutQuint(double x) {
        return 1 - Math.pow(1 - x, 5);
    }
}
