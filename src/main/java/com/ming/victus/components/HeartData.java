package com.ming.victus.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HeartData(String heartType, int tier) {

    @SuppressWarnings("null")
    public static final Codec<HeartData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("heartType").forGetter(HeartData::heartType),
            Codec.INT.fieldOf("tier").forGetter(HeartData::tier)
        ).apply(instance, HeartData::new)
    );

    @SuppressWarnings("null")
    public static final StreamCodec<RegistryFriendlyByteBuf, HeartData> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, HeartData::heartType,
            ByteBufCodecs.INT, HeartData::tier,
            HeartData::new
        );
}
