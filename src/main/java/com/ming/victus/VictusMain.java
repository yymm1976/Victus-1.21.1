package com.ming.victus;

import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.config.VictusConfig;
import com.ming.victus.entity.VictusEntityTypes;
import com.ming.victus.item.VictusItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(VictusMain.MOD_ID)
public class VictusMain {
    public static final String MOD_ID = "victus";

    @SuppressWarnings("null")
    public VictusMain(IEventBus modEventBus, ModContainer modContainer) {
        com.ming.victus.hearts.HeartAspectRegistry.registerDefaults();
        VictusItems.ITEMS.register(modEventBus);
        VictusItems.CREATIVE_MODE_TABS.register(modEventBus);
        VictusAttachments.ATTACHMENT_TYPES.register(modEventBus);
        VictusEntityTypes.ENTITY_TYPES.register(modEventBus);
        com.ming.victus.effect.VictusEffects.EFFECTS.register(modEventBus);
        com.ming.victus.effect.VictusPotions.POTIONS.register(modEventBus);
        modEventBus.addListener(com.ming.victus.network.VictusPackets::register);
        // DataGenerators 已通过 @EventBusSubscriber(bus = MOD) 自动注册，无需 addListener

        // 注册配置（COMMON 类型，服务端/客户端均加载）
        modContainer.registerConfig(ModConfig.Type.COMMON, VictusConfig.SPEC);

        // 在 FMLCommonSetupEvent 初始化 L2Tabs 兼容
        // 此阶段所有配置已加载（CONFIG 阶段在 COMMON_SETUP 之前），能正确读取用户配置；
        // 且在 RegisterEvent 之前，reg() 向 DeferredRegister 添加条目仍有效
        modEventBus.addListener(VictusMain::onCommonSetup);
    }

    /**
     * Common Setup 阶段初始化 L2Tabs 兼容。
     *
     * <p>NeoForge mod loading 阶段顺序：@Mod 构造 → CONFIG(配置加载) → COMMON_SETUP → ... → 游戏启动(RegisterEvent)。
     * 因此本阶段配置值已可用，且 DeferredRegister 仍接受注册（真正注册在后续 RegisterEvent）。</p>
     */
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        initL2TabsCompat();
    }

    /**
     * 反射调用 L2Tabs 兼容入口。
     *
     * <p>VictusMain 不得直接引用 VictusL2TabsCompat（否则 L2Tabs 缺失时类加载失败），
     * 通过反射隔离：Class.forName 仅查找类，方法体内部的 L2Tabs 引用在执行时才解析。</p>
     *
     * <p>VictusL2TabsCompat 类加载安全保证：该类无 L2Tabs 类型的字段/方法签名，
     * 所有 L2Tabs 引用限于 registerHeartTab() 方法体内部（局部变量，运行时解析）。</p>
     */
    private static void initL2TabsCompat() {
        if (!VictusConfig.ENABLE_L2TABS_COMPAT.get()) return;
        try {
            Class<?> clazz = Class.forName("com.ming.victus.client.compat.VictusL2TabsCompat");
            clazz.getMethod("init").invoke(null);
        } catch (ClassNotFoundException e) {
            // 兼容类本身不存在，忽略
        } catch (Exception e) {
            // L2Tabs 已安装但注册失败，记录但不中断加载
            org.slf4j.LoggerFactory.getLogger(MOD_ID).error("Failed to init L2Tabs compat", e);
        }
    }
}
