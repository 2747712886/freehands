# Free Hands Agent Notes

## 仓库与发布

- 默认分支 `1.20.1`（2026-07-20 由 `master` 改名，保留完整历史）。
- 本仓库在**多台电脑间同步开发**（如 `E:\mod\freehands`、`D:\mod\freehands`），盘符因人而异，文档一律用相对路径、不写死。
- 首个公开 Release：tag `1.20.1-0.1.0`，标题 `Free Hands 0.1.0 for Minecraft Forge 1.20.1`，产物 `freehands-forge-1.20.1-0.1.0.jar`。后续发布前先跑完整 GameTest 与 `build`，并把确切 tag/标题/产物名记录在此。

## 美术管线

- `minecraft-art-router` 生成 16x16 贴图：源图必须**正好 `816x816`**（16x51），否则重新生成；再以 `normalize_item_texture.py --grid-locked` 输出合规透明 PNG。凭证不入项目文档；材料变体尽量由已批准的 iron master 生成。可复用提示词与验收标准见 `docs/ai-image-prompts.md`。
- 三个饰品贴图 `freehands:{iron,diamond,netherite}_trinket`：透明 16x16 居中护身符，对称八角轮廓、单像素透明边、≤16 不透明色；铁灰 / 钢蓝带青色核心 / 暗紫灰带酒红核心。耐久 2048/4096/8192。
- 铁/钻九格配方：四同阶护甲 + 四同阶工具/武器 + 同阶材料块；下界合金 = 锻造台 + 升级模板 + 下界合金块升级钻石饰品。

## 开发测试模组（run/dev-mods）

- `downloadDevelopmentClientMods` 把六个客户端测试模组下载到 `run/dev-mods`，经本地 flatDir 仓库以 `runtimeOnly fg.deobf(...)` 暴露。**只做本地验证，绝不进发布产物**。
- JEI `15.20.0.133`、IMBlocker `4.0.5`、JustEnoughCharacters `4.6.7`。
- FTB Ultimine 三件套：`architectury-9.2.14-forge.jar`、`ftb-library-forge-2001.2.12.jar`、`ftb-ultimine-forge-2001.1.8.jar`（后两个走 CurseForge CDN，sha1 与社区 packwiz 清单核对一致）。
- 依赖守卫：六个 devmods 均**仅当对应 jar 存在于 `run/dev-mods` 时才声明**（自动检测）。任何解析不了的 `fg.deobf` 依赖会毒化共享的 `:__obfuscated` 管线、让 Curios 连带失败——缺 jar 必须"跳过声明"而非报错。恢复 JEI/IMBlocker 只需跑一次 `downloadDevelopmentClientMods`。
- **客户端专用 devmods（JEI/IMBlocker/JEC）只进客户端运行配置**：`runGameTestServer` 是专用服务端，加载它们会因引用客户端类（如 `net.minecraft.client.searchtree.SuffixArray`）在 CONSTRUCT 阶段崩溃，故按 `!runningGameTestServer` 排除；Ultimine 三件套服务端安全，始终按 jar 存在性启用。

## 回归测试要点

- `IronTrinketGameTests`（Forge GameTest）：铁/钻/下界合金整套防御、爆炸伤害（铁套基线）、爆炸保护 IV、玩家 TNT 行为。
- 挖掘：虚拟主手 `destroyBlock` 期间的采集检查**直接读已装备栈**（选择规则此刻看到的已是虚拟栈）；该上下文外保持主手优先。空主手 + 解放槽锹可右击草方块→土径、扣 1 耐久。
- 右键：优先级固定 放置→主手→副手→解放槽（解放槽内按 `rightClickPriority` 铲平→去皮→锄地排序，与链式右键一致，避免锄头抢在铲平前把泥土直接锄成耕地）。`UseOnContext` 读 `getItemInHand(MAIN_HAND)`，虚拟主手须同时覆盖 `getMainHandItem` 与 `getItemInHand`。副手空时主手 `PASS` 可立即回退，但必须记录命中位置跳过同次副手空包；副手有物品时等其 `PASS`。只有 `PASS` 进下一槽，非 `PASS` 即停；下个右键从首槽重来。
- 能力饰品多工具右键：锹铲平/熄营火、斧去皮/刮锈/去蜡、剪南瓜(pumpkin carving)/成熟藤蔓（测试 `ironTrinketCanFlatten/Strip/Till/Carve/Scrape/Wax/Mature*`）；**固定斧→锹顺序**（配方无锄，不支持锄地）；每次成功扣 1 耐久并广播主手摆臂。普通工具单放槽中亦可（锹→土径、斧→剥原木、剪→雕南瓜）。
- 一物理右键只执行一次：主手+副手两个空包不重复触发（`freeHandToolRunsOnlyOnceAcrossHandPackets`）；副手空时单主手包也触发（`...HandlesMainHandPacketWhenOffHandIsEmpty`）。
- 成功执行：无条件摆动 `MAIN_HAND` + 返回 `CONSUME`（原版 `sidedSuccess(false)` 映射为 `CONSUME` 且 `shouldSwing()==false`，不能靠 shouldSwing 判断）；`AbilityTrinketItem.applyModifiedState` 不自摆臂，mixin 统一负责。
- 声音：仅 `VirtualMainHandContext.isUsing` 期间，`Level.playSound` mixin 清除"排除发声者"，否则虚拟工具无声（`freeHandRightClickSendsSoundToActingPlayer` 验证恰好一个包）；**勿用于挖掘上下文**。
- 耐久：用 `isDamageableItem()`/`hurtAndBreak()`，尊重原版 `Unbreakable`，无自定义白名单（`unbreakableFreeHandToolDoesNotLoseDurability`）。
- 掉落：植物/树叶按虚拟主手工具结算（剪刀→草方块、饰品→种子）；饰品不接管不需正确工具的方块（`ironTrinketOnGrassDropsSeedsNotGrassBlock` 等）。
- 树叶：饰品不得产生剪刀类掉落；`canPerformAction` **不得含 `DEFAULT_SHEARS_ACTIONS`**（除 `SHEARS_CARVE`），否则树叶被当剪刀。
- 附魔：`ironTrinketCanBeEnchanted` 接受效率/保护，`getEnchantmentValue()` 为正。
- 攻击：解放槽最高武器叠伤，仅当**直接伤害实体是玩家**（玩家 TNT/抛射物等间接伤害不算近战）；继承暴击倍率，只扣参与叠伤的武器耐久。
- 运行：`runGameTestServer` 从 run 目录读 SNBT 夹具：源 `src/gametest/resources/gameteststructures/empty.snbt`，`copyGameTestStructures` 在 `prepareRunGameTestServer` 前 staging。

## 项目概览

本仓库是 `解放双手 / Free Hands` Minecraft Forge 模组：通过 Curios 增加两个 `free_hand` 解放槽，玩家把原版工具/武器/护甲及本模组饰品放入后，不占主副手即获对应功能。

- Minecraft `1.20.1` · Forge `47.4.10` · Mod ID `freehands` · 主包 `com.yourname.freehands` · 必需依赖 Curios。

## 个人工具

- `minecraft-art-router` 位于 `C:\Users\a2747\.codex\skills\minecraft-art-router`（本机个人工具）：`imagegen` 生成严格 16x16 网格概念稿 → `normalize_item_texture.py --grid-locked` 输出透明 PNG 与最近邻预览。
- `freehands:iron_trinket` 自有贴图 `assets/freehands/textures/item/iron_trinket.png`：透明 16x16 居中轨道铁芯，锻造核心 + 四对称轨道节点，模型引用 `freehands:item/iron_trinket`。

## 当前实现

- 注册 `freehands:iron_trinket`、`freehands:diamond_trinket`、`freehands:netherite_trinket`（中文名"铁/钻石/下界合金饰品"）；`free_hand` Curios 槽位（大小 2）；`data/curios/tags/items/free_hand.json` 白名单。原版工具/武器/护甲及饰品均可放入。
- 挖掘：两解放槽中选最适合当前方块的工具/饰品能力；主手无速度加成时，选中工具作为虚拟主手参与完整原版破坏流程，真实主手物品栏不变，回调/掉落附魔/耐久都读解放槽工具。
- 右键：主、副手均未处理时，解放槽锹/斧/锄/剪刀或能力饰品作为最后回退执行受支持行为并耗自身耐久（能力饰品支持铲平/熄营火、去皮/刮锈/去蜡、雕南瓜/成熟藤蔓，固定斧→锹顺序，不受潜行影响，成功摆主手）。
- 攻击：解放槽最高攻击值叠加到本次伤害并对该武器/工具扣耐久；显式支持锋利类伤害、火焰附加、抢夺等级。
- 防御：原版护甲护甲值/韧性由服务端 transient attribute modifier 累加；饰品经 Curios 属性接口直接提供护甲；受伤扣耐久 + 保护类附魔补充减伤。
- 无耐久物品用原版 `Unbreakable` 标记；`ItemStack.isDamageableItem()` 已尊重该标记。

## 能力实现规则

- 挖掘优先级：主手对目标方块速度加成 > 基础时始终主手；否则用解放槽最优工具。实现：`FreeHandEvents` 经采集检查/破坏速度事件选工具，Mixin 在 `ServerPlayerGameMode.destroyBlock` 内暴露为虚拟主手；不写/不换真实主手物品栏，时运/精准/效率/工具回调/耐久全走原版。
- 右键实现：放置、主手、副手优先于解放槽。副手空时主手 `PASS` 可立即回退但须记录命中位置并跳过同次副手空包；副手有物品时等其 `PASS` 后才按 `rightClickPriority`（铲平→去皮→锄地）尝试解放槽物品。只有 `PASS` 进下一槽，任何非 `PASS` 停止遍历；下次右键重来。`UseOnContext` 读 `getItemInHand(MAIN_HAND)`，虚拟主手必须覆盖该访问器。
- 右键成功：无条件摆动 `MAIN_HAND` 并返回 `CONSUME`，防止 `handleUseItemOn` 按发包的手摆空手（原版 `sidedSuccess(false)` 映射为 `CONSUME`、`shouldSwing()==false`，不能靠 shouldSwing 判断）。`AbilityTrinketItem.applyModifiedState` 不自摆臂，mixin 统一处理。
- Curios 读取：**不用已弃用的 `findCurios(LivingEntity, String...)`**；用 `CuriosApi.getCuriosInventory(player)` + `getStacksHandler(FreeHands.FREE_HAND_SLOT)` + `IDynamicStackHandler`。
- 攻击逻辑：解放槽最高攻击值作为额外伤害叠加，不替换主手伤害；只扣实际参与叠伤的槽内武器/工具耐久。只选伤害最高者；显式补充锋利类伤害、火焰附加、该物品抢夺等级，额外伤害继承暴击倍率；其他特殊攻击附魔不默认兼容。
- 防御逻辑：原版护甲经 transient attribute modifier 提供属性；能力饰品必须在 `ICurioItem#getAttributeModifiers` 自供护甲，**不得重复计入 transient 汇总**；保护类附魔与护甲耐久在受伤事件补充处理。
- 植物/树叶：`canStackHarvest` 对剪刀恒 true（掉落交给战利品表）；`canHarvestWithAbility` 对不需正确工具的方块返回 false，避免能力饰品错误接管。
- 无耐久：只用原版 `Unbreakable`，不建自定义耐久白名单。

## 已取消方向

不按旧 `Core Trinkets/coretrinkets` 扩展：不做核心升级主线 / 特殊合成升级系统 / 核心附魔扩展 / 自定义能量单位 / FE、Mana、AE 等能量兼容层；不再用 `iron_core`、`core` 槽或 `coretrinkets` 命名。

## 项目结构

- `src/main/java/com/yourname/freehands`：Java 源码。
- `src/main/resources/META-INF/mods.toml`：模组元数据与依赖。
- `src/main/resources/assets/freehands`：客户端资源、模型、本地化。
- `src/main/resources/data/freehands`：配方与 Curios 槽位数据。
- `src/main/resources/data/curios/tags/items/free_hand.json`：槽位物品白名单。
- `build.gradle` / `gradle.properties`：ForgeGradle、仓库、依赖、模组元数据。

## 常用命令

从项目根目录运行（`GRADLE_USER_HOME` 因机器而异，按本机缓存目录设置）。

构建：

```powershell
.\gradlew.bat build --no-daemon --max-workers=1 --console=plain
```

启动客户端：

```powershell
.\gradlew.bat runClient
```

生成 IntelliJ 运行配置：

```powershell
.\gradlew.bat genIntellijRuns
```

构建环境：Gradle wrapper 当前 `8.14.5`（官方源），基线 `8.8`（腾讯镜像）。勿在文档写死 `GRADLE_USER_HOME` 具体路径。

## 开发约束

- 文件操作先确认目标路径，尤其删除/移动/重命名；不用 `Remove-Item` 对数组、通配符或目录递归删除；多文件逐个清理。
- 复杂任务用 todo list；Markdown 保持 UTF-8；Git 提交信息、推送说明和相关沟通用中文。
- 不要把其他模组混进本项目；多个模组放在仓库父目录下各自独立子目录；不要删除仓库根目录外的无关文件，除非用户明确要求。
- 不重新引入旧 `coretrinkets` 命名。

## Ultimine 兼容测试

- FTB Ultimine `2001.1.8`（依赖 Architectury `>=9.2.14`、FTB Library `>=2001.2.1`）。三 jar 已纳入 `downloadDevelopmentClientMods` 自动下载；**三 jar 齐全才启用**（`enableUltimineCompatibility` 自动检测，可 `-PenableUltimineCompatibility=false` 临时关闭）。缺 jar 自动跳过，不影响干净构建；依赖不进发布产物。
- 不要把这三个生产 JAR 放进 `run/mods`：官方映射开发环境下，Architectury 未重映射 refmap 会让 `MixinFallingBlockEntity` 找不到字段而崩溃。
- Ultimine 会嵌套调用 `ServerPlayerGameMode.destroyBlock`：虚拟主手上下文必须为每次嵌套**成对入栈/出栈**，不能在内层返回时清除外层工具。
- `FTBUltimineMixin` 在 `blockRightClick` 的 `RETURN` 注入：主手流程自然执行；`PASS` 则反射试副手（传 `OFF_HAND`，不设虚拟上下文；原版 `PlatformMethods` 直接读 `getItemInHand(OFF_HAND)`）；仍 `PASS` 遍历 `ultimineUseStacks`（解放槽按 `rightClickPriority` 铲→斧→锄，其余 Curios 槽按 slot order）。每件 `beginUsing` 后反射调 `MAIN_HAND`；任一阶段成功即替换返回值并取消事件，已消费右键不继续遍历（草方块铲土径与锄地分开为两次右键）。
- 链式右键按解放槽工具判定动作分支：`PlatformMethodsImpl.blockRightClick` 依 `getItemInHand(hand).canPerformAction` 走 锄→斧→锹（右击锄地/剥皮/铲平），解放槽工具经虚拟主手命中同一分支。**土径(锹土路)锄地**：原版 `HoeItem.TILLABLES` 允许 dirt_path→farmland，但 Ultimine 的 `ftbultimine:farmland_tillable` 标签缺 `minecraft:dirt_path`，本模组以 `src/main/resources/data/ftbultimine/tags/blocks/farmland_tillable.json`（`replace:false`）合并该方块补上。另注意：Ultimine 的 `shovel_flattenable` 标签含 `#forge:dirt`，因此链式铲平对泥土/砂土也生效（泥土→土径），饰品+锄头连锁在泥土上也能走出"先土径、第二次右键再耕地"的两步流程。
- **GameTest 瞄准必须设 `yHeadRot`**：Ultimine 的 `pick` 视线读 `LivingEntity.getViewYRot`→`yHeadRot`，而 `yHeadRot` 只在 `Player.serverAiStep` 每 tick 从 `yRot` 同步；测试里 `setYRot` 后同一 tick 就 post 事件，射线会沿出生随机偏航发射、偶发 MISS，表现为连锁测试随机失败。`aimAtBlock` 必须同时 `setYHeadRot`/`setYBodyRot`。
- 测试：`ultimineChainsFreeHandShovelRightClick`（右键连锁两草方块铲土径、扣 2 耐久）、`ultimineChainsFreeHandHoeOnDirt`（锄连锁两泥土→耕地）、`ultimineChainsFreeHandHoeOnDirtPath`（锄连锁两土径→耕地，依赖上方标签合并）、`ultimineChainsFreeHandAxeStripsLogs`（斧连锁剥两原木）、`ultimineChainsIronTrinketFlattensGrass`（饰品连锁铲两草方块）、`ultimineTrinketFlattensDirtBeforeHoeTills`（饰品+锄头：第一次连锁右键饰品把两泥土铲成土径、扣饰品 2 耐久且锄头不掉耐久，第二次连锁右键锄头把两土径锄成耕地、扣锄头 2 耐久）、`ultimineHoeInEarlierSlotStillFlattensFirstViaTrinket`（锄头占前槽时仍由饰品先铲平，顺序不受槽位影响）、`ultimineDamagesFreeHandPickaxeForEveryBrokenBlock`（左键连锁破两石头、扣 2 耐久）。**断言最终方块状态与耐久，不断言最外层返回值**（Ultimine 递归破坏并取消最外层 `destroyBlock`）。测试用静默网络连接只用于避免人工 `ServerPlayer` 缺客户端连接，不得移入生产代码。

## 验证清单

- `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain` 通过。
- `.\gradlew.bat runClient` 能启动客户端；Curios UI 显示两个"解放槽"。
- 铁镐入解放槽后空手可挖铁级方块；空手/主手无速度加成时解放槽工具掉耐久，主手有加成时保持主手。
- 铁剑入解放槽后攻击伤害提高；参与攻击的槽内物品掉耐久。
- 护甲入解放槽后护甲值变化；受伤掉耐久，保护附魔参与减伤。
- 铁/钻/下界合金饰品分别提供整套对应材质防御；下界合金另有 `0.4` 击退抗性。
- `freehands:iron_trinket` 可装备并提供铁级能力。
- 主、副手为空时解放槽中的锄/斧/锹/剪刀可右键方块并耗耐久。
- 方块放置、主手右键、副手右键优先于解放槽工具右键。
- 解放槽工具的效率/时运/精准采集/耐久附魔、武器的锋利/火焰附加/抢夺/耐久附魔需手动进游戏验证。
<!-- HANDOFF-START -->
## 交接区（自动维护，请勿手改本块）
- 更新时间：2026-08-31 13:30
- 本次完成：接手上个会话中断的任务。定位并修复连锁测试偶发失败根因（`aimAtBlock` 缺 `setYHeadRot`/`setYBodyRot`，Ultimine `pick` 读 `yHeadRot` 而非 `yRot`）；清理上个会话残留的 DBG 调试代码；`runGameTestServer` 连续三轮 43/43 全绿、`build` 通过，产物含 `farmland_tillable` 土径标签合并。
- 关键决策：饰品+锄头连锁两步流程（第一次右键饰品铲土径、第二次锄耕地）由 `rightClickPriority` 排序保证，单块回退 `freeHandUseStacks` 也已改为同序；泥土直接被锄成耕地只在"没有可铲平方块"时才是正确行为，草方块/泥土（Ultimine `shovel_flattenable` 含 `#forge:dirt`）都会先出土径。
- 未完成 / 下一步：改动未提交（用户未确认）；用户需进游戏复测饰品+锄头连锁（用新 `build/libs/freehands-forge-1.20.1-0.1.0.jar` 或 `runClient`），重点：按住 Ultimine 键右击草方块/泥土应连锁出土径，再次右击土径应连锁锄成耕地。
- 入手点：`git status` 看未提交改动（AGENTS.md、build.gradle、FreeHandEvents、IronTrinketGameTests、新增 `src/main/resources/data/ftbultimine/`）；诊断日志在会话记录里，`.cowork-temp/` 已清空。
- 维护规则：实质工作收尾时整块覆盖本标记区（agents-md-keeper skill）。
<!-- HANDOFF-END -->
