package com.ming.victus.network;

import com.ming.victus.VictusMain;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 通用网络包注册器 — 仅注册服务端接收的包。
 * 客户端接收的包（AspectBrokenPacket、SyncAspectsPacket）由
 * {@link com.ming.victus.client.VictusClientPackets} 在客户端侧独立注册，
 * 避免通用代码引用客户端专属类（如 Minecraft.getInstance()）。
 */
public class VictusPackets {

    @SuppressWarnings("null")
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VictusMain.MOD_ID).versioned("1.0");

        // 客户端→服务端的包在此注册
        // （当前无此类包；移除心相操作已由 VoidAspectItem 在服务端物品逻辑中处理，
        //   不再接受客户端主动请求，防止伪造包导致物品复制）

        // 服务端→客户端的包由 VictusClientPackets 在客户端侧注册
    }
}
