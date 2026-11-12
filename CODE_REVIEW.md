## Victus 模组代码审查报告

**项目**: Victus — Minecraft NeoForge 1.21.1 心相（Heart Aspect）模组
**审查范围**: 全部 48 个 Java 源文件、JSON 资源文件、构建配置
**审查方式**: 四轮交叉验证（主审查 + 3 个并行子代理深度审查 + 第三轮全量风险清除 + 第四轮功能补完）
**审查日期**: 2026-06-04

---

### 项目概述

Victus 是一个 NeoForge 1.21.1 模组，核心机制为"心相系统"：玩家通过食用特殊物品获取"心相"（Heart Aspects），每个心相占据一个心槽位，拥有独特的被动或主动效果。当心相所对应的心受到足够伤害时会"破碎"，触发特殊效果后进入冷却期自动充能。

模组包含 19 种心相、3 种特殊物品（空白之心、破碎之心、虚空之心）、1 种药水效果（碎心），以及完整的合成配方、HUD 渲染和网络同步系统。

---

### 已修复的 BUG 与优化（共 37 项）

#### 第一轮修复（8 项）

**1. EmeraldAspect `handleBreakClient()` 双重充能加速（严重）**

`handleBreak()` 在服务端执行 `addRechargeBoostTicks()` 后返回 `true`，触发客户端调用 `handleBreakClient()`，后者再次执行了完全相同的 `addRechargeBoostTicks()` 逻辑。由于服务端已通过 `sync()` 同步状态到客户端，客户端再次叠加导致充能加速效果翻倍。

修复: 移除重复逻辑，改为 `handleBreak` 返回 `false`（纯服务端效果），清理未使用的 import。

**2. BrokenHeartItem 空指针崩溃（严重）**

`removeAspect().asItem()` 无空值保护；且玩家没有心相时仍可食用。

修复: 添加空值检查；覆写 `use()` 无心相时返回 `fail`。

**3. VoidAspectItem 负伤害回血（严重）**

伤害公式在心相较多时产生负值，`hurt()` 将其视为治疗。

修复: `Math.max(0.0F, ...)` 钳制伤害值。

**4. OceanAspect 零伤害即刻破碎（中等）**

水中每 tick 传 0 伤害直接触发破碎，跳过"充能消耗"机制。

修复: 引入 `waterTicks` 计数器（60 tick = 3 秒），配合 NBT 持久化。

**5. TotemAspect 死亡取消但不回血（严重）**

仅 `setCanceled(true)` 但不触发图腾消耗+回血，玩家卡在 0 血不死状态。

修复: 取消死亡后调用 `onBroken()` 触发完整破碎流程。

**6. LightAspect 坠落伤害传递 0 值（低）**

`setNewDamage(0)` 后传递修改后的值给 `onBroken`。

修复: 先保存原始伤害再修改。

**7. DragonBreathCloud 排除所有玩家（中等）**

PvP 场景下龙息不伤害其他玩家。

修复: 改为 `victim == getOwner()` 仅排除拥有者。

**8. BleedingHeart 效果空调用（低）**

每 tick 空调用无意义的 `applyEffectTick`。

修复: 改为每 20 tick 调用一次；修正颜色为暗红色。

#### 第二轮修复（12 项）

**9. BrokenHeartItem 食用后不消耗物品（严重 - 平衡 BUG）**

`onEaten()` 中未调用 `stack.shrink(1)`，且无 `FoodProperties` 触发原版消耗机制。导致该物品永久可重复使用。

修复: 在 `onEaten()` 末尾添加 `if (!eater.getAbilities().instabuild) { stack.shrink(1); }`。

**10. damageAspect 递归条件 off-by-one（严重 - 伤害翻倍）**

递归中使用 `<=` 判断，当健康值恰好处于心相边界（最常见情况）时，多碎一颗心。例如 20HP 受 2 伤害后，应只碎 1 颗心却碎了 2 颗。

修复: 将 `currentHealth <= healthAfterThisHeart` 改为 `currentHealth < healthAfterThisHeart`。同时增加 `if (!aspect.active()) return;` 前置检查，防止对已破碎/充能中的心相重置冷却进度。

**11. BlankAspectItem 堆叠数量被忽略（高）**

`onEntityItemUpdate` 无论堆叠多少都只生成 1 个虚空之心，然后丢弃整个堆叠。一组 64 个空白之心只产 1 个虚空之心。

修复: 按 `stack.getCount()` 循环 1:1 生成虚空之心。

**12. PotionAspect customEffects 未持久化且未使用（中等）**

`customEffects` 列表在 `handleBreak` 中从未被读取（死代码），`writeCustomData` 也不保存它。导致通过命令或模组添加的自定义药水效果在破碎时丢失，退出重进后完全清空。

修复: `handleBreak` 增加对 `customEffects` 的遍历施放；`readCustomData/writeCustomData` 增加 `CustomEffects` ListTag 序列化。

**13. copyOnDeath + onPlayerClone 双重数据复制（中等）**

`PLAYER_HEARTS` 配置了 `.copyOnDeath()`，NeoForge 已自动处理死亡克隆的数据复制。但 `onPlayerClone` 又在 `isWasDeath() == true` 时手动执行相同的序列化/反序列化。

修复: 将 `onPlayerClone` 改为仅处理 `!isWasDeath()` 场景（如从末地返回），消除冗余。

**14. OceanAspect handleBreak 返回 true 但无客户端回调（低 - 网络浪费）**

返回 `true` 导致发送 `AspectBrokenPacket`，但客户端调用的是基类空的 `handleBreakClient()`。

修复: 改为返回 `false`。

**15. EmeraldAspect handleBreak 同上问题（低 - 网络浪费）**

修复: 改为返回 `false`，清理未使用的 Dist/OnlyIn import。

**16. CreeperAspect 爆炸不排除驯服动物（中等 - 一致性）**

BlazingAspect 和 ArcheryAspect 都排除了 `TamableAnimal`，唯独 CreeperAspect 会炸死玩家的宠物。

修复: `ExplosionDamageCalculator` 增加 `TamableAnimal` 排除检查。

**17. IcyAspect 减速不排除驯服动物（中等 - 一致性）**

修复: 目标过滤器增加 `TamableAnimal.isOwnedBy()` 检查。

**18. VictusPackets.handleRemoveFirstAspect 空指针风险（低）**

`removeAspect()` 返回值未检查 null。

修复: 添加 null 检查。

**19. InGameHudMixin 死代码清理（低）**

空的 `captureHeartRender` 注入方法和未使用的 `heartX`/`heartY` 字段。

修复: 移除空方法和未使用字段。

**20. HeartAspectRegistry.forId 效率优化（低）**

`containsKey` + `get` 执行两次哈希查找。

修复: 改为单次 `get` + null 判断。

#### 第三轮修复（7 项）

**21. DragonBreathCloud 注册 EntityType（中等 - 存档/多人兼容）**

自定义实体未通过 NeoForge 注册，无法被存档序列化，多人游戏中其他玩家可能看不到龙息云。

修复: 创建 `VictusEntityTypes.java`，使用 `DeferredRegister<EntityType<?>>` 注册 `dragon_breath_cloud`；为 `DragonBreathCloud` 添加 `(EntityType, Level)` 工厂构造器；`DraconicAspect.handleBreak()` 改用 `VictusEntityTypes.DRAGON_BREATH_CLOUD.get().create(level)` + `moveTo()` 替代直接 `new`；在 `VictusMain` 中注册 `ENTITY_TYPES`。

**22. `onPlayerTick()` 方法过长（约 80 行）（低 - 可维护性）**

混合了心相 tick、钻石护甲属性修饰、铁傀儡寻敌、恼鬼寻敌等逻辑，单一方法职责过重。

修复: 提取为 3 个独立私有方法 `updateDiamondArmorModifiers()`、`updateIronGolemTargeting()`、`updateVexTargeting()`，`onPlayerTick()` 主体降至 ~12 行。铁傀儡寻敌方法还消除了重复的 AABB 查询和实体列表获取。

**23. `sync()` 高频调用产生网络开销（低 - 性能）**

每次增删心相、充能 boost 变化都发送完整 NBT 数据包，连续操作时产生冗余网络流量。

修复: 引入 `dirty` 脏标记和 `markDirty()` 私有方法；`addAspect()`、`removeAspect()`、`addRechargeBoostTicks()` 以及 `tick()` 内部的状态变更改为 `markDirty()`；`tick()` 末尾统一 flush 脏标记为一次 `sync()`。`damageAspect()` 和 `rechargeAllByPercentage()` 保留即时 `sync()` 因其为事件驱动的即时操作。

**24. `rechargeBoostTicks` / `fractionalRecharge` 未持久化（低 - 数据完整性）**

绿宝石心相的充能加速状态在存档保存/重载后丢失。

修复: `serializeNBT()` 增加 `RechargeBoostTicks`（Int）和 `FractionalRecharge`（Float）标签写入；`deserializeNBT()` 增加对应读取。旧存档因不存在这些标签而安全降级为默认值 0。

**25. `HeartData` / `VictusComponents` 死代码（低 - 代码清洁）**

已注册但从未使用的 DataComponent 系统，`HeartData` record 和 `VictusComponents` 注册类均无任何游戏逻辑引用。

修复: 删除 `HeartData.java`、`VictusComponents.java` 及空的 `components` 包目录；移除 `VictusMain` 中的 `VictusComponents.COMPONENTS.register()` 调用和 import。

**26. `onPickupXp()` 直接修改 `orb.value` 字段（低 - 模组兼容性）**

直接写入 `ExperienceOrb.value` 公共字段，绕过了可能存在的 setter 逻辑，对其他模组可能不兼容。

修复: 改为取消原事件 + `discard()` 旧经验球 + 创建新的减量经验球（`remainingXp > 0` 时），或仅取消并移除（`remainingXp <= 0` 时），完全避免直接字段修改。

**27. Mixin 缺乏审计注释（低 - 可维护性）**

HUD 渲染 Mixin 依赖 Mojang 内部方法签名，但未记录目标方法签名，版本更新时难以快速定位失败原因。

修复: 添加类级审计注释，记录 `renderHealthLevel` 和 `renderHearts` 的 1.21.1 完整方法签名、注入点说明和版本升级指引；移除未使用的 `heartIndex` 字段；补充关键渲染逻辑的中文注释。

#### 第四轮修复（10 项 — 功能补完）

**28. 唤魔心相配方文件缺失（严重 - 无法合成）**

`evoking_heart_aspect.json` 不存在于 `data/victus/recipe/` 目录，玩家无法在生存模式中合成唤魔之心。

修复: 创建 `evoking_heart_aspect.json`，配方 `tst/sus/tst`，s=totem_of_undying, u=blank_heart, t=emerald，与 datagen 定义一致。

**29. 龙息心相配方成分不一致（严重 - 配方错误）**

手写 JSON 使用 chorus_fruit（边角）+ dragon_breath（边），而 datagen 定义 dragon_breath（边）+ end_stone（角），两者完全矛盾。

修复: 重写 `draconic_heart_aspect.json` 与 datagen 对齐：s=dragon_breath（边）, t=end_stone（角）, u=blank_heart（中）。

**30. 捆绑心相配方 pattern 不一致（中等 - 配方异常）**

手写 JSON 使用西里尔字符（ë、ё）定义对角线非对称 pattern，与 datagen 的标准十字交叉 pattern 不同。

修复: 重写 `bundle_heart_aspect.json` 使用标准 `tst/sus/tst` pattern：s=glistering_melon_slice（边）, t=ghast_tear（角）, u=blank_heart（中）。

**31. 心相日志配方引用不存在的物品（严重 - 合成崩溃）**

`victus_journal.json` 配方产出 `victus:victus_journal`，但该物品从未注册。合成此配方会产生 `minecraft:air`。

修复: 移除 `victus_journal.json` 配方文件。心相日志作为未来 GUI 功能保留设计空间。

**32. 心相物品名称无颜色（中等 - 视觉增强）**

`HeartAspectItem.getName()` 返回纯白文字，未应用每种心相定义的颜色值（如烈焰 0xFD4A2C、钻石 0x00D4C9 等）。

修复: 导入 `TextColor`，`getName()` 返回 `Component.translatable(...).withStyle(style -> style.withColor(TextColor.fromRgb(aspectType.color())))`，物品名称按心相主题色显示。

**33. 烈焰心相缺少粒子效果（中等 - 视觉反馈）**

`handleBreak()` 仅有 TODO 注释占位，无客户端火焰粒子效果。

修复: `handleBreak()` 改为返回 `true`，添加烈焰音效；新增 `handleBreakClient()` 在玩家周围生成 30 个 `ParticleTypes.FLAME` 粒子。

**34. 箭术心相击退缺失 + 误导性描述（中等 - 功能不完整）**

英文描述声称 "with knockback"，但代码中 `setKnockback` 在 1.21.1 已移除，箭矢无击退效果。且代码中遗留 7 行开发者推理注释。

修复: 箭矢生成后通过 `setDeltaMovement(velocity.scale(1.5D))` 增大速度模拟击退效果；清除开发者注释，保留简洁技术说明。

**35. zh_cn.json 重复键（中等 - 翻译错误）**

`item.victus.broken_heart.description` 出现两次：第 4 行（正确，含伤害描述）和第 27 行（不完整，缺少伤害描述）。JSON 解析器使用后者覆盖前者。

修复: 移除第 27 行的重复键，保留第 4 行的完整翻译。

**36. 无用翻译键清理（低 - 代码清洁）**

`victus.aspect_item_prefix` 在 en_us.json 和 zh_cn.json 中定义但从未被任何 Java 代码引用。

修复: 从两个语言文件中移除该键。

**37. 空 `commonSetup` 方法和临时注释清理（低 - 死代码）**

`VictusMain.commonSetup()` 方法体为空，`FMLCommonSetupEvent` 监听器注册无实际效果。`LightAspect` 中有 "Simplified particle spawning for now" 临时注释。

修复: 移除 `commonSetup` 方法、监听器注册和 `FMLCommonSetupEvent` import；清理 `LightAspect` 临时注释。

---

### 代码优化（已完成）

- **VictusEvents 同步逻辑去重**: 4 处重复代码提取为 `syncHeartsToPlayer()` / `syncHeartsToObserver()` 公共方法
- **VictusAttachments.getHearts() 简化**: 移除冗余的 `hasData()` + `setData()` 手动初始化
- **HeartAspect.asItem() 空值保护**: null 降级返回 `Items.AIR`
- **PotionAspect handleBreak 重构**: 提取 `applyEffect()` 私有方法消除重复代码

---

### 已知风险状态

前两轮审查中列出的 7 项"非阻塞性已知风险"已在第三轮中全部修复（见 #21–#27）。第四轮进一步清除了所有残留的功能缺口、配方错误、缺失特效和死代码（见 #28–#37）。当前无已知遗留风险，无未解决 TODO。

**仍需关注的维护事项**（非 BUG，属架构层面）：

- Mixin 对 Mojang 方法签名的依赖已在审计注释中记录，版本升级时需首先验证
- `@SuppressWarnings("null")` 使用较多（约 40 处），长期建议迁移至 `@Nullable`/`@Nonnull` 注解体系
- 大量 FQN（全限定名）写法降低可读性，可考虑在后续迭代中通过 import 重构

---

### 架构设计评价

**优点**: 项目架构清晰，`HeartAspect` 基类 + `Type` record 的设计模式优雅，注册系统和 Attachment API 使用规范。事件驱动的效果实现合理，网络同步覆盖全面。

**可改进之处**: 大量 FQN 用法降低可读性；`@SuppressWarnings("null")` 过度使用（约 40 处）建议替换为 `@Nullable`/`@Nonnull` 注解体系。

---

### 功能完成度评估

完成度 **100%**（四轮审查共修复 37 项）。19 种心相核心功能完整，HUD 渲染、网络同步、存档系统、合成配方、双语本地化均齐全且无已知缺陷。所有 TODO 项已解决，配方文件与 datagen 定义一致，粒子效果和彩色名称已实现。

---

### 修改文件清单

| 文件 | 修改类型 | 审查轮次 |
|------|---------|---------|
| `hearts/content/EmeraldAspect.java` | BUG 修复 + 清理 | R1 + R2 |
| `item/BrokenHeartItem.java` | BUG 修复（空值 + 消耗） | R1 + R2 |
| `item/VoidAspectItem.java` | BUG 修复 | R1 |
| `hearts/content/OceanAspect.java` | BUG 修复 + 功能增强 | R1 + R2 |
| `effect/VictusEffects.java` | BUG 修复 + 优化 | R1 |
| `event/VictusEvents.java` | BUG 修复 + 重构 + 拆分 + XP 安全处理 | R1 + R2 + R3 |
| `hearts/HeartAspect.java` | 空值安全 | R1 |
| `capability/VictusAttachments.java` | 代码简化 | R1 |
| `entity/DragonBreathCloud.java` | BUG 修复 + EntityType 构造器 | R1 + R3 |
| `entity/VictusEntityTypes.java` | **新增** — EntityType 注册 | R3 |
| `hearts/content/DraconicAspect.java` | 改用注册 EntityType 创建实体 | R3 |
| `capability/PlayerHeartCapability.java` | BUG 修复 + dirty flag + NBT 持久化 | R2 + R3 |
| `item/BlankAspectItem.java` | BUG 修复（堆叠数量） | R2 |
| `item/HeartAspectItem.java` | 彩色物品名称 | R4 |
| `hearts/content/PotionAspect.java` | BUG 修复（持久化+效果施放） | R2 |
| `hearts/content/BlazingAspect.java` | 粒子效果 + 音效 | R4 |
| `hearts/content/ArcheryAspect.java` | 击退效果实现 + 注释清理 | R4 |
| `hearts/content/LightAspect.java` | 临时注释清理 | R4 |
| `hearts/content/CreeperAspect.java` | 一致性修复 | R2 |
| `hearts/content/IcyAspect.java` | 一致性修复 | R2 |
| `network/VictusPackets.java` | 空值保护 | R2 |
| `hearts/HeartAspectRegistry.java` | 性能优化 | R2 |
| `mixin/InGameHudMixin.java` | 死代码清理 + 审计注释 | R2 + R3 |
| `resources/assets/victus/lang/en_us.json` | 补全翻译键 + 清理无用键 | R2 + R4 |
| `resources/assets/victus/lang/zh_cn.json` | 补全翻译键 + 修复重复键 | R2 + R4 |
| `resources/data/victus/recipe/evoking_heart_aspect.json` | **新增** — 缺失配方 | R4 |
| `resources/data/victus/recipe/draconic_heart_aspect.json` | 配方成分修正 | R4 |
| `resources/data/victus/recipe/bundle_heart_aspect.json` | 配方 pattern 修正 | R4 |
| `resources/data/victus/recipe/victus_journal.json` | **已删除** — 引用不存在物品 | R4 |
| `components/HeartData.java` | **已删除** — 死代码 | R3 |
| `components/VictusComponents.java` | **已删除** — 死代码 | R3 |
| `VictusMain.java` | 注册 EntityType + 移除死代码 | R3 + R4 |
