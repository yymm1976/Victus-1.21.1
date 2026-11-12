package com.ming.victus.test;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.hearts.content.ArcheryAspect;
import com.ming.victus.hearts.content.BlazingAspect;
import com.ming.victus.hearts.content.BundleAspect;
import com.ming.victus.hearts.content.CheeseAspect;
import com.ming.victus.hearts.content.CreeperAspect;
import com.ming.victus.hearts.content.DiamondAspect;
import com.ming.victus.hearts.content.DraconicAspect;
import com.ming.victus.hearts.content.EmeraldAspect;
import com.ming.victus.hearts.content.EvokingAspect;
import com.ming.victus.hearts.content.GoldenAspect;
import com.ming.victus.hearts.content.GrilledAspect;
import com.ming.victus.hearts.content.IcyAspect;
import com.ming.victus.hearts.content.IronAspect;
import com.ming.victus.hearts.content.LapisAspect;
import com.ming.victus.hearts.content.LightAspect;
import com.ming.victus.hearts.content.OceanAspect;
import com.ming.victus.hearts.content.PotionAspect;
import com.ming.victus.hearts.content.SweetAspect;
import com.ming.victus.hearts.content.TotemAspect;
import com.ming.victus.item.VictusItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder(VictusMain.MOD_ID)
public class VictusGameTests {

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void testCreativeTab(GameTestHelper helper) {
        // Verify that all registered items exist in the registry
        int registrySize = VictusItems.ITEMS.getEntries().size();
        if (registrySize == 0) {
            helper.fail("Item registry is empty! ITEMS has 0 entries");
            return;
        }

        // On a dedicated/server environment, CreativeModeTab.getDisplayItems() returns 0
        // because the tab is not populated without a connected client.
        // We verify item quantity through the registry instead.
        if (registrySize < 22) {
            helper.fail("Expected at least 22 items in registry but got " + registrySize);
            return;
        }

        helper.succeed();
    }

    // -----------------------------------------------------------------------
    // Test 1: 心相 Type ID 与 translationKey 稳定性
    // 防止 NBT 兼容性破坏 + HUD raw key 暴露
    // -----------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void testAspectTypeIdAndTranslationKey(GameTestHelper helper) {
        List<HeartAspect.Type> types = new ArrayList<>();
        types.add(GrilledAspect.TYPE);
        types.add(BundleAspect.TYPE);
        types.add(CreeperAspect.TYPE);
        types.add(DiamondAspect.TYPE);
        types.add(LightAspect.TYPE);
        types.add(OceanAspect.TYPE);
        types.add(TotemAspect.TYPE);
        types.add(PotionAspect.TYPE);
        types.add(ArcheryAspect.TYPE);
        types.add(BlazingAspect.TYPE);
        types.add(DraconicAspect.TYPE);
        types.add(EmeraldAspect.TYPE);
        types.add(EvokingAspect.TYPE);
        types.add(GoldenAspect.TYPE);
        types.add(IcyAspect.TYPE);
        types.add(IronAspect.TYPE);
        types.add(LapisAspect.TYPE);
        types.add(SweetAspect.TYPE);
        types.add(CheeseAspect.TYPE);

        if (types.size() != 19) {
            helper.fail("Expected 19 aspect types but got " + types.size());
            return;
        }

        for (HeartAspect.Type type : types) {
            ResourceLocation id = type.id();
            // 1. namespace 必须是 victus
            if (!"victus".equals(id.getNamespace())) {
                helper.fail("Aspect type namespace must be 'victus' but was '" + id.getNamespace() + "' for " + id);
                return;
            }
            // 2. id path 必须是短名（不以 _heart_aspect 结尾，保持 NBT 兼容）
            String path = id.getPath();
            if (path.endsWith("_heart_aspect")) {
                helper.fail("Aspect type id path must remain short (e.g. 'grilled') but got '" + path + "'");
                return;
            }
            // 3. translationKey 必须返回 item.victus.<path>_heart_aspect
            String key = type.translationKey();
            String expected = "item.victus." + path + "_heart_aspect";
            if (!expected.equals(key)) {
                helper.fail("translationKey mismatch for " + id + ": expected '" + expected + "' but got '" + key + "'");
                return;
            }
            // 4. 不允许出现 _heart_aspect_heart_aspect 重复后缀
            if (key.contains("_heart_aspect_heart_aspect")) {
                helper.fail("translationKey has duplicated suffix for " + id + ": " + key);
                return;
            }
        }

        helper.succeed();
    }

    // -----------------------------------------------------------------------
    // Test 2: mod logo 资源存在且是 PNG
    // 防止 mods.toml logo 缺失 / victus.png 误删
    // -----------------------------------------------------------------------
    private static final byte[] PNG_MAGIC = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void testModLogoExistsAndIsPng(GameTestHelper helper) {
        if (!resourceExists("victus.png")) {
            helper.fail("mod logo resource 'victus.png' is missing from classpath");
            return;
        }
        try (InputStream in = VictusGameTests.class.getClassLoader().getResourceAsStream("victus.png")) {
            if (in == null) {
                helper.fail("mod logo resource 'victus.png' returned null InputStream");
                return;
            }
            byte[] header = in.readNBytes(8);
            if (header.length != 8) {
                helper.fail("mod logo 'victus.png' has insufficient header bytes: " + header.length);
                return;
            }
            for (int i = 0; i < PNG_MAGIC.length; i++) {
                if (header[i] != PNG_MAGIC[i]) {
                    helper.fail("mod logo 'victus.png' is not a valid PNG (byte " + i + " mismatch: 0x"
                        + String.format("%02X", header[i]) + " vs 0x"
                        + String.format("%02X", PNG_MAGIC[i]) + ")");
                    return;
                }
            }
        } catch (Exception e) {
            helper.fail("Failed to read 'victus.png': " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    // -----------------------------------------------------------------------
    // Test 3: 21 个手写 recipe 资源存在
    // 防止 Phase 6 真相源被误删
    // -----------------------------------------------------------------------
    private static final String[] EXPECTED_RECIPES = new String[] {
        "data/victus/recipe/archery_heart_aspect.json",
        "data/victus/recipe/blazing_heart_aspect.json",
        "data/victus/recipe/broken_heart.json",
        "data/victus/recipe/bundle_heart_aspect.json",
        "data/victus/recipe/cheese_heart_aspect.json",
        "data/victus/recipe/creeper_heart_aspect.json",
        "data/victus/recipe/diamond_heart_aspect.json",
        "data/victus/recipe/draconic_heart_aspect.json",
        "data/victus/recipe/emerald_heart_aspect.json",
        "data/victus/recipe/evoking_heart_aspect.json",
        "data/victus/recipe/golden_heart_aspect.json",
        "data/victus/recipe/grilled_heart_aspect.json",
        "data/victus/recipe/icy_heart_aspect.json",
        "data/victus/recipe/iron_heart_aspect.json",
        "data/victus/recipe/lapis_heart_aspect.json",
        "data/victus/recipe/light_heart_aspect.json",
        "data/victus/recipe/ocean_heart_aspect.json",
        "data/victus/recipe/potion_heart_aspect.json",
        "data/victus/recipe/sweet_heart_aspect.json",
        "data/victus/recipe/totem_heart_aspect.json",
        "data/victus/recipe/void_heart_aspect.json"
    };

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void testRecipeResourcesExist(GameTestHelper helper) {
        if (EXPECTED_RECIPES.length != 21) {
            helper.fail("Test fixture expects 21 recipes but array has " + EXPECTED_RECIPES.length);
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String path : EXPECTED_RECIPES) {
            if (!resourceExists(path)) {
                missing.add(path);
            }
        }
        if (!missing.isEmpty()) {
            helper.fail("Missing recipe resources (" + missing.size() + "): " + missing);
            return;
        }
        helper.succeed();
    }

    // -----------------------------------------------------------------------
    // Test 4: 22 个注册物品纹理存在
    // 防止 Phase 9 清理时误删真实物品纹理
    // -----------------------------------------------------------------------
    private static final String[] EXPECTED_ITEM_TEXTURES = new String[] {
        "assets/victus/textures/item/archery_heart_aspect.png",
        "assets/victus/textures/item/blazing_heart_aspect.png",
        "assets/victus/textures/item/blank_heart_aspect.png",
        "assets/victus/textures/item/broken_heart.png",
        "assets/victus/textures/item/bundle_heart_aspect.png",
        "assets/victus/textures/item/cheese_heart_aspect.png",
        "assets/victus/textures/item/creeper_heart_aspect.png",
        "assets/victus/textures/item/diamond_heart_aspect.png",
        "assets/victus/textures/item/draconic_heart_aspect.png",
        "assets/victus/textures/item/emerald_heart_aspect.png",
        "assets/victus/textures/item/evoking_heart_aspect.png",
        "assets/victus/textures/item/golden_heart_aspect.png",
        "assets/victus/textures/item/grilled_heart_aspect.png",
        "assets/victus/textures/item/icy_heart_aspect.png",
        "assets/victus/textures/item/iron_heart_aspect.png",
        "assets/victus/textures/item/lapis_heart_aspect.png",
        "assets/victus/textures/item/light_heart_aspect.png",
        "assets/victus/textures/item/ocean_heart_aspect.png",
        "assets/victus/textures/item/potion_heart_aspect.png",
        "assets/victus/textures/item/sweet_heart_aspect.png",
        "assets/victus/textures/item/totem_heart_aspect.png",
        "assets/victus/textures/item/void_heart_aspect.png"
    };

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void testItemTexturesExist(GameTestHelper helper) {
        if (EXPECTED_ITEM_TEXTURES.length != 22) {
            helper.fail("Test fixture expects 22 item textures but array has " + EXPECTED_ITEM_TEXTURES.length);
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String path : EXPECTED_ITEM_TEXTURES) {
            if (!resourceExists(path)) {
                missing.add(path);
            }
        }
        if (!missing.isEmpty()) {
            helper.fail("Missing item textures (" + missing.size() + "): " + missing);
            return;
        }
        helper.succeed();
    }

    // -----------------------------------------------------------------------
    // Test 5: 死资源确实不存在
    // 防止死资源被误加回来
    // -----------------------------------------------------------------------
    private static final String[] DEAD_RESOURCES = new String[] {
        "assets/victus/textures/item/victus_journal.png",
        "assets/victus/textures/gui/journal_hearts.png"
    };

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void testDeadResourcesAbsent(GameTestHelper helper) {
        List<String> found = new ArrayList<>();
        for (String path : DEAD_RESOURCES) {
            if (resourceExists(path)) {
                found.add(path);
            }
        }
        if (!found.isEmpty()) {
            helper.fail("Dead resources have reappeared (" + found.size() + "): " + found);
            return;
        }
        helper.succeed();
    }

    // -----------------------------------------------------------------------
    // Test 6: VictusRecipeProvider 类不存在
    // 防止 recipe datagen provider 重新引入
    // -----------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void testVictusRecipeProviderAbsent(GameTestHelper helper) {
        try {
            Class.forName("com.ming.victus.datagen.VictusRecipeProvider", false,
                VictusGameTests.class.getClassLoader());
            helper.fail("Class 'com.ming.victus.datagen.VictusRecipeProvider' must NOT exist; it would re-create the dual-source-of-truth recipe problem.");
        } catch (ClassNotFoundException expected) {
            helper.succeed();
        }
    }

    // -----------------------------------------------------------------------
    // 辅助方法
    // -----------------------------------------------------------------------
    private static boolean resourceExists(String path) {
        return VictusGameTests.class.getClassLoader().getResource(path) != null;
    }
}
