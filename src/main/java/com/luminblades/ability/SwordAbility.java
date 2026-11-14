package com.luminblades.ability;

import com.luminblades.sword.SwordSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Habilidade de uma espada: activate (botão direito) e onHit (golpe normal). */
@FunctionalInterface
public interface SwordAbility {
    void activate(ServerLevel level, ServerPlayer player, SwordSettings s);

    /** Efeito padrão de golpe: faíscas na cor da espada. */
    default void onHit(ServerLevel level, LivingEntity attacker, LivingEntity target, SwordSettings s) {
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Fx.burst(level, c, s, (int) (10 * s.density()), 0.25);
    }

    @FunctionalInterface
    interface HitHandler {
        void hit(ServerLevel level, LivingEntity attacker, LivingEntity target, SwordSettings s);
    }

    /** Compõe uma habilidade com um efeito extra no golpe (além das faíscas padrão). */
    static SwordAbility of(SwordAbility base, HitHandler extra) {
        return new SwordAbility() {
            @Override
            public void activate(ServerLevel level, ServerPlayer player, SwordSettings s) {
                base.activate(level, player, s);
            }

            @Override
            public void onHit(ServerLevel level, LivingEntity attacker, LivingEntity target, SwordSettings s) {
                SwordAbility.super.onHit(level, attacker, target, s);
                extra.hit(level, attacker, target, s);
            }
        };
    }
}
