package com.pixelmon.gamemachinestarters.pool;

/** Position of a species within its evolution line, for the All Pokemon mode filters. */
public enum EvolutionStage {
    /** No pre-evolutions and no evolutions (e.g. Kangaskhan, Lapras). */
    SINGLE,
    /** No pre-evolutions but evolves (e.g. Bulbasaur, Dratini). */
    FIRST,
    /** Exactly one pre-evolution step behind it (e.g. Ivysaur, Pikachu). */
    SECOND,
    /** Two or more pre-evolution steps behind it (e.g. Venusaur, Raichu via Pichu). */
    THIRD
}
