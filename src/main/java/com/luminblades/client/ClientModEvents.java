package com.luminblades.client;

import com.luminblades.LuminBlades;
import com.luminblades.registry.ModParticles;
import com.luminblades.vfx.GlowParticle;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** Eventos do barramento do MOD (registro): shader core da aura, provider de partículas, reload do PostChain. */
@Mod.EventBusSubscriber(modid = LuminBlades.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static ShaderInstance auraShader;
    public static ShaderInstance galaxyShader;
    public static ShaderInstance runeShader;
    public static ShaderInstance rainbowShader;

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent e) throws IOException {
        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                new ResourceLocation(LuminBlades.MODID, "blade_aura"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> auraShader = s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                new ResourceLocation(LuminBlades.MODID, "galaxy_slash"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> galaxyShader = s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                new ResourceLocation(LuminBlades.MODID, "rune_circle"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> runeShader = s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                new ResourceLocation(LuminBlades.MODID, "rainbow_ribbon"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> rainbowShader = s);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.GLOW.get(), GlowParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerReload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((ResourceManagerReloadListener) rm -> ClientEvents.BLOOM.close());
    }
}
