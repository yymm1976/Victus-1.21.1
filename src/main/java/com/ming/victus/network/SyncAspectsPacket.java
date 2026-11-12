package com.ming.victus.network;

import com.ming.victus.VictusMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncAspectsPacket(CompoundTag tag) implements CustomPacketPayload {
    @SuppressWarnings("null")
    public static final Type<SyncAspectsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "sync_aspects"));

    @SuppressWarnings("null")
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAspectsPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG, SyncAspectsPacket::tag,
        SyncAspectsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}