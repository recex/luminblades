package com.luminblades.vfx;

/** Tipos de efeito cinematográfico (servidor escolhe, cliente renderiza). */
public enum CineType {
    GALAXY_SLASH,   // faixa cósmica 3D com textura de galáxia
    RUNE_CIRCLE,    // círculo mágico rúnico (chão ou vertical)
    RAINBOW_ARCS,   // fitas em arco-íris + confete triangular
    BLACK_HOLE,     // lente gravitacional + disco de acreção (pós-processamento)
    DRAGON_EYE,     // olho de dragão em tela cheia (só o portador)
    SUN;            // sol em tela cheia (só o portador)

    public static CineType byId(int id) {
        CineType[] v = values();
        return v[Math.floorMod(id, v.length)];
    }
}
