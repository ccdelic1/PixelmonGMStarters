package com.pixelmon.gamemachinestarters.client;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pixelmon.gamemachinestarters.net.OfferSlot;
import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.config.starter.Starter;
import com.pixelmonmod.pixelmon.config.starter.StarterList;

/**
 * Replaces the client-side {@link StarterList} contents with the three Game Machine
 * Starters offers so the vanilla starter screen renders them as its tiles and the
 * Ability Randomizer mod can read the selected Pokemon's ability from
 * {@code StarterList.getStarters().get(clickedIndex)}.
 *
 * <p>The client StarterList is populated once at login (SyncStartersPacket, configuration
 * phase) and is not re-synced during play, so a single injection stays stable. This class
 * is still idempotent and self-healing: it only rebuilds when the list no longer matches
 * the active offer.</p>
 */
public final class ClientStarterInjector {

    private static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");

    /**
     * Grid coordinates (Pixelmon's starter layout units) for a centered row of three tiles.
     * x = 3,7,11 spaces the reel columns ~100px apart; scale 2 gives an ~80x64 click hitbox
     * that comfortably covers the drawn tile. y = 3 centers the row vertically.
     */
    private static final int[] COLUMN_X = {3, 7, 11};
    private static final int ROW_Y = 3;
    private static final int TILE_SCALE = 2;

    /** Names last injected, to skip needless rebuilds. */
    private static List<String> lastInjected = List.of();

    private ClientStarterInjector() {
    }

    /** Ensures the client StarterList holds exactly the active offer, and blanks the welcome text. */
    public static void ensureInjected() {
        List<OfferSlot> slots = ClientOfferState.get();
        if (slots.size() != 3) {
            return; // no active offer yet — leave the list alone until one arrives
        }

        List<String> names = slots.stream().map(OfferSlot::name).toList();
        List<Starter> starters = StarterList.getStarters();
        if (starters.size() == 3 && names.equals(lastInjected)) {
            blankWelcomeText();
            return;
        }

        try {
            starters.clear();
            for (int i = 0; i < 3; i++) {
                starters.add(buildStarter(slots.get(i), COLUMN_X[i]));
            }
            lastInjected = names;
            blankWelcomeText();
        } catch (Exception e) {
            LOGGER.error("[GameMachineStarters] Failed to inject client starter offers: {}", names, e);
        }
    }

    private static Starter buildStarter(OfferSlot slot, int columnX) {
        String specString = slot.name()
            + (slot.ability() == null || slot.ability().isBlank() ? "" : " ability:" + slot.ability());
        PokemonSpecification spec;
        try {
            spec = PokemonSpecificationProxy.create(specString).get();
        } catch (Exception e) {
            // Fall back to the bare species so a bad ability token never drops a tile.
            spec = PokemonSpecificationProxy.create(slot.name()).get();
        }
        return new Starter(spec, 0, columnX, ROW_Y, TILE_SCALE);
    }

    /**
     * Blanks Pixelmon's two welcome-text lines at their source ({@code StarterList.getText()}
     * returns the live array), so the vanilla screen draws empty strings — no competing
     * {@code @ModifyArg} with the Ability Randomizer, which still swaps in its ability text
     * when a tile is selected.
     */
    private static void blankWelcomeText() {
        String[] text = StarterList.getText();
        if (text.length > 0) {
            text[0] = "";
        }
        if (text.length > 1) {
            text[1] = "";
        }
    }
}
