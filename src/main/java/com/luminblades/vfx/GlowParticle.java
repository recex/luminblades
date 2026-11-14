package com.luminblades.vfx;

import com.luminblades.config.LuminConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Partícula aditiva, sempre "full bright", com limite global de instâncias. */
@OnlyIn(Dist.CLIENT)
public class GlowParticle extends TextureSheetParticle {
    private static int live;

    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder b, TextureManager tm) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator t) {
            t.end();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }

        @Override
        public String toString() {
            return "LUMIN_ADDITIVE";
        }
    };

    private boolean counted;

    protected GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                           GlowParticleOptions o, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.rCol = o.r;
        this.gCol = o.g;
        this.bCol = o.b;
        this.quadSize = 0.14F * o.scale;
        this.lifetime = 18 + this.random.nextInt(14);
        this.gravity = 0.0F;
        this.friction = 0.93F;
        this.hasPhysics = false;
        pickSprite(sprites);
        live++;
        counted = true;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = Math.max(0.0F, 1.0F - (float) this.age / (float) this.lifetime);
    }

    @Override
    public float getQuadSize(float partialTick) {
        return this.quadSize * (1.0F - 0.55F * ((this.age + partialTick) / (float) this.lifetime));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ADDITIVE;
    }

    @Override
    public void remove() {
        if (counted) {
            counted = false;
            live = Math.max(0, live - 1);
        }
        super.remove();
    }

    public static void resetCount() {
        live = 0;
    }

    public static class Provider implements ParticleProvider<GlowParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(GlowParticleOptions o, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            if (live >= LuminConfig.MAX_PARTICLES.get()) return null;
            return new GlowParticle(level, x, y, z, vx, vy, vz, o, sprites);
        }
    }
}
