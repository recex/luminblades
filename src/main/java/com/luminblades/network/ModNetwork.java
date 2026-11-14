package com.luminblades.network;

import com.luminblades.LuminBlades;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LuminBlades.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, DistortionPacket.class, DistortionPacket::encode,
                DistortionPacket::decode, DistortionPacket::handle);
        CHANNEL.registerMessage(1, CinematicPacket.class, CinematicPacket::encode,
                CinematicPacket::decode, CinematicPacket::handle);
    }
}
