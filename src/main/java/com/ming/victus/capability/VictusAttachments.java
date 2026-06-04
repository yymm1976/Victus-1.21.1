package com.ming.victus.capability;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class VictusAttachments {
    @SuppressWarnings("null")
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, com.ming.victus.VictusMain.MOD_ID);

    @SuppressWarnings("null")
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerHeartCapability>> PLAYER_HEARTS =
        ATTACHMENT_TYPES.register("player_hearts", () ->
            AttachmentType.serializable(holder -> new PlayerHeartCapability((Player) holder))
                .copyOnDeath()
                .build()
        );

    @SuppressWarnings("null")
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> NO_DROPS =
        ATTACHMENT_TYPES.register("no_drops", () ->
            AttachmentType.builder(() -> false).build()
        );

    @SuppressWarnings("null")
    public static PlayerHeartCapability getHearts(Player player) {
        return player.getData(PLAYER_HEARTS.get());
    }
}
