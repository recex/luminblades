package com.luminblades.vfx;

import com.luminblades.config.LuminConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Estado de VFX do cliente: impactos (distorção) e rastros de movimento (buffers reutilizados). */
public final class VfxManager {
    private VfxManager() {
    }

    // ---------------- impactos / distorção ----------------
    private record Impact(double x, double y, double z, float strength, int duration, double start) {
    }

    private static final List<Impact> IMPACTS = new ArrayList<>();

    /** Estado já projetado em tela para o frame atual (lido pelo BloomPostProcessor). */
    public static float impactX, impactY, impactRadius, impactStrength;

    public static void addImpact(double x, double y, double z, float strength, int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (!LuminConfig.DISTORTION_ENABLED.get() || mc.level == null) return;
        if (IMPACTS.size() >= 8) IMPACTS.remove(0);
        IMPACTS.add(new Impact(x, y, z, strength, Math.max(2, duration), mc.level.getGameTime()));
    }

    public static boolean hasImpact() {
        return impactStrength > 0.0005f;
    }

    /** Projeta o impacto mais forte para coordenadas UV de tela. Chamado no estágio de render do mundo. */
    public static void captureImpact(Matrix4f pose, Matrix4f proj, Vec3 cam, float pt) {
        impactStrength = 0f;
        Minecraft mc = Minecraft.getInstance();
        if (IMPACTS.isEmpty() || mc.level == null) return;
        double now = mc.level.getGameTime() + pt;
        IMPACTS.removeIf(i -> now - i.start() >= i.duration());
        Impact best = null;
        float bestS = 0f;
        float bestProg = 0f;
        for (Impact i : IMPACTS) {
            float prog = (float) ((now - i.start()) / i.duration());
            float str = i.strength() * (1f - prog) * (1f - prog);
            if (str > bestS) {
                bestS = str;
                best = i;
                bestProg = prog;
            }
        }
        if (best == null) return;
        Vector4f v = new Vector4f((float) (best.x() - cam.x), (float) (best.y() - cam.y), (float) (best.z() - cam.z), 1f);
        v.mul(pose);
        v.mul(proj);
        if (v.w() <= 0.05f) return;
        float nx = v.x() / v.w();
        float ny = v.y() / v.w();
        if (Math.abs(nx) > 1.6f || Math.abs(ny) > 1.6f) return;
        impactX = nx * 0.5f + 0.5f;
        impactY = ny * 0.5f + 0.5f;
        impactRadius = 0.04f + bestProg * 0.55f;
        impactStrength = bestS * 0.035f * LuminConfig.DISTORTION_STRENGTH.get().floatValue();
    }

    // ---------------- rastros ----------------
    /** Rastro em anel de tamanho fixo (sem alocações por frame). Cada amostra guarda base e ponta da lâmina. */
    public static final class Trail {
        public static final int CAP = 64;
        public final double[] bx = new double[CAP], by = new double[CAP], bz = new double[CAP];
        public final double[] tx = new double[CAP], ty = new double[CAP], tz = new double[CAP];
        public final double[] born = new double[CAP];
        public int start, count, rgb = 0xFFFFFF;

        public int idx(int i) {
            return (start + i) % CAP;
        }

        public void push(Vec3 base, Vec3 tip, double now, int rgb) {
            if (count > 0) {
                int last = idx(count - 1);
                double dx = tip.x - tx[last], dy = tip.y - ty[last], dz = tip.z - tz[last];
                if (dx * dx + dy * dy + dz * dz < 0.0009) return;
            }
            if (count == CAP) {
                start = (start + 1) % CAP;
                count--;
            }
            int i = idx(count);
            bx[i] = base.x;
            by[i] = base.y;
            bz[i] = base.z;
            tx[i] = tip.x;
            ty[i] = tip.y;
            tz[i] = tip.z;
            born[i] = now;
            this.rgb = rgb;
            count++;
        }

        public void prune(double now, double life) {
            while (count > 0 && now - born[start] > life) {
                start = (start + 1) % CAP;
                count--;
            }
        }
    }

    public static final Map<UUID, Trail> TRAILS = new HashMap<>();

    public static void reset() {
        IMPACTS.clear();
        TRAILS.clear();
        impactStrength = 0f;
    }
}
