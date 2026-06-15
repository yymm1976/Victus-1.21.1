package com.ming.victus.client;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.network.AspectBrokenPacket;
import com.ming.victus.network.SyncAspectsPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 客户端专用网络包注册与处理器。
 * 将 playToClient 的包处理逻辑从 common 包（VictusPackets）迁出，
 * 避免通用代码引用 net.minecraft.client.Minecraft 等客户端专属类，
 * 确保 Dedicated Server 环境下类加载安全。
 */
@EventBusSubscriber(modid = VictusMain.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class VictusClientPackets {

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VictusMain.MOD_ID).versioned("1.0");

        // 心相破碎动画包：服务端→客户端
        registrar.playToClient(
            AspectBrokenPacket.TYPE,
            AspectBrokenPacket.STREAM_CODEC,
            VictusClientPackets::handleAspectBroken
        );

        // 心相数据同步包：服务端→客户端
        registrar.playToClient(
            SyncAspectsPacket.TYPE,
            SyncAspectsPacket.STREAM_CODEC,
            VictusClientPackets::handleSyncAspects
        );
    }

    /**
     * 处理心相破碎动画：在客户端触发破碎视觉效果。
     * 仅在客户端执行，通过 enqueueWork 确保在主线程运行。
     */
    private static void handleAspectBroken(final AspectBrokenPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client.player != null) {
                HeartAspect.createBreakEvent(client, data.index(), data.callHandler()).run();
            }
        });
    }

    /**
     * 处理心相数据同步：将服务端发来的心相 NBT 数据反序列化到本地玩家。
     * 仅同步本地玩家自身的数据（SyncAspectsPacket 不携带目标实体 ID，
     * 因此当前不支持观察其他玩家心相）。
     */
    @SuppressWarnings("null")
    private static void handleSyncAspects(final SyncAspectsPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client.player != null) {
                PlayerHeartCapability cap = VictusAttachments.getHearts(client.player);
                if (cap != null) {
                    cap.deserializeNBT(client.player.registryAccess(), data.tag());
                }
            }
        });
    }
}
