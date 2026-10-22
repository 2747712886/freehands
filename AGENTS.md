# Free Hands Agent Notes

## 仓库与发布

- 默认分支 `1.20.1`（2026-07-20 由 `master` 改名，保留完整历史）。
- 本仓库在**多台电脑间同步开发**（如 `E:\mod\freehands`、`D:\mod\freehands`），盘符因人而异，文档一律用相对路径、不写死。
- 首个公开 Release：tag `1.20.1-0.1.0`，标题 `Free Hands 0.1.0 for Minecraft Forge 1.20.1`，产物 `freehands-forge-1.20.1-0.1.0.jar`。后续发布前先跑完整 GameTest 与 `build`，并把确切 tag/标题/产物名记录在此。
- 第二个 Release：tag `1.20.1-0.2.0`（2026-09-01，提交 `a1057f5`），标题 `Free Hands 0.2.0 for Minecraft Forge 1.20.1`，产物 `freehands-forge-1.20.1-0.2.0.jar`。内容：Ultimine 连锁兼容完善（土径锄地标签、孪生包去重、饰品连锁全动作+音效+挥臂）、连锁兜底通用化、自定义工具识别、坐标版音效。发布前 47/47 GameTest 全绿、`build` 通过。

## 美术管线

- 饰品贴图**直接以成品尺寸逐像素手绘**，不经作图模型、不做降采样：`tools/art/trinkets.mjs`（无依赖 Node 脚本）把每颗像素写进代码，`node tools/art/trinkets.mjs` 直接产出 16x16 PNG，`--dry` 打印 ASCII 与色数/透明边自检。
- 配色取自原版贴图采样（`1.20.1.jar` 的 `item/iron_ingot`、`item/diamond`、`item/netherite_ingot`），保证观感原生：铁 `#ffffff/#d8d8d8/#a8a8a8/#828282/#727272/#5e5e5e/#353535`、钻石 `#d5fff6/#4aedd9/#20c5b5/#1aaaa7/#1c919a/#11727a/#145e53`、下界合金 `#737173/#5a575a/#4d494d/#4c4143/#31292a/#271c1d/#111111`（下界合金另加酒红核心 `#7c1f38/#b4536a` 与古董金铆钉 `#c9a227`）。
- 三个饰品贴图 `freehands:{iron,diamond,netherite}_trinket`：透明 16x16 圆盘徽章，外圈亮边（左上打光、右下压暗）+ 中央菱形镶嵌 + 材质宝石核心；单像素透明边、≤16 不透明色。造型在脚本里可切换：`K` 圆盘（默认）、`I` 方章、`L` 吊坠（**已否决**：顶部挂环看着像挂锁，不像饰品）。耐久 2048/4096/8192。
- 旧的 `minecraft-art-router` + `816x816 → normalize_item_texture.py --grid-locked` 路线已弃用（会引入缩放失真）；`docs/ai-image-prompts.md` 仅作历史提示词留档。
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
- 一物理右键只执行一次：两手皆空时客户端本地无法预知解放槽动作，会先后发送主手+副手两个 use 包；`FreeHandEvents.recordFreeHandHandled/isFreeHandHandled/consumeFreeHandHandled`（按玩家 UUID 记位置）供**链式路径与单块回退共用**去重——主手包生效后副手空包一律跳过（`freeHandToolRunsOnlyOnceAcrossHandPackets`、`ultimineChainActsOnlyOncePerPhysicalClick`）；副手空时单主手包也触发（`...HandlesMainHandPacketWhenOffHandIsEmpty`）。
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

- 贴图生成器 `tools/art/trinkets.mjs`（项目内、无第三方依赖）：ASCII/像素级绘制 + PNG 编码都在一个文件里，默认写 `src/main/resources/assets/freehands/textures/item/`，`--dry` 只打印自检结果。
- `minecraft-art-router`（旧本机工具）已不用于本项目：它要求 816x816 源图再 `--grid-locked` 降采样，会引入缩放失真；仅当要画全新造型的概念稿时才考虑。
- 三个饰品贴图 `iron/diamond/netherite_trinket.png` 均由上述脚本产出，模型统一 `minecraft:item/generated` + `freehands:item/<name>`。

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
- `FTBUltimineMixin` 在 `blockRightClick` 的 `RETURN` 注入：主手流程自然执行；`PASS` 则反射试副手（传 `OFF_HAND`，不设虚拟上下文；原版 `PlatformMethods` 直接读 `getItemInHand(OFF_HAND)`）；仍 `PASS` 遍历 `ultimineUseStacks`（解放槽按 `rightClickPriority` 铲→斧→锄，其余 Curios 槽按 slot order）。每件 `beginUsing` 后反射调 `MAIN_HAND`；任一阶段成功即替换返回值并取消事件，已消费右键不继续遍历（草方块铲土径与锄地分开为两次右键）。**孪生包去重**：主手事件被 Ultimine 本体或解放槽连锁处理后即 `recordFreeHandHandled`；同一物理右键随后的 `OFF_HAND` 空包命中该位置时直接跳过连锁，避免"一次点击铲平+锄地连做、下一次点击无动作"。
- 链式右键按解放槽工具判定动作分支：`PlatformMethodsImpl.blockRightClick` 依 `getItemInHand(hand).canPerformAction` 走 锄→斧→锹（右击锄地/剥皮/铲平），解放槽工具经虚拟主手命中同一分支。**土径(锹土路)锄地**：原版 `HoeItem.TILLABLES` 允许 dirt_path→farmland，但 Ultimine 的 `ftbultimine:farmland_tillable` 标签缺 `minecraft:dirt_path`，本模组以 `src/main/resources/data/ftbultimine/tags/blocks/farmland_tillable.json`（`replace:false`）合并该方块补上。另注意：Ultimine 的 `shovel_flattenable` 标签含 `#forge:dirt`，因此链式铲平对泥土/砂土也生效（泥土→土径），饰品+锄头连锁在泥土上也能走出"先土径、第二次右键再耕地"的两步流程。
- **GameTest 瞄准必须设 `yHeadRot`**：Ultimine 的 `pick` 视线读 `LivingEntity.getViewYRot`→`yHeadRot`，而 `yHeadRot` 只在 `Player.serverAiStep` 每 tick 从 `yRot` 同步；测试里 `setYRot` 后同一 tick 就 post 事件，射线会沿出生随机偏航发射、偶发 MISS，表现为连锁测试随机失败。`aimAtBlock` 必须同时 `setYHeadRot`/`setYBodyRot`。
- **GameTestServer 连锁破块偶发 NPE（SendShapePacket codec 竞态）**：FTB-Ultimine 在并行模组构造时偶发漏注册 `ftbultimine:send_shape_packet` 到 Architectury 的 `S2C_CODECS`（同源的 `sync_ultimine_time_packet` 却正常），连锁破块后 `NetworkManager.sendToPlayer(SendShapePacket)` 编码时 `codec` 为 null 直接 NPE，`ultimineDamagesFreeHandPickaxeForEveryBrokenBlock` 随机挂（约 1/3）。仅在测试服务器触发，真实客户端注册完整。已在 `GameTestSupport.ensureUltimineShapeCodecRegistered()` 反射补齐（仅测试侧，FTB 缺席优雅跳过），按下 Ultimine 键前调用。
- **连锁兜底（逐位置 useItemOn）已通用化**：普通工具先走 Ultimine 原生连锁（锄/剥/铲/收作物）；**Ultimine 未处理时**（如剪刀雕南瓜、其他模组的自定义右键交互）以及**饰品**（Ultimine 斧分支强转 `AxeItemAccess` 必炸，且单选分支表达不了多动作）都读 `cachedPositions`（校验 `isPressed` 且 `cachedPos` 等于本次点击位置，防旧缓存），对每个位置执行**完整 `gameMode.useItemOn`**（含 `block.use`，覆盖南瓜雕刻等方块侧交互），并以**方块状态是否真实变化**判定生效——不能依赖返回值：递归事件被 Ultimine 处理时 `interruptFalse.asMinecraft()` 映射为 `FAIL`（`consumesAction()==false`）；状态判据同时天然排除开箱等 GUI 交互。其他模组的右键交互只要走标准 use 流程即自动获得连锁能力。
- **右键工具识别（`canUseOnBlock`）兼容自定义工具**：除饰品/`DiggerItem`/`ShearsItem` 外，声明 `HOE_TILL/AXE_STRIP/SHOVEL_FLATTEN/SHEARS_CARVE` 任一 ToolAction 的物品也可进入解放槽右键系统（单块+连锁），覆盖不继承原版工具类的模组工具。
- **右键反馈（音效+挥臂）**：饰品去皮/刮锈/去蜡经 `applyModifiedState(…, SoundEvent)` 播放 `AXE_STRIP/AXE_SCRAPE/AXE_WAX_OFF`（对齐原版斧头；铲平/熄营火/成熟藤蔓本就有声，锄地声来自 Ultimine 本体）。解放槽动作客户端无法预测，玩家自己看不到挥臂：Ultimine 内部 `swing` 是 `updateSelf=false` 只广播给他人，故连锁配件/副手重试生效后由 mixin 追加 `swing(MAIN_HAND, true)`（`broadcastAndSend` 发给自己），与单块回退一致。
- 测试：`ultimineChainsFreeHandShovelRightClick`（右键连锁两草方块铲土径、扣 2 耐久）、`ultimineChainsFreeHandHoeOnDirt`（锄连锁两泥土→耕地）、`ultimineChainsFreeHandHoeOnDirtPath`（锄连锁两土径→耕地，依赖上方标签合并）、`ultimineChainsFreeHandAxeStripsLogs`（斧连锁剥两原木）、`ultimineChainsFreeHandShearsCarvePumpkins`（剪刀连锁雕两南瓜，锁定"Ultimine 不认识的动作走逐位置 useOn 兜底"的通用化行为）、`ultimineChainsIronTrinketFlattensGrass`（饰品连锁铲两草方块）、`ultimineChainsIronTrinketStripsLogs`（饰品连锁剥两原木、扣 2 耐久，锁定饰品逐位置连锁处理）、`ultimineTrinketFlattensDirtBeforeHoeTills`（饰品+锄头：第一次连锁右键饰品把两泥土铲成土径、扣饰品 2 耐久且锄头不掉耐久，第二次连锁右键锄头把两土径锄成耕地、扣锄头 2 耐久）、`ultimineHoeInEarlierSlotStillFlattensFirstViaTrinket`（锄头占前槽时仍由饰品先铲平，顺序不受槽位影响）、`ultimineChainTriggersThroughRealUseItemOnFlow`（不手动 post 事件，走真实 `gameMode.useItemOn` 包路径触发连锁）、`ultimineChainActsOnlyOncePerPhysicalClick`（模拟客户端孪生包：每次点击 post 主手+副手两个事件，断言第一击只铲平、锄头不掉耐久，第二击才锄成耕地）、`ultimineDamagesFreeHandPickaxeForEveryBrokenBlock`（左键连锁破两石头、扣 2 耐久）。**断言最终方块状态与耐久，不断言最外层返回值**（Ultimine 递归破坏并取消最外层 `destroyBlock`）。测试用静默网络连接只用于避免人工 `ServerPlayer` 缺客户端连接，不得移入生产代码。

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
- 更新时间：2026-10-22 14:46
- 当前分支：`1.21.1`（NeoForge 主线），HEAD 与 `origin/1.21.1` 同步；`1.20.1`（Forge）已发布 0.2.0，作为遗留线仅收紧急修复。工作树同一时刻只检出其一。
- 本次完成：**三档饰品贴图重做**。改为"直接用像素填成品尺寸"：`tools/art/trinkets.mjs`（无依赖 Node）逐像素绘制并直接输出 16x16 PNG，不经作图模型、不做降采样；配色从原版贴图采样（`1.20.1.jar` 的 `item/iron_ingot`、`item/diamond`、`item/netherite_ingot`），下界合金另加酒红核心 + 古董金铆钉。造型三候选内置于脚本：`K` 圆盘徽章（**已选用**，外圈亮边 + 菱形镶嵌 + 宝石核心）、`I` 方章、`L` 吊坠（**已否决**，顶部挂环看着像挂锁）；切换只要 `node tools/art/trinkets.mjs I` 再重载资源。用户评价 K/I 仍偏「平庸」，后续可继续迭代造型（已试过刻面星、珠边齿轮两版概念，未采用）。三个贴图均满足透明 1px 边、≤16 不透明色（实测 7/7/12 色）。验证：`build` 通过、`runGameTestServer` **48/48 全绿**。
- 硬约束（贴图相关）：`minecraft-art-router` 的 `816x816 → normalize_item_texture.py --grid-locked` 老路线已弃用（降采样会糊），`docs/ai-image-prompts.md` 仅留作历史提示词。
- devmods 现状（2026-10-22 复核）：IMBlocker 上游已到 `5.6.2.1`、FTB Library 到 `2101.1.37`，但 `build.gradle` 仍钉 `5.6.0` / `2101.1.35`（运行正常，未升级）；JEI `19.51.0.418` 仍是最新 release（更新的 19.57.x 只有 beta）、JEC `4.5.29`、FTB Ultimine `2101.1.15`、Architectury `13.0.11` 均为当前最新。
- 🔴 连锁静默失效的根因（下次遇到务必先看）：Architectury `PlayerHooks.isFake` 是 `player instanceof ServerPlayer && player.getClass() != ServerPlayer.class`，FTB Ultimine 2101 在入口就判 isFake → **GameTest 里的连锁玩家必须是 `ServerPlayer` 本体**（子类会让 12 个连锁测试全静默）。
- 环境（不变，照做）：所有 gradlew 命令前置 `JAVA_HOME="D:\\Gradle\\jdk-21-temurin"`；增量 `build` ~8-14s、整包 `build runGameTestServer` ~2m20s（GameTest 本体约 5s）。
- 未完成 / 下一步：① dev 客户端里逐个看 L/K/I 三种造型手感（`node tools/art/trinkets.mjs K` → 重启 runClient）；② 若要 32x32 高清包或像素动画帧，在同一脚本里加分辨率参数即可；③ 正文《常用命令》《验证清单》仍混 1.20.1/Forge 口径。
- 入手点：`node tools/art/trinkets.mjs --dry`（不出图，只看 ASCII 与自检）；改完跑 `JAVA_HOME=/d/Gradle/jdk-21-temurin ./gradlew build runGameTestServer`。
- 维护规则：实质工作收尾时整块覆盖本标记区（agents-md-keeper skill）。
<!-- HANDOFF-END -->