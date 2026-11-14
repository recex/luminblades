package com.luminblades.network;

import com.luminblades.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Dispara um efeito cinematográfico no cliente (posição, direção, cor, tamanho, duração, semente, tint). */
public record CinematicPacket(int type, double x, double y, double z, float dx, float dy, float dz,
                              int rgb, float size, int duration, int seed, float tint) {
    public static void encode(CinematicPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.type);
        b.writeDouble(m.x);
        b.writeDouble(m.y);
        b.writeDouble(m.z);
        b.writeFloat(m.dx);
        b.writeFloat(m.dy);
        b.writeFloat(m.dz);
        b.writeInt(m.rgb);
        b.writeFloat(m.size);
        b.writeVarInt(m.duration);
        b.writeInt(m.seed);
        b.writeFloat(m.tint);
    }

    public static CinematicPacket decode(FriendlyByteBuf b) {
        return new CinematicPacket(b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readFloat(), b.readFloat(), b.readFloat(), b.readInt(), b.readFloat(),
                b.readVarInt(), b.readInt(), b.readFloat());
    }

    public static void handle(CinematicPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.addCinematic(m)));
        ctx.get().setPacketHandled(true);
    }
}
