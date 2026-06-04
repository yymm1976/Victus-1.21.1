package com.ming.victus;

import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.components.VictusComponents;
import com.ming.victus.item.VictusItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(VictusMain.MOD_ID)
public class VictusMain {
    public static final String MOD_ID = "victus";

    @SuppressWarnings("null")
    public VictusMain(IEventBus modEventBus) {
        com.ming.victus.hearts.HeartAspectRegistry.registerDefaults();
        VictusItems.ITEMS.register(modEventBus);
        VictusItems.CREATIVE_MODE_TABS.register(modEventBus);
        VictusComponents.COMPONENTS.register(modEventBus);
        VictusAttachments.ATTACHMENT_TYPES.register(modEventBus);
        com.ming.victus.effect.VictusEffects.EFFECTS.register(modEventBus);
        com.ming.victus.effect.VictusPotions.POTIONS.register(modEventBus);
        modEventBus.addListener(com.ming.victus.network.VictusPackets::register);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(com.ming.victus.datagen.DataGenerators::gatherData);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 通用初始化逻辑
    }
}
