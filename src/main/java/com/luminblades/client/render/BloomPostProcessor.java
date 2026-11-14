package com.luminblades.client.render;

import com.google.gson.JsonSyntaxException;
import com.luminblades.LuminBlades;
import com.luminblades.client.cine.CinematicManager;
import com.luminblades.vfx.VfxManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class BloomPostProcessor implements AutoCloseable {
    private static final ResourceLocation CHAIN_ID = new ResourceLocation(LuminBlades.MODID, "shaders/post/lumin_bloom.json");

    private PostChain chain;
    private List<PostPass> passes = new ArrayList<>();
    private int width = -1, height = -1;
    private boolean failed;

    public boolean isLoaded() {
        return chain != null;
    }

    private void findPasses() {
        passes = new ArrayList<>();
        for (Field f : PostChain.class.getDeclaredFields()) {
            if (!List.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(chain);
                if (v instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof PostPass) {
                    for (Object o : l) passes.add((PostPass) o);
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        LuminBlades.LOGGER.error("Nao foi possivel localizar os passes do PostChain");
    }

    private void setUniform(String name, float value) {
        for (PostPass p : passes) {
            p.getEffect().safeGetUniform(name).set(value);
        }
    }

    public void render(float partialTick, float intensity, float threshold, float blurRadius) {
        if (failed) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, CHAIN_ID);
                chain.resize(main.width, main.height);
                width = main.width;
                height = main.height;
                findPasses();
                LuminBlades.LOGGER.info("PostChain {} carregado ({}x{}, {} passes)", CHAIN_ID, width, height, passes.size());
            } catch (IOException | JsonSyntaxException ex) {
                LuminBlades.LOGGER.error("Falha ao carregar PostChain {}", CHAIN_ID, ex);
                failed = true;
                chain = null;
                return;
            }
        } else if (width != main.width || height != main.height) {
            chain.resize(main.width, main.height);
            width = main.width;
            height = main.height;
        }

        setUniform("Intensity", intensity);
        setUniform("Threshold", threshold);
        setUniform("BlurRadius", blurRadius);
        setUniform("ImpactX", VfxManager.impactX);
        setUniform("ImpactY", VfxManager.impactY);
        setUniform("ImpactRadius", VfxManager.impactRadius);
        setUniform("ImpactStrength", VfxManager.impactStrength);
        setUniform("Time", mc.level == null ? 0f : (mc.level.getGameTime() + partialTick) / 20f);
        setUniform("LensX", CinematicManager.lensX);
        setUniform("LensY", CinematicManager.lensY);
        setUniform("LensRadius", CinematicManager.lensRadius);
        setUniform("LensFade", CinematicManager.lensFade);
        setUniform("LensR", CinematicManager.lensR);
        setUniform("LensG", CinematicManager.lensG);
        setUniform("LensB", CinematicManager.lensB);
        setUniform("OverlayMode", CinematicManager.overlayMode);
        setUniform("OverlayAlpha", CinematicManager.overlayAlpha);
        setUniform("OverlayProgress", CinematicManager.overlayProgress);
        setUniform("OverlayR", CinematicManager.overlayR);
        setUniform("OverlayG", CinematicManager.overlayG);
        setUniform("OverlayB", CinematicManager.overlayB);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(partialTick);
        main.bindWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    @Override
    public void close() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        passes = new ArrayList<>();
        failed = false;
    }
}
