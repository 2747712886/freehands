# Free Hands Agent Notes

## Art Pipeline Update

- For `minecraft-art-router` 16x16 item textures generated with `gpt-image-2`, use the minimum valid square source `816x816` (`16x51`). Verify the returned source is exactly `816x816`; regenerate any other size before running grid-locked normalization.
- Reusable trinket image prompts and acceptance criteria are in `docs/ai-image-prompts.md`. Keep credentials out of project documentation and generate material variants from an approved iron master whenever possible.
- `freehands:iron_trinket`, `freehands:diamond_trinket`, and `freehands:netherite_trinket` use transparent 16x16 centered amulet textures. Keep the shared symmetric octagonal silhouette, one-pixel transparent edge, and no more than 16 opaque colors; use layered iron gray, steel-blue with a cyan core, and dark purple-gray with a muted wine core respectively.
- Iron, diamond, and netherite trinkets have 2048, 4096, and 8192 durability. Iron and diamond use the nine-slot recipe of four same-tier armor pieces, four same-tier tools/weapons, and a same-tier material block; netherite upgrades a diamond trinket in a smithing table with a Netherite Upgrade Smithing Template and a netherite block.
- Development client test mods: JEI `15.20.0.133`, IMBlocker `4.0.5`, and JustEnoughCharacters `4.6.7`. `downloadDevelopmentClientMods` fetches verified Forge JARs into `run/dev-mods`; the local flatDir repository exposes them to `runtimeOnly fg.deobf(...)` for userdev remapping. Do not add them as published-mod dependencies.

## Automated Regression Tests

- `IronTrinketGameTests` is the Forge GameTest suite for the trinket tiers. It verifies full iron armor, diamond armor/toughness, netherite armor/toughness/knockback resistance, fixed explosion damage matching a full iron set, Blast Protection IV behavior, and player-owned TNT behavior.
- It also verifies that an iron trinket harvests stone with a cobblestone drop and loses one durability during mining. During virtual-main-hand `destroyBlock` processing, the harvest check must inspect the equipped stack directly because the selection rule sees the already-virtual stack; outside that context, retain main-hand priority.
- It verifies that an empty main hand can right-click a grass block with a free-hand shovel, producing a dirt path and consuming one durability. `UseOnContext` reads `getItemInHand(MAIN_HAND)`, so virtual-main-hand support must cover both `getMainHandItem` and `getItemInHand` during `ServerPlayerGameMode.useItemOn`.
- `ironTrinketCanFlattenGrassBlockWithRightClick`, `ironTrinketCanStripOakLogWithRightClick`, `ironTrinketCanTillDirtPathWithRightClick`, `ironTrinketCanCarvePumpkinWithRightClick`, `ironTrinketCanScrapeCopperWithRightClick`, `ironTrinketCanRemoveCopperWaxWithRightClick`, and `ironTrinketCanMatureGrowingVinesWithRightClick` verify the ability trinket's multi-tool block actions. It supports shovel flattening/campfire dousing, axe stripping/scraping/wax removal, pumpkin carving, and growing-plant maturation. Its block-action order is axe then shovel. Hoe tilling is not supported since the recipe does not include a hoe. Every successful action costs one durability and broadcasts a main-hand swing.
- `freeHandRightClickSendsSoundToActingPlayer` registers an embedded test player and verifies that a successful free-hand shovel right-click sends exactly one `ClientboundSoundPacket` to the acting player. Vanilla server sound calls exclude the actor because a physical hand normally predicts the sound locally; during `VirtualMainHandContext.isUsing`, the `Level.playSound` mixin must clear that exclusion so virtual tools and ability trinkets have audible feedback. Do not apply this behavior to mining contexts.
- `freeHandHoeCanTillDirtWithRightClick`, `freeHandAxeCanStripOakLogWithRightClick`, and `freeHandShearsCanCarvePumpkinWithRightClick` verify that each regular tool works when it is the only item in a free-hand slot. `ironTrinketDoesNotOverrideSlotOrderWhileSneaking` verifies the first-slot trinket still flattens dirt before a later hoe, including while sneaking.
- `unbreakableFreeHandToolDoesNotLoseDurability` equips an iron shovel with the vanilla `Unbreakable` NBT flag, flattens a grass block, and asserts that its damage remains zero. Durability code must use `ItemStack.isDamageableItem()` / `hurtAndBreak()` and must not add a separate item-tag whitelist.
- Right-click block-use priority is fixed: block placement, main-hand use, off-hand use, then a free-hand tool. If the off hand is empty, a main-hand `PASS` may run the fallback immediately and must record the hit position so the matching off-hand empty packet cannot run it again. If the off hand has an item, wait for its `useItemOn` to return `PASS` before falling back. GameTests verify that main-hand dirt placement and an off-hand shovel both take priority over an equipped free-hand shovel.
- `freeHandToolFiresWhenMainHandItemReturnsPASS` verifies that a main-hand tool that returns `PASS` (e.g. a shovel on a dirt path) falls through to free-hand tools (a hoe tills the path). `freeHandUseStacks` no longer checks main-hand emptiness; the vanilla `useItemOn` result already determines whether fallback should run.
- After both hands return `PASS`, free-hand block use tries compatible stacks in `free_hand` slot order. Each candidate runs the full `ServerPlayerGameMode.useItemOn` flow; only `PASS` advances to the next slot, and every non-`PASS` result stops the traversal. The next player right-click starts a new traversal from the first slot. GameTests cover pickaxe-to-shovel fallback, shovel-to-hoe fallback on a dirt path, stopping after a successful first-slot shovel, and a second click that continues from a dirt path to farmland.
- `freeHandToolRunsOnlyOnceAcrossHandPackets` simulates the main-hand and off-hand packets sent for one unhandled block click. A free-hand Iron Trinket must flatten dirt once, leaving a dirt path and consuming exactly one durability; it must not use its hoe action again from the off-hand packet. `freeHandToolHandlesMainHandPacketWhenOffHandIsEmpty` verifies that a lone main-hand packet still runs an equipped free-hand tool when the off hand is empty.
- When a free-hand tool's `useOn` returns non-`PASS` (block was modified), the mixin unconditionally swings `MAIN_HAND` and returns `CONSUME` to prevent `handleUseItemOn` from swinging the wrong hand. Vanilla's `sidedSuccess(false)` maps to `CONSUME` whose `shouldSwing()` is `false`, so a `shouldSwing()`-based guard would never trigger. `AbilityTrinketItem.applyModifiedState` must not swing itself; the mixin owns the swing for all free-hand tools.
- `ironTrinketOnGrassDropsSeedsNotGrassBlock` and `shearsInFreeHandOnGrassDropsGrassBlock` verify that plant/foliage drops respect the tool in the virtual main hand. Shears produce shears-appropriate drops, and the Iron Trinket must not interfere with blocks that do not require a correct tool.
- `ironTrinketOnLeavesDoesNotDropLeavesBlock` confirms the trinket does not produce shears-type drops for leaves. `canPerformAction` must not include `DEFAULT_SHEARS_ACTIONS` except `SHEARS_CARVE`, or Forge's leaves drop path will treat the trinket as shears.
- `ironTrinketCanBeEnchanted` verifies that the Iron Trinket accepts Efficiency and Protection enchantments, and that its `getEnchantmentValue()` returns a positive tier-appropriate value.
- Run it with `$env:GRADLE_USER_HOME='E:\mod\.gradle-user-home'; .\gradlew.bat runGameTestServer --no-daemon --max-workers=1 --console=plain`.
- Minecraft 1.20.1 loads GameTest SNBT fixtures from the run directory. Keep the source fixture at `src/gametest/resources/gameteststructures/empty.snbt`; `copyGameTestStructures` stages it before `prepareRunGameTestServer`.
- `runGameTestServer` must not load the client-only development test mods. The Gradle dependency guard excludes them only for that task; normal `runClient` continues to load JEI, IMBlocker, and JustEnoughCharacters.
- Extra free-hand attack damage applies only when the direct damage entity is the player. Never use the owner entity alone: player-owned TNT, projectiles, and other indirect damage must not be treated as a melee attack.

## 项目概览

本仓库是 `解放双手 / Free Hands` Minecraft Forge 模组。

- 当前开发路径：`E:\mod\freehands`
- 项目路径：`E:\mod\freehands`
- Minecraft：`1.20.1`
- Forge：`47.4.10`
- Mod ID：`freehands`
- 主包名：`com.yourname.freehands`
- 必需依赖：Curios

核心玩法是通过 Curios 增加两个 `free_hand` 解放槽。玩家可把原版工具、武器、护甲，以及本模组饰品装备到槽中，从而在不占用主副手的情况下获得对应功能。

## 个人工具

- `minecraft-art-router` 位于 `C:\Users\a2747\.codex\skills\minecraft-art-router`，用于路由 Minecraft 美术资源请求。当前支持物品贴图：通过 `imagegen` 生成严格 `16x16` 网格比例的概念稿，再用其 `normalize_item_texture.py --grid-locked` 脚本输出合规的透明 PNG 和最近邻预览。
- `freehands:iron_trinket` 使用自有 `assets/freehands/textures/item/iron_trinket.png`：透明 `16x16` 居中轨道铁芯贴图，锻造核心和四个对称轨道节点表达工具、武器与护甲能力，物品模型引用 `freehands:item/iron_trinket`。

## 当前实现

- 注册 `freehands:iron_trinket`、`freehands:diamond_trinket` 和 `freehands:netherite_trinket`，中文名分别为“铁饰品”“钻石饰品”“下界合金饰品”。
- 注册 Curios 槽位 `free_hand`，槽位大小为 `2`。
- 使用 `data/curios/tags/items/free_hand.json` 限制可放入槽位的物品。
- 支持原版工具、武器、护甲和铁饰品进入解放槽。
- 挖掘能力从两个解放槽中选择最适合当前方块的工具或饰品能力。
- 当主手对目标方块没有速度加成时，解放槽中最适合的工具会作为虚拟主手参与完整原版破坏流程。真实主手物品栏不变，但工具回调、掉落附魔和耐久都读取解放槽工具。
- 主、副手均未处理右键方块操作时，解放槽中的锄、斧、锹、剪刀或能力饰品可作为最后回退执行受支持的右键方块行为并消耗自身耐久。能力饰品支持铁锹铲平/熄灭营火、斧头去皮/刮锈/去蜡、剪刀雕刻南瓜/修剪生长藤蔓（设为最大年龄并停止继续生长），并在成功时摆动主手；能力饰品固定按斧头、铁锹顺序尝试（合成表不含锄头，不支持锄地），不受潜行影响。
- 攻击时从两个解放槽中选择最高攻击伤害叠加到本次伤害，并对对应武器或工具扣耐久。
- 攻击附魔当前支持锋利类伤害、火焰附加和抢夺等级补足。
- 原版护甲的护甲值和韧性由服务端 transient attribute modifier 累加；铁饰品通过 Curios 的属性接口直接提供护甲。所有提供护甲的解放槽物品受伤时扣耐久，并通过保护类附魔补充减伤。
- 无耐久物品使用原版 `Unbreakable` 标记；`ItemStack.isDamageableItem()` 已会尊重该标记。

## 能力实现规则

- 挖掘优先级：主手对目标方块的挖掘速度大于基础速度时，始终优先主手；否则使用解放槽中最适合的工具。
- 挖掘实现：`FreeHandEvents` 通过采集检查和破坏速度事件选择解放槽工具；Mixin 会在 `ServerPlayerGameMode.destroyBlock` 的范围内将其暴露为虚拟主手。不要写入或交换真实主手物品栏。时运、精准采集、效率、工具回调和耐久都应走原版流程。
- 右键实现：方块放置、主手右键和副手右键必须优先于解放槽工具。副手为空时，主手 `useItemOn` 返回 `PASS` 可立即回退到解放槽，但必须记录命中位置并跳过同次副手空包，避免重复执行工具；副手有物品时，只有副手 `useItemOn` 返回 `PASS` 后才可按槽位顺序尝试解放槽物品。只有某槽返回 `PASS` 才继续下一槽，任何非 `PASS` 结果均停止本次遍历。下一次玩家右键重新从首槽开始。`UseOnContext` 读取 `getItemInHand(MAIN_HAND)`，虚拟主手必须覆盖该访问器。
- 解放槽工具执行右键成功时，无条件摆动 `MAIN_HAND` 并返回 `CONSUME`，防止 `handleUseItemOn` 根据发包的手（`OFF_HAND`）摆动空手。原因是原版服务端 `sidedSuccess(false)` 实际映射为 `CONSUME`，其 `shouldSwing()` 为 `false`，因此不能依赖 `shouldSwing()` 判断。`AbilityTrinketItem.applyModifiedState` 不允许自行摆臂，统一由 mixin 处理。
- Curios 读取：不要使用已弃用的 `findCurios(LivingEntity, String...)`；读取 `free_hand` 槽时使用 `CuriosApi.getCuriosInventory(player)`、`getStacksHandler(FreeHands.FREE_HAND_SLOT)` 和 `IDynamicStackHandler`。
- 攻击逻辑：解放槽最高攻击值作为额外伤害叠加，不替换主手伤害；实际参与叠加的槽内武器或工具需要扣耐久。
- 攻击附魔：攻击时只选择伤害最高的解放槽物品；当前显式补充锋利类伤害、火焰附加和该物品的抢夺等级，额外伤害继承暴击倍率。其他特殊攻击附魔不要默认认为已完整兼容。
- 防御逻辑：原版护甲通过 transient attribute modifier 提供属性；能力饰品必须在 `ICurioItem#getAttributeModifiers` 中提供自身护甲，不能重复计入 transient 汇总。保护类附魔和所有提供护甲物品的耐久在受伤事件中补充处理。
- 植物/树叶掉落：canStackHarvest 对剪刀始终返回 	rue（掉落由战利品表决定）；canHarvestWithAbility 对不需要正确工具的方块返回 alse，避免能力饰品错误接管。
- 无耐久扩展：使用原版 `Unbreakable` 标记，不要重新引入自定义耐久白名单。

## 已取消方向

不要继续按旧 `Core Trinkets/coretrinkets` 设计扩展：

- 不做核心升级主线。
- 不做特殊合成升级系统。
- 不做核心附魔扩展。
- 不做自定义能量单位。
- 不做 FE、Mana、AE 等能量兼容层。
- 不再使用 `iron_core`、`core` 槽或 `coretrinkets` 资源命名。

## 项目结构

- `src/main/java/com/yourname/freehands`：Java 源码。
- `src/main/resources/META-INF/mods.toml`：Forge 模组元数据和依赖。
- `src/main/resources/assets/freehands`：客户端资源、模型和本地化。
- `src/main/resources/data/freehands`：配方和 Curios 槽位数据。
- `src/main/resources/data/curios/tags/items/free_hand.json`：Curios 槽位物品白名单。
- `build.gradle`：ForgeGradle、仓库和依赖配置。
- `gradle.properties`：Minecraft、Forge、模组元数据和 Curios 版本。

## 常用命令

命令从当前项目目录 `E:\mod\freehands` 运行。

构建：

```powershell
$env:GRADLE_USER_HOME='E:\mod\.gradle-user-home'
.\gradlew.bat build --no-daemon --max-workers=1 --console=plain
```

启动客户端：

```powershell
$env:GRADLE_USER_HOME='E:\mod\.gradle-user-home'
.\gradlew.bat runClient
```

生成 IntelliJ 运行配置：

```powershell
$env:GRADLE_USER_HOME='E:\mod\.gradle-user-home'
.\gradlew.bat genIntellijRuns
```

## 开发约束

- 任何文件操作先确认目标路径，尤其是删除、移动、重命名。
- 不要使用 `Remove-Item` 对数组、通配符或目录递归执行删除。
- 如果需要清理多个文件，必须逐个文件单独执行。
- 复杂任务使用 todo list。
- Markdown 文件必须保持 UTF-8，避免中文乱码。
- Git 提交信息、推送说明和相关沟通必须使用中文。
- 不要把其他模组项目直接混进当前项目；多个模组应放在 `E:\mod` 下的独立子目录。
- 不要删除 `E:\mod` 根目录文件，除非用户明确要求。
- 不要重新引入旧 `coretrinkets` 命名。

## 运行时注意

## Ultimine 兼容测试

- FTB Ultimine `2001.1.8`（Minecraft `1.20.1` / Forge）依赖 Architectury `>= 9.2.14` 与 FTB Library `>= 2001.2.1`。开发测试使用 `architectury-9.2.14-forge.jar`、`ftb-library-forge-2001.2.12.jar` 与 `ftb-ultimine-forge-2001.1.8.jar`，全部放在 `run/dev-mods`。
- 不要把上述生产 JAR 直接放进 `run/mods`：在官方映射的 ForgeGradle 开发环境中，Architectury 的未重映射 refmap 会导致 `MixinFallingBlockEntity` 找不到字段并崩溃。
- 三个 JAR 都在 `run/dev-mods` 时，IDEA、普通 `runClient` 和 `runGameTestServer` 会自动通过 `fg.deobf` 重映射并加载它们，无需额外参数；缺少任意 JAR 时自动跳过，不影响干净构建。需要临时关闭时使用 `-PenableUltimineCompatibility=false`。无论哪种情况，依赖都不会进入发布产物。
- 已验证该开关下 Architectury、FTB Library、FTB Ultimine 与 Free Hands 均完成客户端加载；Ultimine 会嵌套调用 `ServerPlayerGameMode.destroyBlock`，虚拟主手上下文必须为每次嵌套调用成对入栈和出栈，不能在内层返回时清除外层工具。
- 不要把解放槽工具映射到 Ultimine 的 `RIGHT_CLICK_BLOCK` 预处理事件。只要 Ultimine 处理成功，Architectury 就会取消整个原版右键包，导致客户端放置预测被服务器回滚，且无法保证放置方块、主手、副手、解放槽的既定优先级。当前仅兼容 Ultimine 左键连锁采集；解放槽工具右键仍在原版 `ServerPlayerGameMode.useItemOn` 回退阶段执行。
- `ultimineDoesNotCancelFreeHandRightClick` 与 `ultimineDoesNotCancelMainHandBlockPlacement` 通过 Forge 的真实右键事件验证：按住 Ultimine 时不允许取消解放槽或主手方块的原版右键包，随后由既有的解放槽回退逻辑处理工具右键。测试用静默网络连接只用于避免 GameTest 人工 `ServerPlayer` 缺失客户端连接，不能移入生产代码。
- `ultimineDamagesFreeHandPickaxeForEveryBrokenBlock` 通过真实 Forge `BlockEvent.BREAK` 验证左键连锁：解放槽铁镐破坏两个相邻石头时，两个方块均被破坏、工具恰好损耗两点耐久，且物理主手保持为空。Ultimine 会自行递归破坏并取消最外层 `destroyBlock` 调用，因此测试应断言最终方块状态和耐久，不应断言最外层返回值。


## 验证清单

- `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain` 通过。
- `.\gradlew.bat runClient` 能启动客户端。
- Curios UI 显示两个“解放槽”。
- 铁镐放入解放槽后，空手可挖铁级方块。
- 空手或主手没有对应方块速度加成、解放槽放合适工具时，解放槽工具掉耐久；主手有速度加成时保持主手逻辑。
- 解放槽工具的效率、时运、精准采集和耐久附魔需要手动进游戏验证。
- 铁剑放入解放槽后，攻击伤害提高。
- 解放槽武器或工具参与攻击后，对应槽内物品掉耐久。
- 解放槽武器的锋利、火焰附加、抢夺、耐久附魔需要手动进游戏验证。
- 护甲放入解放槽后，护甲值变化。
- 解放槽护甲受到攻击后掉耐久，保护类附魔参与减伤。
- 铁、钻石、下界合金饰品放入解放槽后，分别提供完整原版铁、钻石、下界合金套装的防御属性；下界合金还提供 `0.4` 击退抗性。
- `freehands:iron_trinket` 可装备并提供铁级能力。
- 主、副手为空时，解放槽中的锄、斧、锹或剪刀可右键方块并消耗自身耐久。
- 方块放置、主手右键和副手右键必须优先于解放槽工具右键。
