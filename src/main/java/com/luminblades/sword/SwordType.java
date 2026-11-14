package com.luminblades.sword;

import com.luminblades.ability.Abilities;
import com.luminblades.ability.SwordAbility;

/** Catálogo das 14 espadas: id, cor padrão, parâmetros padrão e habilidade. */
public enum SwordType {
    AURORA_VORTEX("aurora_vortex", "#33E6FF", d(1.0, 50, 6.0, 1.0, 240, 8.0), Abilities.AURORA_VORTEX),
    FALLING_STAR("falling_star", "#FFD24D", d(1.0, 6, 8.0, 1.0, 120, 7.0), Abilities.FALLING_STAR),
    PRISMATIC_EDGE("prismatic_edge", "#F2E6FF", d(1.0, 1, 12.0, 1.0, 160, 9.0), Abilities.PRISMATIC_EDGE),
    LUNAR_TIDE("lunar_tide", "#8FB8FF", d(1.0, 14, 9.0, 1.0, 140, 7.0), Abilities.LUNAR_TIDE),
    CRIMSON_NOVA("crimson_nova", "#FF2E4D", d(1.2, 30, 6.0, 1.0, 400, 14.0), Abilities.CRIMSON_NOVA),
    ETHEREAL_VEIL("ethereal_veil", "#A64DFF", d(1.0, 1, 10.0, 1.0, 100, 0.0), Abilities.ETHEREAL_VEIL),
    SOLAR_FLAME("solar_flame", "#FF7A1A", d(1.0, 16, 7.0, 1.0, 120, 8.0), Abilities.SOLAR_FLAME),
    GLIMMER_FROST("glimmer_frost", "#CFF6FF", d(1.0, 100, 5.0, 1.0, 300, 4.0), Abilities.GLIMMER_FROST),
    ARCANE_TEMPEST("arcane_tempest", "#4D7CFF", d(1.2, 12, 14.0, 1.0, 160, 7.0), Abilities.ARCANE_TEMPEST),
    LUMINOUS_ROOT("luminous_root", "#3DFF9A", d(1.0, 80, 4.5, 1.0, 260, 3.0), Abilities.LUMINOUS_ROOT),
    ECLIPSE("eclipse", "#FFB800", d(1.0, 120, 5.5, 1.0, 500, 3.0), Abilities.ECLIPSE),
    WANDERING_COMET("wandering_comet", "#FF5EC8", d(1.0, 50, 4.0, 1.0, 140, 12.0), Abilities.WANDERING_COMET),
    COSMIC_PULSE("cosmic_pulse", "#E633FF", d(1.2, 36, 9.0, 1.0, 320, 9.0), Abilities.COSMIC_PULSE),
    RADIANT_PHOENIX("radiant_phoenix", "#FFA31A", d(1.0, 40, 5.0, 1.0, 600, 10.0), Abilities.RADIANT_PHOENIX);

    public final String id;
    public final String defaultColor;
    public final Defaults defaults;
    public final SwordAbility ability;

    SwordType(String id, String defaultColor, Defaults defaults, SwordAbility ability) {
        this.id = id;
        this.defaultColor = defaultColor;
        this.defaults = defaults;
        this.ability = ability;
    }

    public record Defaults(double intensity, int duration, double size, double density, int cooldown, double power) {
    }

    private static Defaults d(double intensity, int duration, double size, double density, int cooldown, double power) {
        return new Defaults(intensity, duration, size, density, cooldown, power);
    }
}
