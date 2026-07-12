package com.pixelmon.gamemachinestarters.pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.config.starter.Starter;
import com.pixelmonmod.pixelmon.config.starter.StarterList;

/**
 * Runtime {@link SpeciesInfoProvider} backed by Pixelmon's species registry.
 * Instantiate only once Pixelmon's registries are populated (server start).
 */
public class PixelmonSpeciesProvider implements SpeciesInfoProvider {

    private static final Logger LOGGER = LogManager.getLogger("PixelmonGameMachineStarters");

    /**
     * Classic starter trios (gens 1-9), the fallback when Pixelmon's starter list is
     * unavailable and Whitelist Mode needs its vanilla-starters pool.
     */
    private static final List<String> CLASSIC_STARTERS = List.of(
        "Bulbasaur", "Charmander", "Squirtle",
        "Chikorita", "Cyndaquil", "Totodile",
        "Treecko", "Torchic", "Mudkip",
        "Turtwig", "Chimchar", "Piplup",
        "Snivy", "Tepig", "Oshawott",
        "Chespin", "Fennekin", "Froakie",
        "Rowlet", "Litten", "Popplio",
        "Grookey", "Scorbunny", "Sobble",
        "Sprigatito", "Fuecoco", "Quaxly"
    );

    /** Normalized name -> Species, built lazily from the registry. */
    private Map<String, Species> byNormalizedName;

    private Map<String, Species> index() {
        if (byNormalizedName == null) {
            byNormalizedName = new HashMap<>();
            for (Species species : PixelmonSpecies.getAll()) {
                byNormalizedName.put(Names.normalize(species.getName()), species);
            }
        }
        return byNormalizedName;
    }

    private Optional<Species> find(String name) {
        return Optional.ofNullable(index().get(Names.normalize(name)));
    }

    @Override
    public boolean exists(String name) {
        return find(name).isPresent();
    }

    @Override
    public List<String> allSpeciesNames() {
        List<String> names = new ArrayList<>();
        for (Species species : PixelmonSpecies.getAll()) {
            String name = species.getName();
            if ("MissingNo".equalsIgnoreCase(name) || Names.normalize(name).equals("missingno")) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    @Override
    public boolean isLegendary(String name) {
        return find(name).map(Species::isLegendary).orElse(false);
    }

    @Override
    public boolean isUltraBeast(String name) {
        return find(name).map(Species::isUltraBeast).orElse(false);
    }

    @Override
    public boolean isMythical(String name) {
        return find(name).map(Species::isMythical).orElse(false);
    }

    @Override
    public EvolutionStage stageOf(String name) {
        Optional<Species> species = find(name);
        if (species.isEmpty()) {
            return EvolutionStage.SINGLE;
        }
        int depth = preEvolutionDepth(species.get(), new HashSet<>());
        boolean evolves = !species.get().getDefaultForm().getEvolutions().isEmpty();
        if (depth == 0) {
            return evolves ? EvolutionStage.FIRST : EvolutionStage.SINGLE;
        }
        return depth == 1 ? EvolutionStage.SECOND : EvolutionStage.THIRD;
    }

    /**
     * Longest pre-evolution chain length below this species. Works whether Pixelmon's
     * preEvolutions field lists only the direct pre-evolution or the whole chain,
     * because it takes the max over recursive depths with a visited guard.
     */
    private int preEvolutionDepth(Species species, Set<Integer> visited) {
        if (!visited.add(species.getDex())) {
            return 0;
        }
        List<Species> pres = species.getDefaultForm().getPreEvolutions();
        int max = 0;
        for (Species pre : pres) {
            max = Math.max(max, 1 + preEvolutionDepth(pre, visited));
        }
        return max;
    }

    @Override
    public List<String> vanillaStarterNames() {
        List<String> names = new ArrayList<>();
        try {
            for (Starter starter : StarterList.getStarters()) {
                String spec = starter.spec().toString().trim();
                if (!spec.isEmpty()) {
                    names.add(spec.split("\\s+")[0]);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[GameMachineStarters] Could not read Pixelmon starter list, using classic starters", e);
        }
        if (names.isEmpty()) {
            names.addAll(CLASSIC_STARTERS);
        }
        return names;
    }
}
