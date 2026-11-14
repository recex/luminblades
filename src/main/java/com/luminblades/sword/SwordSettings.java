package com.luminblades.sword;

/** Parâmetros efetivos (já lidos da config) de uma espada. */
public record SwordSettings(int rgb, double intensity, int duration, double size,
                            double density, int cooldown, double power) {
}
