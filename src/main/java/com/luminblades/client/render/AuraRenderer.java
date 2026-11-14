package com.luminblades.client.render;

import com.luminblades.client.ClientModEvents;
import com.luminblades.client.anim.SwordAnimator;
import com.luminblades.config.LuminConfig;
import com.luminblades.sword.LuminSwordItem;
import com.luminblades.sword.SwordSettings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Desenha a aura (shader core blade_aura) como 3 billboards aditivos ao longo da lâmina. Com culling. */
public final class AuraRenderer {
    /** Brilho de espadas de OUTROS jogadores vistas neste frame (o Bloom usa isso). */
    public static float remoteGlow;

    private AuraRenderer() {
    }

    public static void render(PoseStack ps, Camera cam, float pt, Frustum frustum) {
        remoteGlow = 0f;
        ShaderInstance shader = ClientModEvents.auraShader;
        Minecraft mc = Minecraft.getInstance();
        if (shader == null || mc.level == null || !LuminConfig.AURA_ENABLED.get()) return;

        Vec3 camPos = cam.getPosition();
        double cull = LuminConfig.CULL_DISTANCE.get();
        double cull2 = cull * cull;
        float base = LuminConfig.AURA_SIZE.get().floatValue();
        float time = (mc.level.getGameTime() + pt) / 20f;
        Matrix4f m = ps.last().pose();

        BufferBuilder bb = null;
        for (Player pl : mc.level.players()) {
            ItemStack st = pl.getMainHandItem();
            if (!(st.getItem() instanceof LuminSwordItem sw) || pl.isInvisible()) continue;
            boolean local = pl == mc.player;
            boolean firstPerson = local && mc.options.getCameraType().isFirstPerson();

            Vec3 eye = pl.getEyePosition(pt);
            Vec3 dir;
            Vec3 center;
            if (firstPerson) {
                Vector3f look = cam.getLookVector(), left = cam.getLeftVector(), up = cam.getUpVector();
                dir = new Vec3(look.x(), look.y(), look.z());
                center = eye.add(dir.scale(1.15))
                        .add(-left.x() * 0.45, -left.y() * 0.45, -left.z() * 0.45)
                        .add(-up.x() * 0.35, -up.y() * 0.35, -up.z() * 0.35);
            } else {
                dir = pl.getViewVector(pt);
                Vec3 right = dir.cross(new Vec3(0, 1, 0)).normalize();
                center = eye.add(0, -0.35, 0).add(dir.scale(1.0)).add(right.scale(0.35));
            }
            if (center.distanceToSqr(camPos) > cull2) continue;
            if (!frustum.isVisible(new AABB(center, center).inflate(base * 3.0))) continue;

            SwordSettings cfg = com.luminblades.config.LuminConfig.settings(sw.type);
            float level = local ? SwordAnimator.glowFactor(pt) : 1f;
            if (!local) remoteGlow = 0.7f;
            float alpha = Math.min(1f, (float) cfg.intensity() * 0.55f * level);
            if (alpha < 0.01f) continue;
            float r = ((cfg.rgb() >> 16) & 255) / 255f, g = ((cfg.rgb() >> 8) & 255) / 255f, b = (cfg.rgb() & 255) / 255f;

            if (bb == null) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                RenderSystem.setShader(() -> shader);
                shader.safeGetUniform("Time").set(time);
                bb = Tesselator.getInstance().getBuilder();
                bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            }
            float[] scale = {1.0f, 0.75f, 0.55f};
            double[] along = {0.0, 0.55, 1.1};
            for (int i = 0; i < 3; i++) {
                Vec3 c = center.add(dir.scale(along[i])).subtract(camPos);
                quad(bb, m, cam, c, base * scale[i] * (float) Math.max(0.6, cfg.intensity()), r, g, b, alpha * (1f - i * 0.2f));
            }
        }
        if (bb != null) {
            BufferUploader.drawWithShader(bb.end());
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void quad(BufferBuilder bb, Matrix4f m, Camera cam, Vec3 c, float size, float r, float g, float b, float a) {
        Vector3f l = cam.getLeftVector(), u = cam.getUpVector();
        float lx = l.x() * size, ly = l.y() * size, lz = l.z() * size;
        float ux = u.x() * size, uy = u.y() * size, uz = u.z() * size;
        float x = (float) c.x, y = (float) c.y, z = (float) c.z;
        bb.vertex(m, x + lx - ux, y + ly - uy, z + lz - uz).uv(0f, 1f).color(r, g, b, a).endVertex();
        bb.vertex(m, x - lx - ux, y - ly - uy, z - lz - uz).uv(1f, 1f).color(r, g, b, a).endVertex();
        bb.vertex(m, x - lx + ux, y - ly + uy, z - lz + uz).uv(1f, 0f).color(r, g, b, a).endVertex();
        bb.vertex(m, x + lx + ux, y + ly + uy, z + lz + uz).uv(0f, 0f).color(r, g, b, a).endVertex();
    }
}
