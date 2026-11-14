package com.luminblades.ability;

import com.luminblades.sword.SwordSettings;
import com.luminblades.vfx.CineType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** As 14 habilidades. Cada uma é um método independente; parâmetros vêm de {@link SwordSettings}. */
public final class Abilities {
    private static final double TAU = Math.PI * 2;

    public static final SwordAbility AURORA_VORTEX = Abilities::auroraVortex;
    public static final SwordAbility FALLING_STAR = Abilities::fallingStar;
    public static final SwordAbility PRISMATIC_EDGE = Abilities::prismaticEdge;
    public static final SwordAbility LUNAR_TIDE = Abilities::lunarTide;
    public static final SwordAbility CRIMSON_NOVA = Abilities::crimsonNova;
    public static final SwordAbility ETHEREAL_VEIL = Abilities::etherealVeil;
    public static final SwordAbility SOLAR_FLAME = SwordAbility.of(Abilities::solarFlame,
            (l, a, t, s) -> t.setSecondsOnFire(3));
    public static final SwordAbility GLIMMER_FROST = SwordAbility.of(Abilities::glimmerFrost,
            (l, a, t, s) -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                t.setTicksFrozen(t.getTicksRequiredToFreeze());
            });
    public static final SwordAbility ARCANE_TEMPEST = Abilities::arcaneTempest;
    public static final SwordAbility LUMINOUS_ROOT = Abilities::luminousRoot;
    public static final SwordAbility ECLIPSE = SwordAbility.of(Abilities::eclipse,
            (l, a, t, s) -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0)));
    public static final SwordAbility WANDERING_COMET = Abilities::wanderingComet;
    public static final SwordAbility COSMIC_PULSE = Abilities::cosmicPulse;
    public static final SwordAbility RADIANT_PHOENIX = SwordAbility.of(Abilities::radiantPhoenix,
            (l, a, t, s) -> a.heal(1.0F));

    private Abilities() {
    }

    // 1. Aurora Vortex: vórtice de luz que puxa inimigos e implode no fim.
    private static void auroraVortex(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 c = Fx.aimPoint(p, 7.0).add(0, 1.0, 0);
        double R = s.size();
        Fx.sound(l, c, SoundEvents.BEACON_ACTIVATE, 1f, 1.6f);
        Fx.ring(l, c, R, 28, s, 0.0, -0.2);
        Fx.cine(l, CineType.RUNE_CIRCLE, c.subtract(0, 0.9, 0), new Vec3(0, 1, 0), s.rgb(), (float) (R * 1.4), s.duration(), 0f);
        Fx.cine(l, CineType.BLACK_HOLE, c.add(0, 1.2, 0), Vec3.ZERO, s.rgb(), (float) (R * 0.35), s.duration(), 0f);
        ServerScheduler.run(l, s.duration(), t -> {
            double prog = t / (double) s.duration();
            int arms = Math.max(1, (int) Math.round(3 * s.density()));
            for (int a = 0; a < arms; a++) {
                double base = t * 0.4 + a * TAU / arms;
                for (int k = 1; k <= 4; k++) {
                    double r = R * k / 4.0 * (1.0 - 0.4 * prog);
                    double ang = base + k * 0.7;
                    Fx.glow(l, s, c.x + Math.cos(ang) * r, c.y + k * 0.2, c.z + Math.sin(ang) * r,
                            -Math.sin(ang) * 0.25, 0.06, Math.cos(ang) * 0.25);
                }
            }
            for (LivingEntity e : Fx.targets(l, p, c, R)) {
                Vec3 pull = c.subtract(e.position()).normalize().scale(0.1 + 0.08 * s.intensity());
                e.setDeltaMovement(e.getDeltaMovement().scale(0.8).add(pull));
                e.hurtMarked = true;
            }
            if (t == s.duration() - 1) {
                for (LivingEntity e : Fx.targets(l, p, c, R)) Fx.hurt(l, p, e, s.power());
                Fx.burst(l, c, s, (int) (40 * s.density()), 0.4);
                Fx.distort(l, c, (float) s.intensity(), 14);
                Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.8f);
            }
            return true;
        });
    }

    // 2. Falling Star: investida com rastro estelar que fere quem atravessa.
    private static void fallingStar(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 dir = p.getLookAngle();
        double speed = s.size() / Math.max(1, s.duration());
        Set<LivingEntity> hit = new HashSet<>();
        Fx.sound(l, p.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 1.5f);
        Fx.cine(l, CineType.GALAXY_SLASH, p.position().add(dir.scale(s.size() * 0.5)).add(0, 1.0, 0), dir, s.rgb(),
                (float) (s.size() * 1.6), 24, 0f);
        ServerScheduler.run(l, s.duration(), t -> {
            if (!p.isAlive()) return false;
            p.setDeltaMovement(dir.scale(speed));
            p.hurtMarked = true;
            p.fallDistance = 0;
            Vec3 pos = p.position().add(0, 1.0, 0);
            Fx.burst(l, pos, s, (int) (6 * s.density()) + 2, 0.08);
            for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(1.2), x -> Fx.canHit(p, x))) {
                if (hit.add(e)) {
                    Fx.hurt(l, p, e, s.power());
                    Fx.burst(l, e.position().add(0, e.getBbHeight() * 0.5, 0), s, 20, 0.35);
                }
            }
            if (t == s.duration() - 1) {
                Fx.burst(l, pos, s, (int) (50 * s.density()), 0.5);
                Fx.distort(l, pos, 0.6f, 10);
            }
            return true;
        });
    }

    // 3. Prismatic Edge: três feixes que refratam em arco-íris.
    private static void prismaticEdge(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 eye = p.getEyePosition().add(0, -0.25, 0);
        Vec3 look = p.getLookAngle();
        Set<LivingEntity> hit = new HashSet<>();
        Fx.sound(l, eye, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.4f);
        Fx.cine(l, CineType.RAINBOW_ARCS, Fx.aimPoint(p, s.size() * 0.6), Vec3.ZERO, s.rgb(), (float) (s.size() * 0.3), 44, 0f);
        for (int i = -1; i <= 1; i++) {
            Vec3 dir = look.yRot((float) Math.toRadians(i * 14.0));
            Vec3 end = eye.add(dir.scale(s.size()));
            BlockHitResult h = l.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (h.getType() != HitResult.Type.MISS) end = h.getLocation();
            double len = Math.max(0.5, end.distanceTo(eye));
            for (double d = 0; d <= len; d += 0.4) {
                Vec3 pt = eye.add(dir.scale(d));
                float hue = (float) (((d / len) * 0.6 + i * 0.15 + 1.0) % 1.0);
                Fx.glowC(l, Mth.hsvToRgb(hue, 0.85f, 1f), (float) s.intensity(), pt.x, pt.y, pt.z,
                        dir.x * 0.05, dir.y * 0.05, dir.z * 0.05);
            }
            Fx.burstC(l, end, Mth.hsvToRgb((i + 1) / 3f, 0.8f, 1f), (float) s.intensity(), (int) (14 * s.density()), 0.3);
            for (LivingEntity e : Fx.onSegment(l, p, eye, end, 0.6)) {
                if (hit.add(e)) Fx.hurt(l, p, e, s.power());
            }
        }
        Fx.distort(l, eye.add(look.scale(2.0)), 0.4f, 8);
    }

    // 4. Lunar Tide: onda de luz em arco que empurra.
    private static void lunarTide(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 look = p.getLookAngle();
        Vec3 fwd = new Vec3(look.x, 0, look.z);
        fwd = fwd.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : fwd.normalize();
        final Vec3 f = fwd;
        Vec3 o = p.position().add(0, 1.0, 0);
        Set<LivingEntity> hit = new HashSet<>();
        Fx.sound(l, o, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 0.7f);
        Vec3 slashAxis = f.cross(new Vec3(0, 1, 0));
        Fx.cine(l, CineType.GALAXY_SLASH, o.add(f.scale(s.size() * 0.6)).add(0, 0.5, 0), slashAxis, s.rgb(),
                (float) (s.size() * 1.5), s.duration() + 12, 0f);
        ServerScheduler.run(l, s.duration(), t -> {
            double dist = (t + 1) / (double) s.duration() * s.size();
            int pts = (int) (22 * s.density()) + 6;
            for (int i = 0; i < pts; i++) {
                double a = Math.toRadians(-70 + 140.0 * i / (pts - 1));
                Vec3 d = f.yRot((float) a);
                Fx.glow(l, s, o.x + d.x * dist, o.y + Math.sin(i * 0.8 + t) * 0.15, o.z + d.z * dist,
                        d.x * 0.15, 0.02, d.z * 0.15);
            }
            for (LivingEntity e : Fx.targets(l, p, o, s.size() + 1.5)) {
                Vec3 flat = new Vec3(e.getX() - o.x, 0, e.getZ() - o.z);
                double d = flat.length();
                if (d < 0.01) continue;
                double ang = Math.acos(Mth.clamp(flat.normalize().dot(f), -1.0, 1.0));
                if (ang < Math.toRadians(75) && Math.abs(d - dist) < 1.3 && hit.add(e)) {
                    Fx.hurt(l, p, e, s.power());
                    Vec3 k = flat.normalize().scale(0.9);
                    e.push(k.x, 0.3, k.z);
                    e.hurtMarked = true;
                }
            }
            return true;
        });
    }

    // 5. Crimson Nova: carrega luz e detona em supernova.
    private static void crimsonNova(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 c = p.position().add(0, 1.0, 0);
        double R = s.size();
        Fx.sound(l, c, SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.7f);
        Fx.cine(l, CineType.RUNE_CIRCLE, c.subtract(0, 0.9, 0), new Vec3(0, 1, 0), s.rgb(), (float) (R * 1.5), s.duration(), 0f);
        ServerScheduler.run(l, s.duration(), t -> {
            if (!p.isAlive()) return false;
            double prog = t / (double) s.duration();
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, 3, false, false, false));
            int n = (int) (6 * s.density()) + 3;
            for (int i = 0; i < n; i++) {
                Vec3 d = Fx.randomUnit(l.random).scale(R * (1.0 - prog * 0.8));
                Vec3 pos = c.add(d);
                Fx.glow(l, s, pos.x, pos.y, pos.z, -d.x * 0.12, -d.y * 0.12, -d.z * 0.12);
            }
            if (t == s.duration() - 1) {
                for (LivingEntity e : Fx.targets(l, p, c, R)) {
                    double d = e.position().distanceTo(c);
                    Fx.hurt(l, p, e, s.power() * Math.max(0.3, 1.0 - d / (R * 1.25)));
                    Vec3 k = e.position().subtract(c).normalize().scale(1.1);
                    e.push(k.x, 0.5, k.z);
                    e.hurtMarked = true;
                }
                Fx.ring(l, c, R * 0.4, 28, s, 0, 0.35);
                Fx.ring(l, c, R * 0.7, 36, s, 0, 0.30);
                Fx.ring(l, c, R, 44, s, 0, 0.25);
                Fx.burst(l, c, s, (int) (120 * s.density()), 0.8);
                Fx.distort(l, c, (float) (1.6 * s.intensity()), 18);
                Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.3f);
                Fx.cineSelf(p, CineType.SUN, s.rgb(), 1f, 44);
            }
            return true;
        });
    }

    // 6. Ethereal Veil: salto curto com distorção.
    private static void etherealVeil(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 from = p.position();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 to = eye.add(look.scale(s.size()));
        BlockHitResult h = l.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = h.getType() == HitResult.Type.MISS ? to : h.getLocation().subtract(look.scale(0.6));
        Vec3 dest = end.subtract(0, p.getEyeHeight(), 0);
        AABB box = p.getBoundingBox();
        boolean ok = false;
        for (int i = 0; i < 5 && !ok; i++) {
            Vec3 d = dest.add(0, i * 0.5, 0);
            if (l.noCollision(p, box.move(d.subtract(from)))) {
                dest = d;
                ok = true;
            }
        }
        if (!ok) {
            Fx.sound(l, from, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 0.5f);
            return;
        }
        Fx.line(l, from.add(0, 1, 0), dest.add(0, 1, 0), s, 0.5);
        Fx.burst(l, from.add(0, 1, 0), s, (int) (40 * s.density()), 0.3);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        Fx.burst(l, dest.add(0, 1, 0), s, (int) (40 * s.density()), 0.3);
        Fx.distort(l, dest.add(0, 1, 0), (float) s.intensity(), 12);
        Fx.sound(l, dest, SoundEvents.ENDERMAN_TELEPORT, 1f, 1.5f);
        Fx.cineSelf(p, CineType.DRAGON_EYE, s.rgb(), 1f, 40);
    }

    // 7. Solar Flame: corte flamejante em leque com rastro persistente.
    private static void solarFlame(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 look = p.getLookAngle();
        Vec3 o = p.getEyePosition().add(0, -0.3, 0);
        double R = s.size();
        Fx.sound(l, o, SoundEvents.BLAZE_SHOOT, 1f, 0.8f);
        Fx.cineSelf(p, CineType.SUN, s.rgb(), 1f, 30);
        for (LivingEntity e : Fx.targets(l, p, o, R)) {
            Vec3 d = e.position().add(0, e.getBbHeight() / 2, 0).subtract(o);
            if (d.lengthSqr() > 1e-4 && d.normalize().dot(look) > Math.cos(Math.toRadians(50))) {
                Fx.hurt(l, p, e, s.power());
                e.setSecondsOnFire(4);
            }
        }
        ServerScheduler.run(l, s.duration(), t -> {
            double sw = Math.toRadians(-55 + 110.0 * t / Math.max(1, s.duration() - 1));
            Vec3 d = look.yRot((float) sw);
            for (double r = 1.0; r <= R; r += 0.6) {
                Vec3 pos = o.add(d.scale(r));
                Fx.glow(l, s, pos.x, pos.y, pos.z, 0, 0.05 + 0.01 * r, 0);
            }
            return true;
        });
    }

    // 8. Glimmer Frost: nuvem de cristais que desacelera e congela.
    private static void glimmerFrost(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 c = Fx.aimPoint(p, 8.0);
        double R = s.size();
        Fx.sound(l, c, SoundEvents.GLASS_BREAK, 1f, 1.8f);
        Fx.cine(l, CineType.RUNE_CIRCLE, c.add(0, 0.05, 0), new Vec3(0, 1, 0), s.rgb(), (float) (R * 1.4), Math.min(s.duration(), 80), 0f);
        Fx.ring(l, c.add(0, 0.2, 0), R, 30, s, 0, 0.25);
        ServerScheduler.run(l, s.duration(), t -> {
            int n = (int) (5 * s.density()) + 2;
            for (int i = 0; i < n; i++) {
                double a = l.random.nextDouble() * TAU;
                double r = Math.sqrt(l.random.nextDouble()) * R;
                Fx.glow(l, s, c.x + Math.cos(a) * r, c.y + 0.2 + l.random.nextDouble() * 2.2, c.z + Math.sin(a) * r, 0, -0.03, 0);
            }
            if (t % 10 == 0) {
                for (LivingEntity e : Fx.targets(l, p, c, R)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 20, e.getTicksFrozen() + 40));
                    if (t % 20 == 0) Fx.hurt(l, p, e, s.power());
                }
            }
            return true;
        });
    }

    // 9. Arcane Tempest: raios em cadeia entre alvos.
    private static void arcaneTempest(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 origin = eye.add(0, -0.3, 0).add(look.scale(0.6));
        Fx.cine(l, CineType.RUNE_CIRCLE, eye.add(look.scale(4.0)), look, s.rgb(), 2.6f, 28, 0f);
        LivingEntity first = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : Fx.targets(l, p, eye, s.size())) {
            Vec3 d = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double dist = d.length();
            if (dist > 0.1 && d.normalize().dot(look) > 0.93 && dist < best && p.hasLineOfSight(e)) {
                best = dist;
                first = e;
            }
        }
        if (first == null) {
            Fx.jagged(l, origin, eye.add(look.scale(s.size() * 0.6)), s);
            Fx.sound(l, origin, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.3f, 2.0f);
            return;
        }
        List<LivingEntity> chain = new ArrayList<>();
        chain.add(first);
        LivingEntity cur = first;
        for (int i = 0; i < 4; i++) {
            LivingEntity next = null;
            double nd = Double.MAX_VALUE;
            for (LivingEntity c : Fx.targets(l, p, cur.position(), s.size() * 0.4)) {
                if (chain.contains(c)) continue;
                double d = c.distanceToSqr(cur);
                if (d < nd) {
                    nd = d;
                    next = c;
                }
            }
            if (next == null) break;
            chain.add(next);
            cur = next;
        }
        ServerScheduler.run(l, chain.size() * 3, t -> {
            if (t % 3 != 0) return true;
            int i = t / 3;
            LivingEntity tgt = chain.get(i);
            Vec3 from = i == 0 ? origin : chain.get(i - 1).position().add(0, chain.get(i - 1).getBbHeight() / 2, 0);
            Vec3 to = tgt.position().add(0, tgt.getBbHeight() / 2, 0);
            Fx.jagged(l, from, to, s);
            Fx.hurt(l, p, tgt, s.power() * Math.pow(0.8, i));
            Fx.burst(l, to, s, 25, 0.4);
            Fx.sound(l, to, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4f, 1.8f);
            if (i == chain.size() - 1) Fx.distort(l, to, 0.5f, 8);
            return true;
        });
    }

    // 10. Luminous Root: raízes de luz que prendem inimigos no lugar.
    private static void luminousRoot(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 c = Fx.aimPoint(p, 8.0);
        double R = s.size();
        List<LivingEntity> rooted = Fx.targets(l, p, c, R);
        Fx.sound(l, c, SoundEvents.MOSS_PLACE, 1.5f, 0.6f);
        Fx.cine(l, CineType.RUNE_CIRCLE, c.add(0, 0.05, 0), new Vec3(0, 1, 0), s.rgb(), (float) (R * 1.4), s.duration(), 0f);
        for (LivingEntity e : rooted) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, s.duration(), 6));
            Fx.hurt(l, p, e, s.power());
        }
        ServerScheduler.run(l, s.duration(), t -> {
            for (LivingEntity e : rooted) {
                if (!e.isAlive()) continue;
                Vec3 m = e.getDeltaMovement();
                e.setDeltaMovement(0, Math.min(m.y, 0), 0);
                e.hurtMarked = true;
                double h = (t % 20) / 20.0 * e.getBbHeight();
                Fx.glow(l, s, e.getX() + (l.random.nextDouble() - 0.5), e.getY() + h, e.getZ() + (l.random.nextDouble() - 0.5), 0, 0.05, 0);
            }
            int n = (int) (4 * s.density()) + 2;
            for (int i = 0; i < n; i++) {
                double a = l.random.nextDouble() * TAU;
                double r = Math.sqrt(l.random.nextDouble()) * R;
                for (int h = 0; h < 4; h++) {
                    Fx.glow(l, s, c.x + Math.cos(a) * r, c.y + h * 0.4, c.z + Math.sin(a) * r, 0, 0.08, 0);
                }
            }
            return true;
        });
    }

    // 11. Eclipse: área de sombra com corona dourada.
    private static void eclipse(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 c = Fx.aimPoint(p, 8.0).add(0, 0.1, 0);
        double R = s.size();
        DustParticleOptions dark = new DustParticleOptions(new Vector3f(0.03f, 0.02f, 0.06f), 2.2f);
        Fx.sound(l, c, SoundEvents.WITHER_SPAWN, 0.4f, 1.8f);
        Fx.cine(l, CineType.BLACK_HOLE, c.add(0, R * 0.5 + 1.5, 0), Vec3.ZERO, s.rgb(), (float) (R * 0.25), s.duration(), 0f);
        Fx.cineSelf(p, CineType.DRAGON_EYE, s.rgb(), 1f, 50);
        ServerScheduler.run(l, s.duration(), t -> {
            double rad = R * Math.min(1.0, t / 10.0);
            int pts = (int) (36 * s.density()) + 8;
            for (int i = 0; i < pts; i++) {
                double a = i * TAU / pts + t * 0.05;
                Fx.glow(l, s, c.x + Math.cos(a) * rad, c.y + 0.15 + Math.sin(a * 3 + t * 0.3) * 0.1, c.z + Math.sin(a) * rad, 0, 0.04, 0);
            }
            l.sendParticles(dark, c.x, c.y + 1.2, c.z, 6, rad * 0.5, 0.6, rad * 0.5, 0.0);
            if (t % 10 == 0) {
                for (LivingEntity e : Fx.targets(l, p, c, R)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    if (t % 20 == 0) Fx.hurt(l, p, e, s.power());
                }
            }
            return true;
        });
    }

    // 12. Wandering Comet: projétil teleguiado em espiral.
    private static void wanderingComet(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3[] pos = {p.getEyePosition().add(p.getLookAngle()).add(0, -0.3, 0)};
        Vec3[] vel = {p.getLookAngle().scale(0.95)};
        Fx.sound(l, pos[0], SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
        ServerScheduler.run(l, s.duration(), t -> {
            LivingEntity tgt = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : Fx.targets(l, p, pos[0], 12.0)) {
                Vec3 d = e.position().add(0, e.getBbHeight() / 2, 0).subtract(pos[0]);
                double dist = d.length();
                if (dist > 0.1 && d.normalize().dot(vel[0].normalize()) > 0.5 && dist < best) {
                    best = dist;
                    tgt = e;
                }
            }
            if (tgt != null) {
                Vec3 want = tgt.position().add(0, tgt.getBbHeight() / 2, 0).subtract(pos[0]).normalize();
                vel[0] = vel[0].normalize().scale(0.8).add(want.scale(0.2)).normalize().scale(0.95);
            }
            Vec3 next = pos[0].add(vel[0]);
            BlockHitResult h = l.clip(new ClipContext(pos[0], next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            boolean boom = h.getType() != HitResult.Type.MISS;
            if (boom) next = h.getLocation();
            Vec3 fw = vel[0].normalize();
            Vec3 a = fw.cross(new Vec3(0, 1, 0));
            a = a.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : a.normalize();
            Vec3 b = fw.cross(a).normalize();
            for (int k = 0; k < 3; k++) {
                double ang = t * 0.8 + k * TAU / 3;
                Vec3 o = a.scale(Math.cos(ang) * 0.4).add(b.scale(Math.sin(ang) * 0.4));
                Fx.glow(l, s, next.x + o.x, next.y + o.y, next.z + o.z, -fw.x * 0.05, -fw.y * 0.05, -fw.z * 0.05);
            }
            Fx.burst(l, next, s, 3, 0.02);
            pos[0] = next;
            if (!boom) boom = !l.getEntitiesOfClass(LivingEntity.class, new AABB(next, next).inflate(0.7), x -> Fx.canHit(p, x)).isEmpty();
            if (boom || t == s.duration() - 1) {
                for (LivingEntity e : Fx.targets(l, p, next, s.size())) {
                    Fx.hurt(l, p, e, s.power());
                    Vec3 k = e.position().subtract(next).normalize().scale(0.6);
                    e.push(k.x, 0.3, k.z);
                    e.hurtMarked = true;
                }
                Fx.burst(l, next, s, (int) (70 * s.density()), 0.6);
                Fx.ring(l, next, s.size() * 0.6, 24, s, 0, 0.3);
                Fx.distort(l, next, (float) s.intensity(), 12);
                Fx.sound(l, next, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.6f);
                Fx.cine(l, CineType.RAINBOW_ARCS, next, Vec3.ZERO, s.rgb(), (float) (s.size() * 0.8), 44, 0f);
                return false;
            }
            return true;
        });
    }

    // 13. Cosmic Pulse: três ondas de choque consecutivas com distorção do espaço.
    private static void cosmicPulse(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 c = p.position().add(0, 0.3, 0);
        Vec3 lookFlat = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
        lookFlat = lookFlat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : lookFlat.normalize();
        Fx.cine(l, CineType.BLACK_HOLE, c.add(lookFlat.scale(6.0)).add(0, 2.5, 0), Vec3.ZERO, s.rgb(), (float) (s.size() * 0.25), s.duration(), 0f);
        Fx.cine(l, CineType.RUNE_CIRCLE, c.subtract(0, 0.25, 0), new Vec3(0, 1, 0), s.rgb(), (float) (s.size() * 1.2), s.duration(), 0f);
        List<Set<LivingEntity>> hits = List.of(new HashSet<>(), new HashSet<>(), new HashSet<>());
        ServerScheduler.run(l, s.duration(), t -> {
            for (int w = 0; w < 3; w++) {
                int age = t - w * 8;
                if (age < 0 || age >= 14) continue;
                if (age == 0) {
                    Fx.distort(l, c.add(0, 0.7, 0), (float) (1.3 * s.intensity()), 14);
                    Fx.sound(l, c, SoundEvents.WARDEN_SONIC_BOOM, 0.5f, 1.6f);
                }
                double r = (age + 1) / 14.0 * s.size();
                Fx.ring(l, c, r, (int) (30 * s.density()) + 12, s, 0.1, 0.12);
                for (LivingEntity e : Fx.targets(l, p, c, s.size() + 1.5)) {
                    Vec3 flat = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z);
                    double d = flat.length();
                    if (d < 0.01) continue;
                    if (Math.abs(d - r) < 1.4 && hits.get(w).add(e)) {
                        Fx.hurt(l, p, e, s.power() / 3.0);
                        Vec3 k = flat.normalize().scale(0.8);
                        e.push(k.x, 0.35, k.z);
                        e.hurtMarked = true;
                    }
                }
            }
            return true;
        });
    }

    // 14. Radiant Phoenix: cura o portador, queima ao redor e abre asas de luz.
    private static void radiantPhoenix(ServerLevel l, ServerPlayer p, SwordSettings s) {
        Vec3 c = p.position().add(0, 1.0, 0);
        double R = s.size();
        p.heal((float) s.power());
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, s.duration() + 60, 0, false, true, true));
        for (LivingEntity e : Fx.targets(l, p, c, R)) {
            Fx.hurt(l, p, e, s.power() * 0.5);
            e.setSecondsOnFire(4);
            Vec3 k = e.position().subtract(c).normalize().scale(0.7);
            e.push(k.x, 0.3, k.z);
            e.hurtMarked = true;
        }
        Fx.ring(l, p.position().add(0, 0.2, 0), R, 36, s, 0, 0.35);
        Fx.burst(l, c, s, (int) (60 * s.density()), 0.5);
        Fx.distort(l, c, (float) s.intensity(), 14);
        Fx.sound(l, c, SoundEvents.FIRECHARGE_USE, 1.2f, 0.7f);
        Fx.cine(l, CineType.RAINBOW_ARCS, p.position().add(0, 0.3, 0), Vec3.ZERO, s.rgb(), (float) (R * 0.5), 44, 0.85f);
        Fx.cineSelf(p, CineType.SUN, s.rgb(), 1f, 36);
        ServerScheduler.run(l, s.duration(), t -> {
            if (!p.isAlive()) return false;
            Vec3 base = p.position().add(0, 1.3, 0);
            Vec3 right = Vec3.directionFromRotation(0, p.getYRot() + 90);
            Vec3 back = Vec3.directionFromRotation(0, p.getYRot() + 180);
            double flap = Math.sin(t * 0.35) * 0.35 + 0.65;
            int n = (int) (10 * s.density()) + 3;
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < n; i++) {
                    double u = i / (double) (n - 1);
                    Vec3 pt = base.add(right.scale(side * (0.4 + u * 2.4))).add(back.scale(0.3 + u * 0.6))
                            .add(0, Math.sin(u * Math.PI) * 1.4 * flap, 0);
                    Fx.glow(l, s, pt.x, pt.y, pt.z, 0, 0.03, 0);
                }
            }
            return true;
        });
    }
}
