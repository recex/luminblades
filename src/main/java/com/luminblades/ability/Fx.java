package com.luminblades.ability;

import com.luminblades.network.DistortionPacket;
import com.luminblades.network.ModNetwork;
import com.luminblades.sword.SwordSettings;
import com.luminblades.vfx.CineType;
import com.luminblades.vfx.GlowParticleOptions;
import com.luminblades.network.CinematicPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Utilitários de efeitos e alvos usados pelas habilidades (lado servidor). */
public final class Fx {
    private Fx() {
    }

    // ---------- partículas ----------
    public static void glow(ServerLevel l, SwordSettings s, double x, double y, double z, double vx, double vy, double vz) {
        glowC(l, s.rgb(), (float) s.intensity(), x, y, z, vx, vy, vz);
    }

    /** Uma partícula com velocidade explícita (count = 0 => velocidade vem dos deltas). */
    public static void glowC(ServerLevel l, int rgb, float scale, double x, double y, double z, double vx, double vy, double vz) {
        l.sendParticles(GlowParticleOptions.of(rgb, scale), x, y, z, 0, vx, vy, vz, 1.0);
    }

    /** Explosão de {@code count} partículas em um único pacote. */
    public static void burst(ServerLevel l, Vec3 p, SwordSettings s, int count, double speed) {
        burstC(l, p, s.rgb(), (float) s.intensity(), count, speed);
    }

    public static void burstC(ServerLevel l, Vec3 p, int rgb, float scale, int count, double speed) {
        if (count <= 0) return;
        l.sendParticles(GlowParticleOptions.of(rgb, scale), p.x, p.y, p.z, count, 0.2, 0.2, 0.2, speed);
    }

    public static void ring(ServerLevel l, Vec3 c, double radius, int points, SwordSettings s, double yOff, double outSpeed) {
        for (int i = 0; i < points; i++) {
            double a = i * (Math.PI * 2) / points;
            double cx = Math.cos(a), sz = Math.sin(a);
            glow(l, s, c.x + cx * radius, c.y + yOff, c.z + sz * radius, cx * outSpeed, 0.01, sz * outSpeed);
        }
    }

    public static void line(ServerLevel l, Vec3 a, Vec3 b, SwordSettings s, double step) {
        int n = Math.max(1, (int) (a.distanceTo(b) / step));
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.lerp(b, i / (double) n);
            glow(l, s, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    /** Raio em zigue-zague entre dois pontos. */
    public static void jagged(ServerLevel l, Vec3 a, Vec3 b, SwordSettings s) {
        RandomSource r = l.random;
        Vec3 prev = a;
        int n = 8;
        for (int i = 1; i <= n; i++) {
            Vec3 p = a.lerp(b, i / (double) n);
            if (i < n) {
                p = p.add((r.nextDouble() - 0.5) * 0.7, (r.nextDouble() - 0.5) * 0.7, (r.nextDouble() - 0.5) * 0.7);
            }
            line(l, prev, p, s, 0.3);
            prev = p;
        }
    }

    public static void distort(ServerLevel l, Vec3 p, float strength, int duration) {
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(p.x, p.y, p.z, 64.0, l.dimension())),
                new DistortionPacket(p.x, p.y, p.z, strength, duration));
    }

    /** Efeito cinematográfico visível para jogadores num raio de 96 blocos. */
    public static void cine(ServerLevel l, CineType t, Vec3 p, Vec3 dir, int rgb, float size, int duration, float tint) {
        CinematicPacket pk = new CinematicPacket(t.ordinal(), p.x, p.y, p.z, (float) dir.x, (float) dir.y, (float) dir.z,
                rgb, size, duration, l.random.nextInt(), tint);
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(p.x, p.y, p.z, 96.0, l.dimension())), pk);
    }

    /** Overlay em tela cheia (olho de dragão / sol): só o portador vê. */
    public static void cineSelf(ServerPlayer sp, CineType t, int rgb, float size, int duration) {
        CinematicPacket pk = new CinematicPacket(t.ordinal(), sp.getX(), sp.getY(), sp.getZ(), 0f, 0f, 0f,
                rgb, size, duration, sp.getRandom().nextInt(), 0f);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), pk);
    }

    public static void sound(ServerLevel l, Vec3 p, SoundEvent ev, float vol, float pitch) {
        l.playSound(null, p.x, p.y, p.z, ev, SoundSource.PLAYERS, vol, pitch);
    }

    public static Vec3 randomUnit(RandomSource r) {
        Vec3 v = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
        return v.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : v.normalize();
    }

    // ---------- alvos ----------
    public static boolean canHit(Player p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e instanceof Player tp && !p.canHarmPlayer(tp)) return false;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return false;
        return !e.isAlliedTo(p);
    }

    public static List<LivingEntity> targets(ServerLevel l, Player p, Vec3 c, double r) {
        double r2 = r * r;
        return l.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                x -> canHit(p, x) && x.distanceToSqr(c) <= r2);
    }

    public static List<LivingEntity> onSegment(ServerLevel l, Player p, Vec3 a, Vec3 b, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(radius + 1), x -> canHit(p, x))) {
            if (e.getBoundingBox().inflate(radius).clip(a, b).isPresent()) out.add(e);
        }
        return out;
    }

    public static void hurt(ServerLevel l, Player p, LivingEntity e, double amount) {
        if (amount > 0) e.hurt(l.damageSources().indirectMagic(p, p), (float) amount);
    }

    public static Vec3 aimPoint(Player p, double max) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(max));
        BlockHitResult h = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return h.getType() == HitResult.Type.MISS ? end : h.getLocation();
    }
}
