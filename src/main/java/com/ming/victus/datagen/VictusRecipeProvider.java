package com.ming.victus.datagen;

import com.ming.victus.item.VictusItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("null")
public class VictusRecipeProvider extends RecipeProvider {

    public VictusRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(packOutput, registries);
    }

    @Override
    @SuppressWarnings("null")
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, VictusItems.BROKEN_HEART.get())
            .pattern("sbs")
            .pattern("b b")
            .pattern("sbs")
            .define('s', Items.BONE_MEAL)
            .define('b', Items.APPLE)
            .unlockedBy("has_apple", has(Items.APPLE))
            .save(output);

        buildAspectRecipe(output, VictusItems.GRILLED_HEART_ASPECT.get(), Items.COOKED_BEEF, Items.COOKED_BEEF);
        buildAspectRecipe(output, VictusItems.BUNDLE_HEART_ASPECT.get(), Items.GLISTERING_MELON_SLICE, Items.GHAST_TEAR);
        buildAspectRecipe(output, VictusItems.CREEPER_HEART_ASPECT.get(), Items.GUNPOWDER, Items.TNT);
        buildAspectRecipe(output, VictusItems.DIAMOND_HEART_ASPECT.get(), Items.DIAMOND, Items.DIAMOND);
        buildAspectRecipe(output, VictusItems.LIGHT_HEART_ASPECT.get(), Items.PHANTOM_MEMBRANE, Items.FEATHER);
        
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, VictusItems.OCEAN_HEART_ASPECT.get())
            .pattern("hsn")
            .pattern("sus")
            .pattern("nsh")
            .define('s', Items.GLOW_INK_SAC)
            .define('u', VictusItems.BLANK_HEART_ASPECT.get())
            .define('h', Items.HEART_OF_THE_SEA)
            .define('n', Items.NAUTILUS_SHELL)
            .unlockedBy("has_blank_heart", has(VictusItems.BLANK_HEART_ASPECT.get()))
            .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, VictusItems.TOTEM_HEART_ASPECT.get())
            .pattern("sus")
            .pattern("uau")
            .pattern("sus")
            .define('s', Items.END_CRYSTAL)
            .define('u', Items.TOTEM_OF_UNDYING)
            .define('a', VictusItems.BLANK_HEART_ASPECT.get())
            .unlockedBy("has_blank_heart", has(VictusItems.BLANK_HEART_ASPECT.get()))
            .save(output);

        buildAspectRecipe(output, VictusItems.POTION_HEART_ASPECT.get(), Items.GLASS, Items.GLASS);
        buildAspectRecipe(output, VictusItems.ARCHERY_HEART_ASPECT.get(), Items.FLINT, Items.FEATHER);
        buildAspectRecipe(output, VictusItems.BLAZING_HEART_ASPECT.get(), Items.MAGMA_CREAM, Items.BLAZE_ROD);
        
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, VictusItems.DRACONIC_HEART_ASPECT.get())
            .pattern("tst")
            .pattern("sus")
            .pattern("tst")
            .define('s', Items.DRAGON_BREATH)
            .define('u', VictusItems.BLANK_HEART_ASPECT.get())
            .define('t', Items.END_STONE)
            .unlockedBy("has_blank_heart", has(VictusItems.BLANK_HEART_ASPECT.get()))
            .save(output);

        buildAspectRecipe(output, VictusItems.EMERALD_HEART_ASPECT.get(), Items.EMERALD, Items.EMERALD);
        
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, VictusItems.EVOKING_HEART_ASPECT.get())
            .pattern("tst")
            .pattern("sus")
            .pattern("tst")
            .define('s', Items.TOTEM_OF_UNDYING)
            .define('u', VictusItems.BLANK_HEART_ASPECT.get())
            .define('t', Items.EMERALD)
            .unlockedBy("has_blank_heart", has(VictusItems.BLANK_HEART_ASPECT.get()))
            .save(output);

        buildAspectRecipe(output, VictusItems.GOLDEN_HEART_ASPECT.get(), Items.GOLDEN_APPLE, Items.GOLD_INGOT);
        buildAspectRecipe(output, VictusItems.ICY_HEART_ASPECT.get(), Items.BLUE_ICE, Items.PACKED_ICE);
        buildAspectRecipe(output, VictusItems.IRON_HEART_ASPECT.get(), Items.IRON_BLOCK, Items.IRON_BLOCK);
        buildAspectRecipe(output, VictusItems.LAPIS_HEART_ASPECT.get(), Items.EXPERIENCE_BOTTLE, Items.LAPIS_LAZULI);
        buildAspectRecipe(output, VictusItems.SWEET_HEART_ASPECT.get(), Items.SUGAR, Items.SUGAR);
        buildAspectRecipe(output, VictusItems.CHEESE_HEART_ASPECT.get(), Items.MILK_BUCKET, Items.BROWN_MUSHROOM);
        buildAspectRecipe(output, VictusItems.VOID_HEART_ASPECT.get(), Items.CHORUS_FRUIT, Items.END_STONE);
    }

    private void buildAspectRecipe(RecipeOutput output, ItemLike result, ItemLike inner, ItemLike outer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
            .pattern("tst")
            .pattern("sus")
            .pattern("tst")
            .define('s', inner)
            .define('u', VictusItems.BLANK_HEART_ASPECT.get())
            .define('t', outer)
            .unlockedBy("has_blank_heart", has(VictusItems.BLANK_HEART_ASPECT.get()))
            .save(output);
    }
}