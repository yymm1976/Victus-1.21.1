import json
import os

with open('src/main/resources/assets/victus/lang/zh_cn.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for k, v in data.items():
    if "之心" in v and not v.endswith("之心"):
        pass # description
    elif k.endswith("_heart_aspect"):
        name = v.replace("心形切片", "之心").replace("心", "之心")
        # Some are like 钻石 -> 钻石之心
        if not name.endswith("之心"):
            name = name + "之心"
        if "之心之心" in name:
            name = name.replace("之心之心", "之心")
        data[k] = name

data["item.victus.grilled_heart_aspect"] = "烤肉之心"
data["item.victus.creeper_heart_aspect"] = "苦力怕之心"
data["item.victus.diamond_heart_aspect"] = "钻石之心"
data["item.victus.light_heart_aspect"] = "轻灵之心"
data["item.victus.evoking_heart_aspect"] = "唤魔之心"
data["item.victus.archery_heart_aspect"] = "箭术之心"
data["item.victus.ocean_heart_aspect"] = "海洋之心"
data["item.victus.draconic_heart_aspect"] = "巨龙之心"
data["item.victus.icy_heart_aspect"] = "冰霜之心"
data["item.victus.potion_heart_aspect"] = "药水之心"
data["item.victus.lapis_heart_aspect"] = "青金石之心"
data["item.victus.void_heart_aspect"] = "虚空之心"
data["item.victus.sweet_heart_aspect"] = "甜蜜之心"

data["item.victus.grilled_heart_aspect.description"] = "破碎时直接恢复饥饿值与饱和度。"
data["item.victus.creeper_heart_aspect.description"] = "破碎时产生爆炸，但不会伤及自身。"
data["item.victus.light_heart_aspect.description"] = "破碎时免疫本次坠落伤害。"
data["item.victus.evoking_heart_aspect.description"] = "破碎时召唤一个持剑恼鬼为你作战。"
data["item.victus.archery_heart_aspect.description"] = "破碎时向附近发射5根带有击退效果的瞬间伤害药水箭。"
data["item.victus.ocean_heart_aspect.description"] = "在水中时扣除充能，提供水下呼吸、海豚的恩赐和潮涌能量。"
data["item.victus.draconic_heart_aspect.description"] = "破碎时在原地生成不会伤害你的龙息滞留云。"
data["item.victus.icy_heart_aspect.description"] = "破碎时给予周围敌人缓慢效果。"
data["item.victus.potion_heart_aspect.description"] = "饮下药水时将其存储，破碎时释放储存的药水效果。"
data["item.victus.lapis_heart_aspect.description"] = "未被消耗时，吸收经验球来恢复生命值。"
data["item.victus.sweet_heart_aspect.description"] = "破碎时提供速度与急迫效果。"

data["effect.victus.bleeding_heart"] = "碎心"
data["item.minecraft.potion.effect.bleeding_heart"] = "碎心药水"
data["item.minecraft.splash_potion.effect.bleeding_heart"] = "喷溅型碎心药水"
data["item.minecraft.lingering_potion.effect.bleeding_heart"] = "滞留型碎心药水"

with open('src/main/resources/assets/victus/lang/zh_cn.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

with open('src/main/resources/assets/victus/lang/en_us.json', 'r', encoding='utf-8') as f:
    en_data = json.load(f)

en_data["item.victus.grilled_heart_aspect.description"] = "Directly restores hunger and saturation when broken."
en_data["item.victus.creeper_heart_aspect.description"] = "Creates an explosion when broken, without harming yourself."
en_data["item.victus.light_heart_aspect.description"] = "Completely negates fall damage when broken."
en_data["item.victus.evoking_heart_aspect.description"] = "Summons a tamed Vex with a sword when broken."
en_data["item.victus.archery_heart_aspect.description"] = "Shoots 5 Harming II tipped arrows with knockback when broken."
en_data["item.victus.ocean_heart_aspect.description"] = "Consumes charge while in water to grant Water Breathing, Dolphin's Grace, and Conduit Power."
en_data["item.victus.draconic_heart_aspect.description"] = "Spawns a safe dragon breath cloud when broken."
en_data["item.victus.icy_heart_aspect.description"] = "Grants Slowness to surrounding enemies when broken."
en_data["item.victus.potion_heart_aspect.description"] = "Stores a potion effect when drinking, and releases it when broken."
en_data["item.victus.lapis_heart_aspect.description"] = "Consumes experience orbs to restore health when active."
en_data["item.victus.sweet_heart_aspect.description"] = "Grants Speed and Haste when broken."

en_data["effect.victus.bleeding_heart"] = "Bleeding Heart"
en_data["item.minecraft.potion.effect.bleeding_heart"] = "Potion of the Bleeding Heart"
en_data["item.minecraft.splash_potion.effect.bleeding_heart"] = "Splash Potion of the Bleeding Heart"
en_data["item.minecraft.lingering_potion.effect.bleeding_heart"] = "Lingering Potion of the Bleeding Heart"

with open('src/main/resources/assets/victus/lang/en_us.json', 'w', encoding='utf-8') as f:
    json.dump(en_data, f, ensure_ascii=False, indent=2)
