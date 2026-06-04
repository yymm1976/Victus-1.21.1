package com.ming.victus.network;

import com.ming.victus.VictusMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AspectBrokenPacket(int index, boolean callHandler) implements CustomPacketPayload {
    @SuppressWarnings("null")
    public static final Type<AspectBrokenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "aspect_broken"));

    @SuppressWarnings("null")
    public static final StreamCodec<RegistryFriendlyByteBuf, AspectBrokenPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, AspectBrokenPacket::index,
        ByteBufCodecs.BOOL, AspectBrokenPacket::callHandler,
        AspectBrokenPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
