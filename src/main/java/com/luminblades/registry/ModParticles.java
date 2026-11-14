package com.luminblades.registry;

import com.luminblades.LuminBlades;
import com.luminblades.vfx.GlowParticleOptions;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LuminBlades.MODID);

    @SuppressWarnings("deprecation")
    public static final RegistryObject<ParticleType<GlowParticleOptions>> GLOW = PARTICLES.register("glow",
            () -> new ParticleType<GlowParticleOptions>(false, GlowParticleOptions.DESERIALIZER) {
                @Override
                public Codec<GlowParticleOptions> codec() {
                    return GlowParticleOptions.CODEC;
                }
            });

    private ModParticles() {
    }
}
