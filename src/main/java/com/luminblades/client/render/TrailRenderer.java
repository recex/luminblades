package com.luminblades.client.render;

import com.luminblades.config.LuminConfig;
import com.luminblades.sword.LuminSwordItem;
import com.luminblades.vfx.VfxManager;
import com.luminblades.vfx.VfxManager.Trail;
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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.Map;

/** Rastro em fita (base -> ponta da lâmina) amostrado a cada frame durante o swing; fade por idade. */
public final class TrailRenderer {
    private TrailRenderer() {
    }

    public static void render(PoseStack ps, Camera cam, float pt, Frustum frustum) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !LuminConfig.TRAIL_ENABLED.get()) {
            VfxManager.TRAILS.clear();
            return;
        }
        double now = mc.level.getGameTime() + pt;
        double life = LuminConfig.TRAIL_LIFETIME_TICKS.get();
        double cull = LuminConfig.CULL_DISTANCE.get();
        double cull2 = cull * cull;
        Vec3 camPos = cam.getPosition();
        float width = LuminConfig.TRAIL_WIDTH.get().floatValue();
        Matrix4f m = ps.last().pose();

        for (Player pl : mc.level.players()) {
            Trail tr = VfxManager.TRAILS.get(pl.getUUID());
            if (pl.getMainHandItem().getItem() instanceof LuminSwordItem sw && pl.swinging
                    && pl.distanceToSqr(camPos) < cull2) {
                if (tr == null) VfxManager.TRAILS.put(pl.getUUID(), tr = new Trail());
                float swing = pl.getAttackAnim(pt);
                float ease = 1f - (1f - swing) * (1f - swing);
                float yaw = pl.getViewYRot(pt) + 75f - 150f * ease;
                float pitch = pl.getViewXRot(pt) * 0.5f - 12f - (float) Math.sin(swing * Math.PI) * 18f;
                Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
                Vec3 shoulder = pl.getEyePosition(pt).add(0, -0.35, 0);
                int rgb = LuminConfig.settings(sw.type).rgb();
                tr.push(shoulder.add(dir.scale(0.6)), shoulder.add(dir.scale(2.0)), now, rgb);
            }
            if (tr != null) tr.prune(now, life);
        }
        // limpeza de rastros vazios / jogadores ausentes
        Iterator<Map.Entry<java.util.UUID, Trail>> it = VfxManager.TRAILS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<java.util.UUID, Trail> e = it.next();
            if (e.getValue().count == 0 && mc.level.getPlayerByUUID(e.getKey()) == null) it.remove();
        }

        BufferBuilder bb = null;
        for (Trail tr : VfxManager.TRAILS.values()) {
            if (tr.count < 2) continue;
            int a0 = tr.idx(0), a1 = tr.idx(tr.count - 1);
            AABB box = new AABB(tr.tx[a0], tr.ty[a0], tr.tz[a0], tr.tx[a1], tr.ty[a1], tr.tz[a1]).inflate(2.5);
            if (!frustum.isVisible(box)) continue;
            if (bb == null) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                bb = Tesselator.getInstance().getBuilder();
                bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            }
            float r = ((tr.rgb >> 16) & 255) / 255f, g = ((tr.rgb >> 8) & 255) / 255f, b = (tr.rgb & 255) / 255f;
            for (int i = 0; i < tr.count - 1; i++) {
                int p = tr.idx(i), q = tr.idx(i + 1);
                float ap = fade(now - tr.born[p], life), aq = fade(now - tr.born[q], life);
                vtx(bb, m, camPos, tr.tx[p], tr.ty[p], tr.tz[p], r, g, b, ap);
                vtx(bb, m, camPos, tr.tx[q], tr.ty[q], tr.tz[q], r, g, b, aq);
                vtx(bb, m, camPos, Mth.lerp(width, tr.tx[q], tr.bx[q]), Mth.lerp(width, tr.ty[q], tr.by[q]),
                        Mth.lerp(width, tr.tz[q], tr.bz[q]), r, g, b, 0f);
                vtx(bb, m, camPos, Mth.lerp(width, tr.tx[p], tr.bx[p]), Mth.lerp(width, tr.ty[p], tr.by[p]),
                        Mth.lerp(width, tr.tz[p], tr.bz[p]), r, g, b, 0f);
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

    private static float fade(double age, double life) {
        float t = (float) Mth.clamp(age / life, 0.0, 1.0);
        return (1f - t) * (1f - t) * 0.85f;
    }

    private static void vtx(BufferBuilder bb, Matrix4f m, Vec3 cam, double x, double y, double z,
                            float r, float g, float b, float a) {
        bb.vertex(m, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color(r, g, b, a).endVertex();
    }
}
