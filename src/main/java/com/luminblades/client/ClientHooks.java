package com.luminblades.client;

import com.luminblades.client.anim.SwordAnimator;
import com.luminblades.client.cine.CinematicManager;
import com.luminblades.network.CinematicPacket;
import com.luminblades.vfx.VfxManager;

/** Ponte servidor/comum -> cliente (chamada só via DistExecutor). */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void onActivate() {
        SwordAnimator.pulseActivate();
    }

    public static void addCinematic(CinematicPacket m) {
        CinematicManager.add(m);
    }

    public static void addImpact(double x, double y, double z, float strength, int duration) {
        VfxManager.addImpact(x, y, z, strength, duration);
    }
}
