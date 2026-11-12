package com.ming.victus.datagen;

import com.ming.victus.VictusMain;
import com.ming.victus.item.VictusItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class VictusItemModelProvider extends ItemModelProvider {

    public VictusItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, VictusMain.MOD_ID, existingFileHelper);
    }

    @Override
    @SuppressWarnings("null")
    protected void registerModels() {
        VictusItems.ITEMS.getEntries().forEach(item -> {
            String name = item.getId().getPath();
            basicItem(name);
        });
    }

    @SuppressWarnings("null")
    private void basicItem(String name) {
        withExistingParent(name, "item/generated").texture("layer0", "item/" + name);
    }
}
