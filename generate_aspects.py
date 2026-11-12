import os

aspects = [
    "GrilledAspect", "BundleAspect", "CreeperAspect", "DiamondAspect", 
    "LightAspect", "OceanAspect", "TotemAspect", "PotionAspect", 
    "ArcheryAspect", "BlazingAspect", "DraconicAspect", "EmeraldAspect", 
    "EvokingAspect", "GoldenAspect", "IcyAspect", "IronAspect", 
    "LapisAspect", "SweetAspect", "CheeseAspect"
]

template = """package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class {name} extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "{id}"),
        0, // textureIndex placeholder
        60, // standardRechargeDuration placeholder
        0xFFFFFF, // color placeholder
        HeartAspect.NEVER_UPDATE,
        {name}::new
    );

    public {name}(Player player) {
        super(player, TYPE);
    }
}
"""

directory = "src/main/java/com/ming/victus/hearts/content"
if not os.path.exists(directory):
    os.makedirs(directory)

for aspect in aspects:
    id_name = aspect.replace("Aspect", "").lower()
    filepath = os.path.join(directory, f"{aspect}.java")
    with open(filepath, "w") as f:
        f.write(template.format(name=aspect, id=id_name))

print("Created skeleton classes.")
