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

        generator.addProvider(
            event.includeClient(),
            new VictusItemModelProvider(packOutput, existingFileHelper)
        );

        generator.addProvider(
            event.includeServer(),
            new VictusRecipeProvider(packOutput, event.getLookupProvider())
        );
    }
}
