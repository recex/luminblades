package com.luminblades.vfx;

import com.luminblades.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

/** Partícula luminosa com cor (r,g,b) e escala; trafega servidor -> cliente via sendParticles. */
public class GlowParticleOptions implements ParticleOptions {
    public static final Codec<GlowParticleOptions> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("r").forGetter(o -> o.r),
            Codec.FLOAT.fieldOf("g").forGetter(o -> o.g),
            Codec.FLOAT.fieldOf("b").forGetter(o -> o.b),
            Codec.FLOAT.fieldOf("scale").forGetter(o -> o.scale)
    ).apply(i, GlowParticleOptions::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<GlowParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public GlowParticleOptions fromCommand(ParticleType<GlowParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            reader.expect(' ');
            float s = reader.readFloat();
            return new GlowParticleOptions(r, g, b, s);
        }

        @Override
        public GlowParticleOptions fromNetwork(ParticleType<GlowParticleOptions> type, FriendlyByteBuf buf) {
            return new GlowParticleOptions(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }
    };

    public final float r, g, b, scale;

    public GlowParticleOptions(float r, float g, float b, float scale) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.scale = scale;
    }

    public static GlowParticleOptions of(int rgb, float scale) {
        return new GlowParticleOptions(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, scale);
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.GLOW.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
        buf.writeFloat(scale);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f",
                BuiltInRegistries.PARTICLE_TYPE.getKey(getType()), r, g, b, scale);
    }
}
