package com.luminblades.config;

import com.luminblades.sword.SwordSettings;
import com.luminblades.sword.SwordType;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/** Config COMMON (config/luminblades-common.toml): efeitos globais + uma seção por espada. */
public final class LuminConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue BLOOM_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BLOOM_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue BLOOM_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue BLOOM_BLUR_RADIUS;
    public static final ForgeConfigSpec.BooleanValue AURA_ENABLED;
    public static final ForgeConfigSpec.DoubleValue AURA_SIZE;
    public static final ForgeConfigSpec.BooleanValue TRAIL_ENABLED;
    public static final ForgeConfigSpec.IntValue TRAIL_LIFETIME_TICKS;
    public static final ForgeConfigSpec.DoubleValue TRAIL_WIDTH;
    public static final ForgeConfigSpec.BooleanValue DISTORTION_ENABLED;
    public static final ForgeConfigSpec.DoubleValue DISTORTION_STRENGTH;
    public static final ForgeConfigSpec.IntValue MAX_PARTICLES;
    public static final ForgeConfigSpec.IntValue CULL_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue CINE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue CINE_OVERLAYS;
    public static final ForgeConfigSpec.DoubleValue CINE_INTENSITY;

    private static final Map<SwordType, Section> SECTIONS = new EnumMap<>(SwordType.class);

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Efeitos visuais globais").push("global");
        BLOOM_ENABLED = b.comment("Liga o pos-processamento de Bloom (PostChain)").define("bloomEnabled", true);
        BLOOM_THRESHOLD = b.comment("Brilho minimo (0-1) para entrar no Bloom").defineInRange("bloomThreshold", 0.8, 0.0, 1.0);
        BLOOM_INTENSITY = b.comment("Intensidade do Bloom").defineInRange("bloomIntensity", 1.0, 0.0, 4.0);
        BLOOM_BLUR_RADIUS = b.comment("Raio do blur do Bloom (em texels)").defineInRange("bloomBlurRadius", 1.5, 0.5, 6.0);
        AURA_ENABLED = b.comment("Liga a aura shader ao redor da lamina").define("auraEnabled", true);
        AURA_SIZE = b.comment("Tamanho base da aura (blocos)").defineInRange("auraSize", 0.9, 0.2, 3.0);
        TRAIL_ENABLED = b.comment("Liga rastros de movimento durante ataques").define("trailEnabled", true);
        TRAIL_LIFETIME_TICKS = b.comment("Duracao do rastro (ticks)").defineInRange("trailLifetimeTicks", 14, 2, 40);
        TRAIL_WIDTH = b.comment("Largura do rastro (fracao do comprimento da lamina)").defineInRange("trailWidth", 0.55, 0.05, 1.0);
        DISTORTION_ENABLED = b.comment("Liga a distorcao de espaco nos impactos").define("distortionEnabled", true);
        DISTORTION_STRENGTH = b.comment("Forca da distorcao").defineInRange("distortionStrength", 1.0, 0.0, 3.0);
        MAX_PARTICLES = b.comment("Limite de particulas luminosas simultaneas no cliente").defineInRange("maxParticles", 2500, 100, 20000);
        CULL_DISTANCE = b.comment("Distancia maxima (blocos) para desenhar aura/rastro").defineInRange("cullDistance", 64, 8, 256);
        CINE_ENABLED = b.comment("Liga os efeitos cinematograficos (corte cosmico, circulo runico, arco-iris, buraco negro)").define("cinematicEnabled", true);
        CINE_OVERLAYS = b.comment("Liga os overlays em tela cheia (olho de dragao, sol)").define("cinematicOverlays", true);
        CINE_INTENSITY = b.comment("Intensidade dos efeitos cinematograficos").defineInRange("cinematicIntensity", 1.0, 0.0, 2.0);
        b.pop();

        for (SwordType t : SwordType.values()) {
            b.push(t.id);
            SECTIONS.put(t, new Section(b, t));
            b.pop();
        }
        SPEC = b.build();
    }

    private LuminConfig() {
    }

    public static SwordSettings settings(SwordType t) {
        Section s = SECTIONS.get(t);
        int rgb;
        try {
            rgb = Integer.parseInt(s.color.get().substring(1), 16);
        } catch (Exception e) {
            rgb = 0xFFFFFF;
        }
        return new SwordSettings(rgb, s.intensity.get(), s.duration.get(), s.size.get(),
                s.density.get(), s.cooldown.get(), s.power.get());
    }

    private static final class Section {
        final ForgeConfigSpec.ConfigValue<String> color;
        final ForgeConfigSpec.DoubleValue intensity;
        final ForgeConfigSpec.IntValue duration;
        final ForgeConfigSpec.DoubleValue size;
        final ForgeConfigSpec.DoubleValue density;
        final ForgeConfigSpec.IntValue cooldown;
        final ForgeConfigSpec.DoubleValue power;

        Section(ForgeConfigSpec.Builder b, SwordType t) {
            SwordType.Defaults d = t.defaults;
            color = b.comment("Cor principal (#RRGGBB)")
                    .define("color", t.defaultColor, o -> o instanceof String s && s.matches("#[0-9A-Fa-f]{6}"));
            intensity = b.comment("Intensidade visual (tamanho/brilho das particulas e aura)")
                    .defineInRange("intensity", d.intensity(), 0.1, 4.0);
            duration = b.comment("Duracao do efeito/habilidade (ticks)")
                    .defineInRange("durationTicks", d.duration(), 1, 600);
            size = b.comment("Tamanho/alcance/raio da habilidade (blocos)")
                    .defineInRange("size", d.size(), 0.5, 40.0);
            density = b.comment("Frequencia/densidade de particulas (1.0 = padrao)")
                    .defineInRange("particleDensity", d.density(), 0.0, 4.0);
            cooldown = b.comment("Recarga da habilidade (ticks)")
                    .defineInRange("cooldownTicks", d.cooldown(), 0, 24000);
            power = b.comment("Dano (ou cura) base da habilidade")
                    .defineInRange("power", d.power(), 0.0, 200.0);
        }
    }
}
