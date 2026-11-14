package com.luminblades.client;

import com.luminblades.LuminBlades;
import com.luminblades.client.anim.SwordAnimator;
import com.luminblades.client.cine.CinematicManager;
import com.luminblades.client.cine.CinematicRenderer;
import com.luminblades.client.render.AuraRenderer;
import com.luminblades.client.render.BloomPostProcessor;
import com.luminblades.client.render.TrailRenderer;
import com.luminblades.config.LuminConfig;
import com.luminblades.sword.LuminSwordItem;
import com.luminblades.vfx.GlowParticle;
import com.luminblades.vfx.VfxManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eventos do barramento FORGE (jogo):
 *  - AFTER_PARTICLES: aura + rastros + projeção do impacto de distorção;
 *  - RenderGuiEvent.Pre: roda o PostChain (depois do mundo E da mão, antes da GUI);
 *  - RenderHandEvent: animação de entrada/ataque/ativação na primeira pessoa.
 */
@Mod.EventBusSubscriber(modid = LuminBlades.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    public static final BloomPostProcessor BLOOM = new BloomPostProcessor();

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) SwordAnimator.tick();
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = e.getPartialTick();
        AuraRenderer.render(e.getPoseStack(), e.getCamera(), pt, e.getFrustum());
        TrailRenderer.render(e.getPoseStack(), e.getCamera(), pt, e.getFrustum());
        CinematicRenderer.render(e.getPoseStack(), e.getCamera(), pt, e.getFrustum());
        CinematicManager.captureScreen(e.getPoseStack().last().pose(), e.getProjectionMatrix(), e.getCamera().getPosition(), pt);
        VfxManager.captureImpact(e.getPoseStack().last().pose(), e.getProjectionMatrix(), e.getCamera().getPosition(), pt);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float pt = e.getPartialTick();
        float glow = Math.max(SwordAnimator.glowFactor(pt), AuraRenderer.remoteGlow);
        float bloom = LuminConfig.BLOOM_ENABLED.get() ? glow * LuminConfig.BLOOM_INTENSITY.get().floatValue() : 0f;
        boolean distort = LuminConfig.DISTORTION_ENABLED.get() && VfxManager.hasImpact();
        if (bloom < 0.02f && !distort && !CinematicManager.screenActive()) return;
        BLOOM.render(pt, bloom, LuminConfig.BLOOM_THRESHOLD.get().floatValue(),
                LuminConfig.BLOOM_BLUR_RADIUS.get().floatValue());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getItemStack().getItem() instanceof LuminSwordItem)) return;
        float pt = e.getPartialTick();
        float attack = SwordAnimator.attack(pt), act = SwordAnimator.activate(pt), eq = SwordAnimator.equip(pt);
        PoseStack ps = e.getPoseStack();
        ps.translate(0.02f * act, 0.05f * act - 0.03f * attack - (1f - eq) * 0.15f, -0.06f * act);
        ps.mulPose(Axis.ZP.rotationDegrees(-6f * attack + 4f * (1f - eq)));
        float sc = 1f + 0.05f * act;
        ps.scale(sc, sc, sc);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        VfxManager.reset();
        CinematicManager.reset();
        GlowParticle.resetCount();
        SwordAnimator.reset();
    }
}
