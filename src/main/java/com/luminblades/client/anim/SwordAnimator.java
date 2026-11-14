package com.luminblades.client.anim;

import com.luminblades.sword.LuminSwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Máquina de animação do cliente (suavização exponencial + interpolação por partialTick).
 * glow: ativa/desativa a aura (cai a ~45% durante a recarga); equip: entrada da espada;
 * attack/activate: pulsos que decaem após o golpe/habilidade.
 */
public final class SwordAnimator {
    private static float glow, prevGlow, attack, prevAttack, activate, prevActivate, equip, prevEquip;
    private static boolean wasSwinging;

    private SwordAnimator() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            reset();
            return;
        }
        if (mc.isPaused()) return;
        prevGlow = glow;
        prevAttack = attack;
        prevActivate = activate;
        prevEquip = equip;

        ItemStack held = p.getMainHandItem();
        boolean holding = held.getItem() instanceof LuminSwordItem;
        boolean cooling = holding && p.getCooldowns().isOnCooldown(held.getItem());
        float target = holding ? (cooling ? 0.45f : 1f) : 0f;
        glow += (target - glow) * 0.16f;
        if (Math.abs(target - glow) < 0.003f) glow = target;
        equip = holding ? equip + (1f - equip) * 0.2f : 0f;
        attack *= 0.75f;
        activate *= 0.9f;
        if (holding && p.swinging && !wasSwinging) attack = 1f;
        wasSwinging = p.swinging;
    }

    public static void pulseActivate() {
        activate = 1f;
    }

    public static float attack(float pt) {
        return Mth.lerp(pt, prevAttack, attack);
    }

    public static float activate(float pt) {
        return Mth.lerp(pt, prevActivate, activate);
    }

    public static float equip(float pt) {
        float e = Mth.lerp(pt, prevEquip, equip);
        return e * e * (3f - 2f * e);
    }

    /** Fator de brilho (aura + bloom) com pulsos de ataque/ativação. */
    public static float glowFactor(float pt) {
        float g = Mth.lerp(pt, prevGlow, glow);
        return Math.min(2.2f, g * (1f + 0.9f * attack(pt) + 1.1f * activate(pt)));
    }

    public static void reset() {
        glow = prevGlow = attack = prevAttack = activate = prevActivate = equip = prevEquip = 0f;
        wasSwinging = false;
    }
}
