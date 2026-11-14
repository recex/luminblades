package com.luminblades.client.cine;

import com.luminblades.client.ClientModEvents;
import com.luminblades.client.cine.CinematicManager.Instance;
import com.luminblades.config.LuminConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/** Efeitos 3D no mundo: faixa de galáxia, círculo rúnico e fitas arco-íris (+ estilhaços/confete). */
public final class CinematicRenderer {
    private CinematicRenderer() {
    }

    public static void render(PoseStack ps, Camera cam, float pt, Frustum frustum) {
        if (CinematicManager.active().isEmpty() || !LuminConfig.CINE_ENABLED.get()) return;
        double now = CinematicManager.now(pt);
        Vec3 camPos = cam.getPosition();
        Matrix4f m = ps.last().pose();
        float time = (float) (now / 20.0);
        float k = LuminConfig.CINE_INTENSITY.get().floatValue();
        double cull = LuminConfig.CULL_DISTANCE.get() * 2.0;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        for (Instance i : CinematicManager.active()) {
            Vec3 c = new Vec3(i.x(), i.y(), i.z());
            if (c.distanceToSqr(camPos) > cull * cull) continue;
            if (!frustum.isVisible(new AABB(c, c).inflate(i.size() * 1.6))) continue;
            float p = CinematicManager.progress(i, now);
            switch (i.type()) {
                case GALAXY_SLASH -> {
                    slash(m, camPos, i, c, p, time, k);
                    shards(m, cam, camPos, i, c, p, 18, false, false, i.size() * 0.6, k);
                }
                case RUNE_CIRCLE -> rune(m, camPos, i, c, p, time, k);
                case RAINBOW_ARCS -> {
                    arcs(m, camPos, i, c, p, time);
                    shards(m, cam, camPos, i, c, p, 48, true, true, i.size() * 1.4, k);
                }
                default -> {
                }
            }
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void v(BufferBuilder bb, Matrix4f m, Vec3 cam, Vec3 p, float u, float vv, float r, float g, float b, float a) {
        bb.vertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z)).uv(u, vv).color(r, g, b, a).endVertex();
    }

    private static float cr(int rgb) { return ((rgb >> 16) & 255) / 255f; }
    private static float cg(int rgb) { return ((rgb >> 8) & 255) / 255f; }
    private static float cb(int rgb) { return (rgb & 255) / 255f; }

    // ---- faixa de galáxia (sempre voltada para a câmera em torno do eixo) ----
    private static void slash(Matrix4f m, Vec3 cam, Instance i, Vec3 c, float p, float time, float k) {
        ShaderInstance sh = ClientModEvents.galaxyShader;
        if (sh == null) return;
        Vec3 axis = new Vec3(i.dx(), i.dy(), i.dz());
        axis = axis.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : axis.normalize();
        Vec3 side = axis.cross(c.subtract(cam));
        side = side.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : side.normalize();
        double half = i.size();
        double w = i.size() * 0.11;
        Vec3 a0 = c.subtract(axis.scale(half)), a1 = c.add(axis.scale(half));
        RenderSystem.setShader(() -> sh);
        sh.safeGetUniform("Time").set(time);
        sh.safeGetUniform("Progress").set(p);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float r = cr(i.rgb()), g = cg(i.rgb()), b = cb(i.rgb());
        v(bb, m, cam, a0.subtract(side.scale(w)), 0f, 0f, r, g, b, k);
        v(bb, m, cam, a1.subtract(side.scale(w)), 1f, 0f, r, g, b, k);
        v(bb, m, cam, a1.add(side.scale(w)), 1f, 1f, r, g, b, k);
        v(bb, m, cam, a0.add(side.scale(w)), 0f, 1f, r, g, b, k);
        BufferUploader.drawWithShader(bb.end());
    }

    // ---- círculo rúnico (plano definido pela normal) ----
    private static void rune(Matrix4f m, Vec3 cam, Instance i, Vec3 c, float p, float time, float k) {
        ShaderInstance sh = ClientModEvents.runeShader;
        if (sh == null) return;
        Vec3 n = new Vec3(i.dx(), i.dy(), i.dz());
        n = n.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : n.normalize();
        Vec3 ref = Math.abs(n.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = n.cross(ref).normalize();
        Vec3 w = n.cross(u).normalize();
        Vec3 cc = c.add(n.scale(0.03));
        double R = i.size();
        RenderSystem.setShader(() -> sh);
        sh.safeGetUniform("Time").set(time);
        sh.safeGetUniform("Progress").set(p);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float r = cr(i.rgb()), g = cg(i.rgb()), b = cb(i.rgb());
        v(bb, m, cam, cc.subtract(u.scale(R)).subtract(w.scale(R)), 0f, 0f, r, g, b, k);
        v(bb, m, cam, cc.add(u.scale(R)).subtract(w.scale(R)), 1f, 0f, r, g, b, k);
        v(bb, m, cam, cc.add(u.scale(R)).add(w.scale(R)), 1f, 1f, r, g, b, k);
        v(bb, m, cam, cc.subtract(u.scale(R)).add(w.scale(R)), 0f, 1f, r, g, b, k);
        BufferUploader.drawWithShader(bb.end());
    }

    // ---- fitas arco-íris: 5 arcos em cúpula + anel no chão ----
    private static void arcs(Matrix4f m, Vec3 cam, Instance i, Vec3 c, float p, float time) {
        ShaderInstance sh = ClientModEvents.rainbowShader;
        if (sh == null) return;
        RenderSystem.setShader(() -> sh);
        sh.safeGetUniform("Time").set(time);
        sh.safeGetUniform("Progress").set(p);
        sh.safeGetUniform("Tint").set(i.tint());
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float r = cr(i.rgb()), g = cg(i.rgb()), b = cb(i.rgb());
        double R = i.size();
        int K = 5, N = 28;
        double yaw0 = (i.seed() & 255) / 255.0 * Math.PI;
        Vec3 up = new Vec3(0, 1, 0);
        for (int a = 0; a < K; a++) {
            double yaw = yaw0 + a * Math.PI / K;
            Vec3 horiz = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            Vec3 nrm = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)).scale(R * 0.07);
            double rad = R * (0.7 + 0.1 * a);
            float hue = a / (float) K;
            for (int s = 0; s < N; s++) {
                double f0 = Math.PI * s / N, f1 = Math.PI * (s + 1) / N;
                Vec3 p0 = c.add(horiz.scale(Math.cos(f0) * rad)).add(up.scale(Math.sin(f0) * rad));
                Vec3 p1 = c.add(horiz.scale(Math.cos(f1) * rad)).add(up.scale(Math.sin(f1) * rad));
                float u0 = s / (float) N, u1 = (s + 1) / (float) N;
                v(bb, m, cam, p0.subtract(nrm), u0, 0f, r, g, b, hue);
                v(bb, m, cam, p1.subtract(nrm), u1, 0f, r, g, b, hue);
                v(bb, m, cam, p1.add(nrm), u1, 1f, r, g, b, hue);
                v(bb, m, cam, p0.add(nrm), u0, 1f, r, g, b, hue);
            }
        }
        // anel horizontal no chão
        int M = 48;
        double ringR = R * 1.25, hw = R * 0.05;
        for (int s = 0; s < M; s++) {
            double f0 = Math.PI * 2 * s / M, f1 = Math.PI * 2 * (s + 1) / M;
            Vec3 d0 = new Vec3(Math.cos(f0), 0, Math.sin(f0)), d1 = new Vec3(Math.cos(f1), 0, Math.sin(f1));
            Vec3 base = c.add(0, 0.05, 0);
            float u0 = s / (float) M, u1 = (s + 1) / (float) M;
            v(bb, m, cam, base.add(d0.scale(ringR - hw)), u0, 0f, r, g, b, 0.3f);
            v(bb, m, cam, base.add(d1.scale(ringR - hw)), u1, 0f, r, g, b, 0.3f);
            v(bb, m, cam, base.add(d1.scale(ringR + hw)), u1, 1f, r, g, b, 0.3f);
            v(bb, m, cam, base.add(d0.scale(ringR + hw)), u0, 1f, r, g, b, 0.3f);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    // ---- estilhaços (triângulos arco-íris / quadrados brancos) que voam para fora ----
    private static void shards(Matrix4f m, Camera cam, Vec3 camPos, Instance i, Vec3 c, float p, int count,
                               boolean triangles, boolean rainbow, double spread, float k) {
        float env = CinematicManager.envelope(p) * Math.min(1f, k);
        if (env < 0.01f) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Random rnd = new Random(i.seed());
        Vector3f L = cam.getLeftVector(), U = cam.getUpVector();
        int sides = triangles ? 3 : 4;
        for (int s = 0; s < count; s++) {
            Vec3 dir = new Vec3(rnd.nextGaussian(), Math.abs(rnd.nextGaussian()) * 0.8 + 0.1, rnd.nextGaussian()).normalize();
            double speed = 0.4 + rnd.nextDouble() * 0.6;
            Vec3 pos = c.add(dir.scale(spread * (0.15 + p * speed * 1.4))).add(0, -p * p * spread * 0.15, 0);
            double sz = spread * (0.025 + rnd.nextDouble() * 0.035);
            double rot = rnd.nextDouble() * Math.PI * 2 + p * (rnd.nextDouble() - 0.5) * 14.0;
            int col = rainbow ? Mth.hsvToRgb(s / (float) count, 0.65f, 1f) : 0xFFFFFF;
            float r = cr(col), g = cg(col), b = cb(col);
            for (int q = 0; q < 4; q++) {
                int idx = Math.min(q, sides - 1);
                double ang = rot + idx * Math.PI * 2 / sides;
                double ox = Math.cos(ang) * sz, oy = Math.sin(ang) * sz;
                double wx = pos.x - camPos.x + L.x() * ox + U.x() * oy;
                double wy = pos.y - camPos.y + L.y() * ox + U.y() * oy;
                double wz = pos.z - camPos.z + L.z() * ox + U.z() * oy;
                bb.vertex(m, (float) wx, (float) wy, (float) wz).color(r, g, b, env).endVertex();
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }
}
