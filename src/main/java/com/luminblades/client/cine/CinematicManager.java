package com.luminblades.client.cine;

import com.luminblades.config.LuminConfig;
import com.luminblades.network.CinematicPacket;
import com.luminblades.vfx.CineType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/** Instâncias ativas de efeitos cinematográficos + estado de tela (lente e overlay) calculado por frame. */
public final class CinematicManager {
    public record Instance(CineType type, double x, double y, double z, float dx, float dy, float dz,
                           int rgb, float size, int duration, int seed, float tint, double start) {
    }

    private static final List<Instance> ACTIVE = new ArrayList<>();

    // estado de tela do frame (lido pelo BloomPostProcessor)
    public static float lensX, lensY, lensRadius, lensFade, lensR, lensG, lensB;
    public static float overlayMode, overlayAlpha, overlayProgress, overlayR, overlayG, overlayB;

    private CinematicManager() {
    }

    public static void add(CinematicPacket m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !LuminConfig.CINE_ENABLED.get()) return;
        CineType t = CineType.byId(m.type());
        boolean overlay = t == CineType.DRAGON_EYE || t == CineType.SUN;
        if (overlay && !LuminConfig.CINE_OVERLAYS.get()) return;
        if (ACTIVE.size() >= 16) ACTIVE.remove(0);
        ACTIVE.add(new Instance(t, m.x(), m.y(), m.z(), m.dx(), m.dy(), m.dz(), m.rgb(), m.size(),
                Math.max(4, m.duration()), m.seed(), m.tint(), mc.level.getGameTime()));
    }

    public static List<Instance> active() {
        return ACTIVE;
    }

    public static double now(float pt) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime() + pt;
    }

    public static float progress(Instance i, double now) {
        return (float) Mth.clamp((now - i.start()) / i.duration(), 0.0, 1.0);
    }

    public static float smooth(float a, float b, float x) {
        float t = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float envelope(float p) {
        return smooth(0f, 0.15f, p) * (1f - smooth(0.75f, 1f, p));
    }

    public static boolean screenActive() {
        return lensFade > 0.001f || overlayAlpha > 0.002f;
    }

    /** Projeta buracos negros para UV de tela e escolhe o overlay mais forte. Chamado no estágio de render do mundo. */
    public static void captureScreen(Matrix4f pose, Matrix4f proj, Vec3 cam, float pt) {
        lensFade = 0f;
        overlayAlpha = 0f;
        overlayMode = 0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ACTIVE.isEmpty()) return;
        double now = now(pt);
        ACTIVE.removeIf(i -> now - i.start() >= i.duration());
        float k = LuminConfig.CINE_INTENSITY.get().floatValue();

        for (Instance i : ACTIVE) {
            float p = progress(i, now);
            float env = Math.min(1f, envelope(p) * k);
            float r = ((i.rgb() >> 16) & 255) / 255f, g = ((i.rgb() >> 8) & 255) / 255f, b = (i.rgb() & 255) / 255f;
            switch (i.type()) {
                case BLACK_HOLE -> {
                    if (env <= lensFade) break;
                    Vector4f v = new Vector4f((float) (i.x() - cam.x), (float) (i.y() - cam.y), (float) (i.z() - cam.z), 1f);
                    v.mul(pose);
                    v.mul(proj);
                    if (v.w() <= 0.1f) break;
                    float rs = i.size() * proj.m11() / v.w() * 0.5f * (0.2f + 0.8f * smooth(0f, 0.3f, p));
                    rs = Math.min(rs, 0.6f);
                    if (rs < 0.004f) break;
                    lensX = (v.x() / v.w()) * 0.5f + 0.5f;
                    lensY = (v.y() / v.w()) * 0.5f + 0.5f;
                    lensRadius = rs;
                    lensFade = env;
                    lensR = Math.max(r, 0.25f);
                    lensG = Math.max(g, 0.25f);
                    lensB = Math.max(b, 0.25f);
                }
                case DRAGON_EYE, SUN -> {
                    if (env <= overlayAlpha) break;
                    overlayAlpha = env;
                    overlayMode = i.type() == CineType.DRAGON_EYE ? 1f : 2f;
                    overlayProgress = p;
                    overlayR = r;
                    overlayG = g;
                    overlayB = b;
                }
                default -> {
                }
            }
        }
    }

    public static void reset() {
        ACTIVE.clear();
        lensFade = 0f;
        overlayAlpha = 0f;
        overlayMode = 0f;
    }
}
