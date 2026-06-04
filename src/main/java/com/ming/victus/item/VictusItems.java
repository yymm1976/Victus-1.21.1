package com.ming.victus.item;

import com.ming.victus.hearts.content.*;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class VictusItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(com.ming.victus.VictusMain.MOD_ID);
    @SuppressWarnings("null")
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, com.ming.victus.VictusMain.MOD_ID);

    public static final DeferredHolder<Item, Item> GRILLED_HEART_ASPECT = ITEMS.register("grilled_heart_aspect", () -> new HeartAspectItem(GrilledAspect.TYPE));
    public static final DeferredHolder<Item, Item> BUNDLE_HEART_ASPECT = ITEMS.register("bundle_heart_aspect", () -> new HeartAspectItem(BundleAspect.TYPE));
    public static final DeferredHolder<Item, Item> CREEPER_HEART_ASPECT = ITEMS.register("creeper_heart_aspect", () -> new HeartAspectItem(CreeperAspect.TYPE));
    public static final DeferredHolder<Item, Item> DIAMOND_HEART_ASPECT = ITEMS.register("diamond_heart_aspect", () -> new HeartAspectItem(DiamondAspect.TYPE));
    public static final DeferredHolder<Item, Item> LIGHT_HEART_ASPECT = ITEMS.register("light_heart_aspect", () -> new HeartAspectItem(LightAspect.TYPE));
    public static final DeferredHolder<Item, Item> OCEAN_HEART_ASPECT = ITEMS.register("ocean_heart_aspect", () -> new HeartAspectItem(OceanAspect.TYPE));
    public static final DeferredHolder<Item, Item> TOTEM_HEART_ASPECT = ITEMS.register("totem_heart_aspect", () -> new HeartAspectItem(TotemAspect.TYPE));
    public static final DeferredHolder<Item, Item> POTION_HEART_ASPECT = ITEMS.register("potion_heart_aspect", () -> new HeartAspectItem(PotionAspect.TYPE));
    public static final DeferredHolder<Item, Item> ARCHERY_HEART_ASPECT = ITEMS.register("archery_heart_aspect", () -> new HeartAspectItem(ArcheryAspect.TYPE));
    public static final DeferredHolder<Item, Item> BLAZING_HEART_ASPECT = ITEMS.register("blazing_heart_aspect", () -> new HeartAspectItem(BlazingAspect.TYPE));
    public static final DeferredHolder<Item, Item> DRACONIC_HEART_ASPECT = ITEMS.register("draconic_heart_aspect", () -> new HeartAspectItem(DraconicAspect.TYPE));
    public static final DeferredHolder<Item, Item> EMERALD_HEART_ASPECT = ITEMS.register("emerald_heart_aspect", () -> new HeartAspectItem(EmeraldAspect.TYPE));
    public static final DeferredHolder<Item, Item> EVOKING_HEART_ASPECT = ITEMS.register("evoking_heart_aspect", () -> new HeartAspectItem(EvokingAspect.TYPE));
    public static final DeferredHolder<Item, Item> GOLDEN_HEART_ASPECT = ITEMS.register("golden_heart_aspect", () -> new HeartAspectItem(GoldenAspect.TYPE));
    public static final DeferredHolder<Item, Item> ICY_HEART_ASPECT = ITEMS.register("icy_heart_aspect", () -> new HeartAspectItem(IcyAspect.TYPE));
    public static final DeferredHolder<Item, Item> IRON_HEART_ASPECT = ITEMS.register("iron_heart_aspect", () -> new HeartAspectItem(IronAspect.TYPE));
    public static final DeferredHolder<Item, Item> LAPIS_HEART_ASPECT = ITEMS.register("lapis_heart_aspect", () -> new HeartAspectItem(LapisAspect.TYPE));
    public static final DeferredHolder<Item, Item> SWEET_HEART_ASPECT = ITEMS.register("sweet_heart_aspect", () -> new HeartAspectItem(SweetAspect.TYPE));
    public static final DeferredHolder<Item, Item> CHEESE_HEART_ASPECT = ITEMS.register("cheese_heart_aspect", () -> new HeartAspectItem(CheeseAspect.TYPE));

    public static final DeferredHolder<Item, Item> VOID_HEART_ASPECT = ITEMS.register("void_heart_aspect", VoidAspectItem::new);
    public static final DeferredHolder<Item, Item> BROKEN_HEART = ITEMS.register("broken_heart", BrokenHeartItem::new);
    public static final DeferredHolder<Item, Item> BLANK_HEART_ASPECT = ITEMS.register("blank_heart_aspect", BlankAspectItem::new);

    @SuppressWarnings("null")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VICTUS_TAB = CREATIVE_MODE_TABS.register(com.ming.victus.VictusMain.MOD_ID, () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.victus"))
        .icon(() -> new net.minecraft.world.item.ItemStack(DIAMOND_HEART_ASPECT.get()))
        .displayItems((parameters, output) -> {
            ITEMS.getEntries().forEach(item -> output.accept(item.get()));
        })
        .build());
}
