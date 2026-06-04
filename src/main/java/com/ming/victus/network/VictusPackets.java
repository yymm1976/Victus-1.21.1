package com.ming.victus.network;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class VictusPackets {

    @SuppressWarnings("null")
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VictusMain.MOD_ID).versioned("1.0");

        registrar.playToClient(
            AspectBrokenPacket.TYPE,
            AspectBrokenPacket.STREAM_CODEC,
            VictusPackets::handleAspectBroken
        );

        registrar.playToClient(
            SyncAspectsPacket.TYPE,
            SyncAspectsPacket.STREAM_CODEC,
            VictusPackets::handleSyncAspects
        );

        registrar.playToServer(
            RemoveFirstAspectPacket.TYPE,
            RemoveFirstAspectPacket.STREAM_CODEC,
            VictusPackets::handleRemoveFirstAspect
        );
    }

    private static void handleAspectBroken(final AspectBrokenPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            @SuppressWarnings("null")
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                HeartAspect.createBreakEvent(client, data.index(), data.callHandler()).run();
            }
        });
    }

    @SuppressWarnings("null")
    private static void handleSyncAspects(final SyncAspectsPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                PlayerHeartCapability cap = VictusAttachments.getHearts(client.player);
                if (cap != null) {
                    cap.deserializeNBT(client.player.registryAccess(), data.tag());
                }
            }
        });
    }

    @SuppressWarnings("null")
    private static void handleRemoveFirstAspect(final RemoveFirstAspectPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof Player player) {
                if (!player.isCreative()) return;
                PlayerHeartCapability cap = VictusAttachments.getHearts(player);
                if (cap != null && !cap.empty()) {
                    HeartAspect aspect = cap.removeAspect();
                    player.spawnAtLocation(aspect.asItem());
                }
            }
        });
    }
}
