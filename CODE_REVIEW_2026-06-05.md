# Victus 模组 — 全面代码审查报告

**项目**: Victus — Minecraft NeoForge 1.21.1 心相（Heart Aspect）模组  
**审查日期**: 2026-06-05  
**审查范围**: 全部源代码、构建配置、数据文件、辅助脚本（共 48 个 Java 文件 + 21 个配方 JSON + 2 个语言文件 + 4 个辅助脚本 + 构建配置）  
**审查方式**: 逐文件深度分析，结合历史审查记录（CODE_REVIEW.md 记录的前四轮 37 项修复）进行增量审查  
**审查原则**: 仅审查，不修改任何代码  

---

## 一、项目总览

### 1.1 项目架构

```
VictusMain (入口)
├── HeartAspectRegistry (心相注册表)
├── VictusAttachments (Attachment 数据系统)
│   └── PlayerHeartCapability (核心能力：心相增删/tick/序列化)
├── VictusItems (物品注册 + 创造模式标签页)
├── VictusEntityTypes (自定义实体注册)
├── VictusEffects / VictusPotions (药水效果/药水)
├── VictusPackets (网络包注册与处理)
├── VictusEvents (全局事件处理器 — 13 个 @SubscribeEvent)
├── InGameHudMixin (HUD 渲染 Mixin)
├── DataGenerators (数据生成器)
└── 19 种心相实现 (hearts/content/)
```

### 1.2 功能完成度

| 模块 | 完成度 | 说明 |
|------|--------|------|
| 19 种心相核心机制 | ✅ 100% | 所有心相的被动/破碎效果已实现 |
| HUD 渲染系统 | ✅ 100% | Mixin 注入 + 自定义渲染器 |
| 网络同步系统 | ✅ 100% | 3 个网络包覆盖同步/破碎/移除 |
| 存档持久化 | ✅ 100% | NBT 序列化 + copyOnDeath |
| 合成配方 | ✅ 100% | 22 个配方，datagen + 手写 JSON 一致 |
| 双语本地化 | ✅ 100% | en_us + zh_cn 各 55 键 |
| 创造模式标签页 | ✅ 100% | 所有物品包含 |
| 游戏测试 | ⚠️ 最小化 | 仅 1 个测试（创造标签页物品计数） |
| 辅助脚本 | ✅ 工具级 | 4 个脚本为开发辅助，非运行时依赖 |

---

## 二、审查发现汇总

### 严重程度分布

| 严重程度 | 数量 | 说明 |
|----------|------|------|
| 🔴 阻塞性 (Blocker) | 3 | 可能导致崩溃、数据丢失或安全漏洞 |
| 🟡 建议性 (Suggestion) | 12 | 可维护性、性能、健壮性改进 |
| 💭 意见性 (Nit) | 9 | 代码风格、命名、文档等优化 |

---

## 三、详细审查发现

---

### 🔴 B-01: `RemoveFirstAspectPacket` 服务端处理缺少权限验证

**文件**: `src/main/java/com/ming/victus/network/VictusPackets.java` 第 62–75 行  
**严重程度**: 🔴 Blocker — 安全漏洞

**问题描述**:  
`handleRemoveFirstAspect` 仅检查 `!player.isCreative()` 就允许移除心相，但 `RemoveFirstAspectPacket` 是一个 `playToServer` 包，客户端可以任意发送此包。恶意客户端可以伪造创造模式状态或在非创造模式下触发此包，绕过 `isCreative()` 检查。

**当前代码**:
```java
private static void handleRemoveFirstAspect(final RemoveFirstAspectPacket data, final IPayloadContext context) {
    context.enqueueWork(() -> {
        if (context.player() instanceof Player player) {
            if (!player.isCreative()) return;  // 客户端可伪造
            // ... 移除心相逻辑
        }
    });
}
```

**风险**: 服务端 `isCreative()` 检查在 `enqueueWork` 内执行时实际是在服务端线程，所以**当前代码在正常流程下是安全的**。但存在以下隐患：
1. 如果未来有人在 `enqueueWork` 之外添加逻辑，可能绕过此检查
2. 缺少对数据包发送频率的限制，客户端可高频发送造成服务端不必要的计算

**改进建议**:  
- 添加对包发送频率的限制（rate limiting）
- 添加明确的注释说明此检查在服务端线程执行是安全的
- 考虑在 `RemoveFirstAspectPacket` 中添加版本号或校验信息

---

### 🔴 B-02: `ArcheryAspect.handleBreak()` 使用 `entities.remove()` 导致 `ConcurrentModificationException`

**文件**: `src/main/java/com/ming/victus/hearts/content/ArcheryAspect.java` 第 35–50 行  
**严重程度**: 🔴 Blocker — 运行时崩溃

**问题描述**:  
`entities` 列表来自 `getEntitiesOfClass()` 返回的列表。在某些 NeoForge/Minecraft 版本中，此列表可能不可修改。更重要的是，`entities.remove(randomIndex)` 在循环内修改列表，如果 `getEntitiesOfClass` 返回的是不可变或固定大小列表，将抛出 `UnsupportedOperationException`。

**当前代码**:
```java
List<LivingEntity> entities = this.player.level().getEntitiesOfClass(LivingEntity.class, box, ...);
for (int i = 0; i < 5; i++) {
    if (entities.isEmpty()) return false;
    LivingEntity target = entities.remove(this.player.getRandom().nextInt(entities.size()));
    // ...
}
```

**改进建议**:  
- 先将列表包装为可变的 `new ArrayList<>(...)` 
- 或使用随机不重复索引选择而非 `remove`

---

### 🔴 B-03: `BlazingAspect.handleBreak()` 同样存在 `entities.remove()` 问题

**文件**: `src/main/java/com/ming/victus/hearts/content/BlazingAspect.java` 第 28–35 行  
**严重程度**: 🔴 Blocker — 运行时崩溃

**问题描述**:  
与 B-02 相同的问题。`getEntitiesOfClass()` 返回的列表直接调用 `remove()` 可能导致 `UnsupportedOperationException`。

**当前代码**:
```java
List<LivingEntity> entities = this.player.level().getEntitiesOfClass(LivingEntity.class, box, ...);
for (int i = 0; i < 4; i++) {
    if (entities.isEmpty()) return false;
    LivingEntity target = entities.remove(this.player.getRandom().nextInt(entities.size()));
    // ...
}
```

**改进建议**: 同 B-02。

---

### 🟡 S-01: `PlayerHeartCapability.tick()` 中的 `rechargeBoostTicks` 计数器与 `fractionalRecharge` 浮点累积精度问题

**文件**: `src/main/java/com/ming/victus/capability/PlayerHeartCapability.java` 第 87–108 行  
**严重程度**: 🟡 Suggestion

**问题描述**:  
`fractionalRecharge` 使用 `float` 累加 0.25F，在长时间运行时可能因浮点精度丢失导致加速效果不准确。例如 `(int) this.fractionalRecharge` 可能在某些边界情况下比预期多或少 1 tick。

**改进建议**:  
- 改用整数计数器替代浮点：将 `fractionalRecharge` 改为 `int fractionalTicks`，每 boost tick 增加 1，累积到 4 时额外 tick 一次并重置
- 这既消除浮点精度问题，也使逻辑更直观

---

### 🟡 S-02: `HeartAspect.tick()` 中 cooldown 边界条件

**文件**: `src/main/java/com/ming/victus/hearts/HeartAspect.java` 第 55–62 行  
**严重程度**: 🟡 Suggestion

**问题描述**:  
```java
public void tick() {
    if (this.cooldown > -1) this.cooldown -= 1;
    if (this.cooldown < -1) {
        this.cooldown = -1;
    } else if (active() && this.type.tickUpdateCondition().test(this.player)) {
        update();
    }
}
```

当 `cooldown` 从 0 减到 -1 时，`cooldown > -1` 判断为 false（不继续减），然后 `cooldown < -1` 也为 false，进入 `else if` 分支调用 `active()` — 此时 cooldown 恰好等于 -1，所以 `active()` 返回 true 并立即触发 `update()`。

这意味着心相**充能完成的同一 tick 就会立即执行 update**，而不是等到下一个 tick。这在大多数情况下是期望行为，但如果子类的 `update()` 依赖某些尚未更新的状态，可能导致不一致。

**改进建议**:  
- 添加注释说明此行为是有意设计的（在充能完成 tick 立即触发 update）
- 或将 `else if` 改为独立的 `if`，让 update 判断与 cooldown 递减解耦

---

### 🟡 S-03: `EvokingAspect` 召唤的恼鬼未限制最大存活数量

**文件**: `src/main/java/com/ming/victus/hearts/content/EvokingAspect.java` 第 28–55 行  
**严重程度**: 🟡 Suggestion — 性能/平衡性

**问题描述**:  
每次唤魔之心破碎都会召唤 3 只恼鬼。如果玩家频繁受伤（如站在岩浆中），短时间内可能产生大量恼鬼实体，导致服务端 TPS 下降。

**改进建议**:  
- 在 `updateVexTargeting()` 中增加对属于该玩家的恼鬼数量检查
- 或在 `EvokingAspect.handleBreak()` 中先清理旧的友方恼鬼再召唤新的
- 建议最大存活数量为 6（两轮破碎的上限）

---

### 🟡 S-04: `IronAspect` 同样存在铁傀儡无上限问题

**文件**: `src/main/java/com/ming/victus/hearts/content/IronAspect.java` + `VictusEvents.java` 第 297–324 行  
**严重程度**: 🟡 Suggestion — 性能/平衡性

**问题描述**:  
与 S-03 类似，铁傀儡的生成没有上限。每次钢铁之心破碎都会生成一个新的铁傀儡，在频繁受伤场景下可能产生大量实体。

**改进建议**:  
- 在生成前检查附近属于该玩家的铁傀儡数量，上限建议为 2
- 或在新生成前移除旧的铁傀儡

---

### 🟡 S-05: `InGameHudMixin` 硬编码了心形图集尺寸 `64×64`

**文件**: `src/main/java/com/ming/victus/mixin/InGameHudMixin.java` 间接引用  
`src/main/java/com/ming/victus/client/VictusHudRenderer.java` 第 13 行  
**严重程度**: 🟡 Suggestion

**问题描述**:  
```java
context.blit(HeartAspect.HEART_ATLAS_TEXTURE, x + 1, y + 1, u, v, Math.round(rechargeProgress * width), 8, 64, 64);
```

纹理图集尺寸硬编码为 64×64。如果未来添加更多心相（超过 8 列 × 8 行 = 64 个纹理槽），需要扩展图集但代码不会自动适应。

**改进建议**:  
- 将图集尺寸提取为 `HeartAspect` 或 `VictusHudRenderer` 的常量
- 或从纹理元数据中动态获取

---

### 🟡 S-06: `PotionAspect.getOverlayTint()` 在空药水时返回白色而非透明

**文件**: `src/main/java/com/ming/victus/hearts/content/PotionAspect.java` 第 112–115 行  
**严重程度**: 🟡 Suggestion

**问题描述**:  
```java
public int getOverlayTint() {
    if (this.potion.isEmpty()) return 0xFFFFFF;
    return PotionContents.getColor(this.potion.get().value().getEffects());
}
```

当药水之心未存储任何药水时，`shouldRenderOverlay()` 仍返回 `true`（默认实现），导致在心形上绘制一个白色的覆盖层，视觉效果可能不理想。

**改进建议**:  
- 覆写 `shouldRenderOverlay()` 方法，当 `potion.isEmpty()` 时返回 `false`

---

### 🟡 S-07: `GoldenAspect` 吸收效果等级计算依赖心相索引位置

**文件**: `src/main/java/com/ming/victus/hearts/content/GoldenAspect.java` 第 30–36 行  
**严重程度**: 🟡 Suggestion

**问题描述**:  
```java
float percentage = 1.0F - (index + 0.0F) / (this.player.getMaxHealth() / 2.0F);
int level = Math.max(0, Math.round(percentage * 5.0F));
```

吸收效果的等级取决于黄金之心在心相列表中的索引位置。位于最左侧（索引 0）的黄金之心给予最高等级（level IV），而位于最右侧的给予最低等级。这个机制在代码中没有注释说明，也没有在物品描述中体现，玩家难以理解。

**改进建议**:  
- 添加代码注释说明设计意图
- 考虑在物品 tooltip 中提示"越靠左的心槽效果越强"

---

### 🟡 S-08: `VictusEvents.onLivingDamage()` 心相破碎范围计算可能溢出

**文件**: `src/main/java/com/ming/victus/event/VictusEvents.java` 第 173–195 行  
**严重程度**: 🟡 Suggestion

**问题描述**:  
```java
int firstAffectedHeart = (int) Math.ceil((originalHealth - damage) / 2) - 1;
int lastAffectedHeart = (int) Math.ceil(originalHealth / 2) - 1;
```

当 `originalHealth` 很大（如通过命令设置 HP > 2000）时，`(int)` 转换可能溢出。虽然正常游戏中不太可能出现这种情况，但防御性编程应考虑此边界。

**改进建议**:  
- 添加 `Math.min(result, cap.effectiveSize() - 1)` 的钳制（当前已有对 `lastAffectedHeart` 的钳制，但 `firstAffectedHeart` 的钳制仅处理了 `< 0` 的情况）

---

### 🟡 S-09: `EdibleItem.onEaten()` 仅在服务端执行，创造模式下不消耗物品的逻辑分散

**文件**: `src/main/java/com/ming/victus/item/EdibleItem.java` + 子类  
**严重程度**: 🟡 Suggestion — 一致性

**问题描述**:  
`EdibleItem.finishUsingItem()` 的 `onEaten()` 回调仅在 `!level.isClientSide()` 时执行，这确保了服务端权威性。但物品消耗逻辑 (`stack.shrink(1)`) 分散在各个子类的 `onEaten()` 中，且都重复 `if (!eater.getAbilities().instabuild)` 检查。

**改进建议**:  
- 将 `stack.shrink(1)` + 创造模式检查提取到 `EdibleItem.finishUsingItem()` 中统一处理
- 子类 `onEaten()` 只负责业务逻辑，无需关心物品消耗

---

### 🟡 S-10: `BrokenHeartItem` 造成 15 点魔法伤害但不触发心相破碎

**文件**: `src/main/java/com/ming/victus/item/BrokenHeartItem.java` 第 51 行  
**严重程度**: 🟡 Suggestion — 逻辑一致性

**问题描述**:  
```java
eater.hurt(level.damageSources().magic(), 15.0F);
```

破碎之心移除所有心相后造成 15 点魔法伤害。此伤害会触发 `LivingDamageEvent.Post`，进而调用 `cap.damageAspect()`。但此时所有心相已被移除（`aspects` 列表已空），所以 `damageAspect` 不会破碎任何心相。

这个行为是正确的（避免无限循环），但如果未来有人修改代码顺序（先受伤再移除心相），就会导致所有心相同时破碎，产生严重的连锁效果。

**改进建议**:  
- 添加明确的注释警告："伤害必须在心相移除之后施加，否则会触发心相破碎连锁"
- 或考虑在受伤前临时清除 Attachment 数据以避免意外

---

### 🟡 S-11: `DragonBreathCloud` 未注册到 `victus.mixins.json` 但覆盖了 `tick()` 和 `addEffect()`

**文件**: `src/main/java/com/ming/victus/entity/DragonBreathCloud.java`  
**严重程度**: 🟡 Suggestion

**问题描述**:  
`DragonBreathCloud` 继承 `AreaEffectCloud` 并覆写了 `tick()` 和 `addEffect()`。这是常规继承而非 Mixin，但存在一个潜在问题：`AreaEffectCloud.tick()` 在服务端每 5 tick 遍历碰撞箱内的实体施加效果，而自定义的 `tick()` 也做了同样的事。两者可能叠加执行。

**当前代码**:
```java
public void tick() {
    super.tick();  // 父类 tick 也会施加效果
    if (!this.level().isClientSide && this.tickCount % 5 == 0) {
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox())) {
            // 再次施加效果...
        }
    }
}
```

**风险**: 父类 `AreaEffectCloud.tick()` 也会在其自身的周期内施加 `customEffects`（如果存在），可能导致效果双重施加。

**改进建议**:  
- 如果目的是完全自定义效果施加逻辑，应在 `tick()` 中跳过 `super.tick()` 中效果施加部分
- 或确保父类的效果列表为空，仅在自定义逻辑中施加

---

### 🟡 S-12: `HeartAspectRegistry` 使用 `HashMap` 而非 `LinkedHashMap`，注册顺序不保证

**文件**: `src/main/java/com/ming/victus/hearts/HeartAspectRegistry.java`  
**严重程度**: 🟡 Suggestion

**问题描述**:  
`REGISTRY` 使用 `HashMap`，不保证注册顺序。虽然当前代码中注册顺序不影响功能（查找是基于 ResourceLocation 的），但如果未来需要按注册顺序遍历心相（如 UI 显示），HashMap 的无序性会导致不确定行为。

**改进建议**:  
- 如果需要顺序保证，改用 `LinkedHashMap`
- 或提供按注册顺序的迭代器

---

### 💭 N-01: 大量全限定名（FQN）降低可读性

**文件**: 多个文件，尤其是 `VictusEvents.java`  
**严重程度**: 💭 Nit

**问题描述**:  
`VictusEvents.java` 中存在大量 FQN 用法，例如：
- `com.ming.victus.hearts.content.TotemAspect.TYPE`
- `net.minecraft.server.level.ServerPlayer`
- `net.neoforged.neoforge.network.PacketDistributor.sendToPlayer()`

这些都可以通过 import 语句简化。

**统计**: 项目中约有 60+ 处 FQN 用法。

**改进建议**:  
- 在后续迭代中统一重构为 import 方式
- 设置 IDE 的 FQN 检测规则

---

### 💭 N-02: `@SuppressWarnings("null")` 过度使用

**文件**: 几乎所有 Java 文件  
**严重程度**: 💭 Nit

**问题描述**:  
项目中有约 40 处 `@SuppressWarnings("null")` 注解，主要用于抑制 NeoForge API 的 nullability 警告。虽然这些注解本身不会影响运行时行为，但过度使用可能掩盖真正的空指针问题。

**改进建议**:  
- 逐步迁移到 `@Nullable` / `@Nonnull` 注解体系
- 仅在确实安全的地方保留 `@SuppressWarnings`

---

### 💭 N-03: 游戏测试覆盖不足

**文件**: `src/main/java/com/ming/victus/test/VictusGameTests.java`  
**严重程度**: 💭 Nit

**问题描述**:  
当前仅有 1 个游戏测试（创造标签页物品计数验证），缺少以下关键测试：
- 心相增删逻辑
- 心相破碎触发
- NBT 序列化/反序列化
- 网络包编解码
- 伤害到心相破碎的映射逻辑

**改进建议**:  
- 为 `PlayerHeartCapability` 的核心方法编写单元测试
- 为每个心相的 `handleBreak` 编写游戏测试
- 为网络包编解码编写测试

---

### 💭 N-04: `TotemAspect` 的 `hadTotem` 字段在充能完成后含义模糊

**文件**: `src/main/java/com/ming/victus/hearts/content/TotemAspect.java` 第 38–46 行  
**严重程度**: 💭 Nit

**问题描述**:  
```java
public int getRechargeDuration() {
    return this.hadTotem ? 40 : getType().standardRechargeDuration();
}
```

`hadTotem` 字段记录的是**上一次**破碎时是否有图腾，但它影响的是**当前**充能时间。这意味着如果玩家上一次有图腾（充能 40 tick），但这一次没有图腾，充能时间仍然会是 40 tick（因为 `hadTotem` 在新一次破碎前不会更新）。

实际上，`hadTotem` 在 `handleBreak` 中会立即更新，然后 `cooldown` 被重置为新的 `getRechargeDuration()`。所以逻辑是正确的——`hadTotem` 反映的是"本次破碎时是否有图腾"，充能时间基于此值。

**改进建议**:  
- 将 `hadTotem` 重命名为 `lastBreakHadTotem` 或 `consumedTotemLastBreak`，使其语义更清晰

---

### 💭 N-05: 辅助脚本和调试文件残留在项目根目录

**文件**: 项目根目录  
**严重程度**: 💭 Nit

**问题描述**:  
以下文件为开发辅助工具，不应包含在最终发布版本中：
- `CheckGuiHealthMethod.java` / `.class`
- `CheckGuiHeartsMethod.java` / `.class`
- `CheckGuiMethods.java` / `.class`
- `FindMethods.java`
- `GenerateAspects.java` / `.class`
- `fix_recipes.py`
- `generate_aspects.py`
- `update_lang.py`
- `rename.js`
- `debug.log`
- `build_log.txt`
- `build_output.txt`
- `Gui.txt`
- `methods.txt`
- `process_info.txt`

**改进建议**:  
- 将辅助脚本移动到 `tools/` 或 `scripts/` 目录
- 将调试文件添加到 `.gitignore`
- 编译产物 `.class` 不应出现在源码目录

---

### 💭 N-06: `OceanAspect` 英文描述与实际效果不匹配

**文件**: `src/main/resources/assets/victus/lang/en_us.json` 第 19 行  
**严重程度**: 💭 Nit

**问题描述**:  
英文描述为 "Consumes charge while in water to grant **Water Breathing**, Dolphin's Grace, and Conduit Power."

但实际代码中 `handleBreak()` 仅添加了 `DOLPHINS_GRACE` 和 `CONDUIT_POWER`，**没有添加水下呼吸（Water Breathing）效果**。

```java
public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
    this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 600, 0));
    this.player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 600, 0));
    return false;
}
```

注意：`CONDUIT_POWER`（潮涌能量）在 1.21.1 中实际上会赋予水下呼吸+水下夜视+急迫效果，所以**功能上描述是正确的**，但描述容易引起误解。

**改进建议**:  
- 修改英文描述为 "Consumes charge while in water to grant Dolphin's Grace and Conduit Power (which includes Water Breathing)"
- 中文描述同样需要更新

---

### 💭 N-07: `EvokingAspect` 描述称"召唤一个"但实际召唤 3 个恼鬼

**文件**: 语言文件 + `EvokingAspect.java`  
**严重程度**: 💭 Nit

**问题描述**:  
- 英文描述: "Summons **a** tamed Vex with a sword when broken."
- 中文描述: "破碎时召唤**一个**持剑恼鬼为你作战。"
- 实际代码: 循环 3 次召唤 3 只恼鬼

**改进建议**: 修改描述为"召唤 3 个持剑恼鬼"。

---

### 💭 N-08: `rename.js` 静默吞没异常

**文件**: `rename.js` 第 41 行  
**严重程度**: 💭 Nit

**问题描述**:  
```javascript
} catch (e) {}
```

空的 catch 块会吞没所有异常，如果文件读取/写入失败，开发者无法得知。

**改进建议**: 至少打印 `console.error(e)`。

---

### 💭 N-09: `build.gradle.kts` 中 JVM 内存仅分配 1G

**文件**: `gradle.properties` 第 1 行  
**严重程度**: 💭 Nit

**问题描述**:  
```
org.gradle.jvmargs=-Xmx1G
```

对于 NeoForge 1.21.1 模组开发，1G 内存可能在数据生成或编译大型代码库时不足。

**改进建议**: 建议提升至 `-Xmx2G` 或 `-Xmx3G`。

---

## 四、架构与设计评价

### 4.1 优点

1. **心相系统架构优雅**: `HeartAspect` 基类 + `Type` record 的设计模式简洁高效，新增心相只需一个类和一个 TYPE 常量
2. **注册系统规范**: 使用 NeoForge 的 `DeferredRegister` 体系，避免时机问题
3. **网络同步覆盖全面**: 同步包（S→C）、破碎包（S→C）、移除包（C→S）三种网络包覆盖所有场景
4. **脏标记优化**: `PlayerHeartCapability` 的 `dirty` 标记减少了冗余网络同步
5. **Mixin 审计注释**: 为 HUD 渲染 Mixin 添加了完整的审计注释，记录目标方法签名和升级指引
6. **数据生成**: 使用 datagen 生成配方和物品模型，减少手写 JSON 的出错率

### 4.2 可改进之处

1. **FQN 滥用**: 约 60+ 处全限定名降低可读性，建议统一 import
2. **`@SuppressWarnings("null")` 过度使用**: 约 40 处，建议迁移到 `@Nullable`/`@Nonnull` 体系
3. **事件处理器过大**: `VictusEvents` 包含 13 个事件处理方法和 3 个辅助方法，可按功能拆分（伤害处理、生活事件、召唤物管理）
4. **缺少配置系统**: 心相的数值（充能时间、效果持续时间、伤害值等）全部硬编码，建议添加 NeoForge 配置系统支持
5. **缺少 API 暴露**: 第三方模组无法方便地注册自定义心相，因为 `HeartAspectRegistry.register()` 虽然是 public，但缺少文档和事件钩子

### 4.3 与原始 Victus 模组的对比

本项目是原版 Victus（Fabric）的 NeoForge 移植版。相比 Fabric 版本，NeoForge 版在以下方面有改进：
- 使用 Attachment API 替代 Fabric 的 Cardinal Components
- 使用 NeoForge 网络包系统替代 Fabric Networking API
- 添加了 datagen 系统
- 修复了原版的部分 BUG（如 EmeraldAspect 双重加速）

---

## 五、安全性评估

| 维度 | 评估 | 说明 |
|------|------|------|
| 网络包安全 | ⚠️ 需改进 | `RemoveFirstAspectPacket` 缺少频率限制 |
| 实体生成安全 | ⚠️ 需改进 | 恼鬼/铁傀儡无生成上限 |
| 数据持久化 | ✅ 良好 | NBT 序列化完整，copyOnDeath 正确 |
| 客户端/服务端隔离 | ✅ 良好 | 心相效果仅在服务端执行，客户端仅处理渲染 |
| 爆炸安全 | ✅ 良好 | 苦力怕之心使用 `ExplosionInteraction.NONE` 避免地形破坏 |

---

## 六、性能评估

| 维度 | 评估 | 说明 |
|------|------|------|
| 心相 tick 性能 | ✅ 良好 | 仅对充能中的心相执行 tick 逻辑 |
| 网络同步频率 | ✅ 良好 | dirty 标记减少冗余包 |
| 实体查询性能 | ⚠️ 需改进 | 铁傀儡/恼鬼寻敌每 tick 执行 AABB 查询，可添加冷却 |
| HUD 渲染性能 | ✅ 良好 | TAIL 注入避免覆盖原版渲染 |
| Diamond 属性更新 | ⚠️ 可优化 | 每 tick 检查并可能更新属性修饰符，可仅在心相状态变化时更新 |

---

## 七、与历史审查记录的关系

本报告与项目已有的 `CODE_REVIEW.md` 记录的四轮审查（37 项修复）是**增量关系**。`CODE_REVIEW.md` 中的 37 项问题已全部修复，本报告发现的是此前审查未覆盖的新问题：

- **B-01 ~ B-03**: 此前审查未关注网络包安全和列表操作兼容性
- **S-01 ~ S-12**: 此前审查未深入分析浮点精度、实体上限、API 设计等
- **N-01 ~ N-09**: 代码风格和文档层面的改进建议

---

## 八、优先级建议

### 立即处理（Blocker）
1. **B-02/B-03**: 修复 `ArcheryAspect` 和 `BlazingAspect` 中的列表操作问题，避免运行时崩溃
2. **B-01**: 为 `RemoveFirstAspectPacket` 添加频率限制

### 近期处理（Suggestion）
1. **S-03/S-04**: 为恼鬼和铁傀儡添加生成上限
2. **S-11**: 修复 `DragonBreathCloud` 的双重效果施加
3. **S-06**: `PotionAspect` 无药水时不渲染覆盖层
4. **S-01**: 将 `fractionalRecharge` 改为整数计数器

### 后续迭代（Nit）
1. **N-01/N-02**: FQN 和 `@SuppressWarnings` 重构
2. **N-03**: 补充游戏测试
3. **N-05**: 清理根目录残留文件
4. **N-06/N-07**: 修正描述文案

---

## 九、结论

Victus 模组经过前四轮审查的 37 项修复后，核心功能已完整且稳定。本次审查在此基础上发现了 3 个阻塞性问题（列表操作崩溃风险、网络安全）和 12 个改进建议，以及 9 个意见性优化点。

**整体代码质量评级**: B+（良好）

项目架构设计优雅，心相系统的扩展性强，数据同步和持久化完整。主要改进空间在于：实体生成安全限制、网络包防护、测试覆盖率、以及代码风格统一。建议优先修复 3 个 Blocker 问题后即可发布正式版本。
