package com.ming.victus;

import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.entity.VictusEntityTypes;
import com.ming.victus.item.VictusItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(VictusMain.MOD_ID)
public class VictusMain {
    public static final String MOD_ID = "victus";

    @SuppressWarnings("null")
    public VictusMain(IEventBus modEventBus) {
        com.ming.victus.hearts.HeartAspectRegistry.registerDefaults();
        VictusItems.ITEMS.register(modEventBus);
        VictusItems.CREATIVE_MODE_TABS.register(modEventBus);
        VictusAttachments.ATTACHMENT_TYPES.register(modEventBus);
        VictusEntityTypes.ENTITY_TYPES.register(modEventBus);
        com.ming.victus.effect.VictusEffects.EFFECTS.register(modEventBus);
        com.ming.victus.effect.VictusPotions.POTIONS.register(modEventBus);
        modEventBus.addListener(com.ming.victus.network.VictusPackets::register);
        // DataGenerators 已通过 @EventBusSubscriber(bus = MOD) 自动注册，无需 addListener
    }
}
