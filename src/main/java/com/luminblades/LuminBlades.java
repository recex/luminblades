package com.luminblades;

import com.luminblades.config.LuminConfig;
import com.luminblades.network.ModNetwork;
import com.luminblades.registry.ModCreativeTabs;
import com.luminblades.registry.ModItems;
import com.luminblades.registry.ModParticles;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Ponto de entrada: só liga os registros e a config. Cada responsabilidade vive em seu pacote. */
@Mod(LuminBlades.MODID)
public class LuminBlades {
    public static final String MODID = "luminblades";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LuminBlades() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LuminConfig.SPEC);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
