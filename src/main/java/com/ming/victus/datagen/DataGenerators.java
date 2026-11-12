package com.ming.victus.datagen;

import com.ming.victus.VictusMain;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = VictusMain.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
@SuppressWarnings("removal")
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // Phase 6 决策：以 src/main/resources/data/victus/recipe/*.json 手写 JSON 为唯一配方真相源。
        // VictusRecipeProvider 不再注册，避免 runData 时生成 recipe 输出并与手写 JSON 形成双源真相。
        generator.addProvider(
            event.includeClient(),
            new VictusItemModelProvider(packOutput, existingFileHelper)
        );
    }
}
