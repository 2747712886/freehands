# Free Hands Agent Notes

## Art Pipeline Update

- For `minecraft-art-router` 16x16 item textures generated with `gpt-image-2`, use the minimum valid square source `816x816` (`16x51`). Verify the returned source is exactly `816x816`; regenerate any other size before running grid-locked normalization.
- `freehands:iron_trinket` uses a transparent 16x16 centered orbital iron-core texture: a forged core with four symmetric orbit nodes representing the combined tool, weapon, and armor role.
- `freehands:iron_trinket` has 2048 durability. Its shaped recipe alternates the four iron armor pieces with iron sword, axe, shovel, and pickaxe around a central iron block.
- Development client test mods: JEI `15.20.0.133`, IMBlocker `4.0.5`, and JustEnoughCharacters `4.6.7`. `downloadDevelopmentClientMods` fetches verified Forge JARs into `run/dev-mods`; the local flatDir repository exposes them to `runtimeOnly fg.deobf(...)` for userdev remapping. Do not add them as published-mod dependencies.

## 项目概览

本仓库是 `解放双手 / Free Hands` Minecraft Forge 模组。

- 当前开发路径：`D:\mod\freehands`
- 项目路径：`D:\mod\freehands`
- Minecraft：`1.20.1`
- Forge：`47.4.10`
- Mod ID：`freehands`
- 主包名：`com.yourname.freehands`
- 必需依赖：Curios
- 可选依赖：Patchouli

核心玩法是通过 Curios 增加两个 `free_hand` 解放槽。玩家可把原版工具、武器、护甲，以及本模组饰品装备到槽中，从而在不占用主副手的情况下获得对应功能。

## 个人工具

- `minecraft-art-router` 位于 `C:\Users\a2747\.codex\skills\minecraft-art-router`，用于路由 Minecraft 美术资源请求。当前支持物品贴图：通过 `imagegen` 生成严格 `16x16` 网格比例的概念稿，再用其 `normalize_item_texture.py --grid-locked` 脚本输出合规的透明 PNG 和最近邻预览。
- `freehands:iron_trinket` 使用自有 `assets/freehands/textures/item/iron_trinket.png`：透明 `16x16` 居中轨道铁芯贴图，锻造核心和四个对称轨道节点表达工具、武器与护甲能力，物品模型引用 `freehands:item/iron_trinket`。

## 当前实现

- 注册 `freehands:iron_trinket`，中文名“铁饰品”。
- 注册 Curios 槽位 `free_hand`，槽位大小为 `2`。
- 使用 `data/curios/tags/items/free_hand.json` 限制可放入槽位的物品。
- 支持原版工具、武器、护甲和铁饰品进入解放槽。
- 挖掘能力从两个解放槽中选择最适合当前方块的工具或饰品能力。
- 当主手对目标方块没有速度加成时，解放槽中最适合的工具会作为虚拟主手参与完整原版破坏流程。真实主手物品栏不变，但工具回调、掉落附魔和耐久都读取解放槽工具。
- 攻击时从两个解放槽中选择最高攻击伤害叠加到本次伤害，并对对应武器或工具扣耐久。
- 攻击附魔当前支持锋利类伤害、火焰附加和抢夺等级补足。
- 护甲值和韧性从两个解放槽中的护甲或饰品累加，槽内护甲受伤时扣耐久，并通过保护类附魔补充减伤。
- `freehands:no_durability_cost` 物品标签用于保留“使用但不掉耐久”的后续扩展点。
- Patchouli 手册资源保留，但 `mods.toml` 中 Patchouli 是可选依赖。

## 能力实现规则

- 挖掘优先级：主手对目标方块的挖掘速度大于基础速度时，始终优先主手；否则使用解放槽中最适合的工具。
- 挖掘实现：`FreeHandEvents` 通过采集检查和破坏速度事件选择解放槽工具；Mixin 会在 `ServerPlayerGameMode.destroyBlock` 的范围内将其暴露为虚拟主手。不要写入或交换真实主手物品栏。时运、精准采集、效率、工具回调和耐久都应走原版流程。
- Curios 读取：不要使用已弃用的 `findCurios(LivingEntity, String...)`；读取 `free_hand` 槽时使用 `CuriosApi.getCuriosInventory(player)`、`getStacksHandler(FreeHands.FREE_HAND_SLOT)` 和 `IDynamicStackHandler`。
- 攻击逻辑：解放槽最高攻击值作为额外伤害叠加，不替换主手伤害；实际参与叠加的槽内武器或工具需要扣耐久。
- 攻击附魔：攻击时只选择伤害最高的解放槽物品；当前显式补充锋利类伤害、火焰附加和该物品的抢夺等级，额外伤害继承暴击倍率。其他特殊攻击附魔不要默认认为已完整兼容。
- 防御逻辑：解放槽护甲属性通过 transient attribute modifier 提供；保护类附魔和护甲耐久在受伤事件中补充处理。
- 无耐久扩展：不要删除 `freehands:no_durability_cost` 标签；后续制作无耐久工具时优先把物品加入该标签，而不是硬编码判断。

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
- `src/main/resources/data/freehands`：配方、Curios 槽位数据和 Patchouli 手册数据。
- `src/main/resources/data/curios/tags/items/free_hand.json`：Curios 槽位物品白名单。
- `build.gradle`：ForgeGradle、仓库和依赖配置。
- `gradle.properties`：Minecraft、Forge、模组元数据、Curios 和 Patchouli 版本。

## 常用命令

命令从当前项目目录 `D:\mod\freehands` 运行。

构建：

```powershell
$env:GRADLE_USER_HOME='D:\mod\.gradle-user-home'
.\gradlew.bat build --no-daemon --max-workers=1 --console=plain
```

启动客户端：

```powershell
$env:GRADLE_USER_HOME='D:\mod\.gradle-user-home'
.\gradlew.bat runClient
```

生成 IntelliJ 运行配置：

```powershell
$env:GRADLE_USER_HOME='D:\mod\.gradle-user-home'
.\gradlew.bat genIntellijRuns
```

## 开发约束

- 任何文件操作先确认目标路径，尤其是删除、移动、重命名。
- 不要使用 `Remove-Item` 对数组、通配符或目录递归执行删除。
- 如果需要清理多个文件，必须逐个文件单独执行。
- 复杂任务使用 todo list。
- Markdown 文件必须保持 UTF-8，避免中文乱码。
- 不要把其他模组项目直接混进当前项目；多个模组应放在 `D:\mod` 下的独立子目录。
- 不要删除 `D:\mod` 根目录文件，除非用户明确要求。
- 不要重新引入旧 `coretrinkets` 命名。

## 运行时注意

Patchouli 在 Forge 开发运行环境中需要 Mixin refmap remapping。保留 Forge run 配置中的：

```groovy
property 'mixin.env.remapRefMap', 'true'
property 'mixin.env.refMapRemappingFile', "${buildDir}/createSrgToMcp/output.srg"
```

删除这些配置可能导致安装 Patchouli 的 `runClient` 在 Mixin 阶段失败。

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
- `freehands:iron_trinket` 可装备并提供铁级能力。
- 无 Patchouli 时不阻止启动；有 Patchouli 时手册可读。
