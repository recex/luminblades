package com.luminblades.client.render;

import com.google.gson.JsonSyntaxException;
import com.luminblades.LuminBlades;
import com.luminblades.client.cine.CinematicManager;
import com.luminblades.vfx.VfxManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

/**
 * Executa a cadeia assets/luminblades/shaders/post/lumin_bloom.json em uma instância PRÓPRIA de PostChain
 * (distorção -> extração -> blur gaussiano 2x -> composição). Só roda quando o chamador pede.
 */
public final class BloomPostProcessor implements AutoCloseable {
    private static final ResourceLocation CHAIN_ID = new ResourceLocation(LuminBlades.MODID, "shaders/post/lumin_bloom.json");

    private PostChain chain;
    private int width = -1, height = -1;
    private boolean failed;

    public boolean isLoaded() {
        return chain != null;
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
                LuminBlades.LOGGER.info("PostChain {} carregado ({}x{})", CHAIN_ID, width, height);
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

        chain.setUniform("Intensity", intensity);
        chain.setUniform("Threshold", threshold);
        chain.setUniform("BlurRadius", blurRadius);
        chain.setUniform("ImpactX", VfxManager.impactX);
        chain.setUniform("ImpactY", VfxManager.impactY);
        chain.setUniform("ImpactRadius", VfxManager.impactRadius);
        chain.setUniform("ImpactStrength", VfxManager.impactStrength);

        chain.setUniform("Time", mc.level == null ? 0f : (mc.level.getGameTime() + partialTick) / 20f);
        chain.setUniform("LensX", CinematicManager.lensX);
        chain.setUniform("LensY", CinematicManager.lensY);
        chain.setUniform("LensRadius", CinematicManager.lensRadius);
        chain.setUniform("LensFade", CinematicManager.lensFade);
        chain.setUniform("LensR", CinematicManager.lensR);
        chain.setUniform("LensG", CinematicManager.lensG);
        chain.setUniform("LensB", CinematicManager.lensB);
        chain.setUniform("OverlayMode", CinematicManager.overlayMode);
        chain.setUniform("OverlayAlpha", CinematicManager.overlayAlpha);
        chain.setUniform("OverlayProgress", CinematicManager.overlayProgress);
        chain.setUniform("OverlayR", CinematicManager.overlayR);
        chain.setUniform("OverlayG", CinematicManager.overlayG);
        chain.setUniform("OverlayB", CinematicManager.overlayB);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(partialTick);
        main.bindWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    /** Chamado no reload de recursos (F3+T): libera a cadeia e permite nova tentativa. */
    @Override
    public void close() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        failed = false;
    }
}
