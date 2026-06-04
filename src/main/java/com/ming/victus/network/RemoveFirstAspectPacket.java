package com.ming.victus.network;

import com.ming.victus.VictusMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoveFirstAspectPacket() implements CustomPacketPayload {
    @SuppressWarnings("null")
    public static final Type<RemoveFirstAspectPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "remove_first_aspect"));

    @SuppressWarnings("null")
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveFirstAspectPacket> STREAM_CODEC = StreamCodec.unit(new RemoveFirstAspectPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
