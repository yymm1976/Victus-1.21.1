package com.ming.victus.test;

import com.ming.victus.VictusMain;
import com.ming.victus.item.VictusItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Collection;

@GameTestHolder(VictusMain.MOD_ID)
public class VictusGameTests {

    @GameTest(template = "minecraft:empty")
    @PrefixGameTestTemplate(false)
    public static void testCreativeTab(GameTestHelper helper) {
        CreativeModeTab tab = VictusItems.VICTUS_TAB.get();
        if (tab == null) {
            helper.fail("Creative tab is null!");
            return;
        }

        Collection<ItemStack> displayItems = tab.getDisplayItems();
        int expectedSize = VictusItems.ITEMS.getEntries().size();

        if (displayItems.size() != expectedSize) {
            helper.fail("Creative tab item count mismatch! Expected " + expectedSize + " but got " + displayItems.size());
            return;
        }

        helper.succeed();
    }
}