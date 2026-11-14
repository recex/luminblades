package com.luminblades.network;

import com.luminblades.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Pede ao cliente uma distorção de espaço (onda de choque) em um ponto do mundo. */
public record DistortionPacket(double x, double y, double z, float strength, int duration) {
    public static void encode(DistortionPacket m, FriendlyByteBuf b) {
        b.writeDouble(m.x);
        b.writeDouble(m.y);
        b.writeDouble(m.z);
        b.writeFloat(m.strength);
        b.writeVarInt(m.duration);
    }

    public static DistortionPacket decode(FriendlyByteBuf b) {
        return new DistortionPacket(b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readVarInt());
    }

    public static void handle(DistortionPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHooks.addImpact(m.x, m.y, m.z, m.strength, m.duration)));
        ctx.get().setPacketHandled(true);
    }
}
