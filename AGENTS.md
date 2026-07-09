# Free Hands Agent Notes

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

## 当前实现

- 注册 `freehands:iron_trinket`，中文名“铁饰品”。
- 注册 Curios 槽位 `free_hand`，槽位大小为 `2`。
- 使用 `data/curios/tags/items/free_hand.json` 限制可放入槽位的物品。
- 支持原版工具、武器、护甲和铁饰品进入解放槽。
- 挖掘能力从两个解放槽中选择最适合当前方块的工具或饰品能力。
- 攻击能力从两个解放槽中选择最高攻击伤害补足。
- 护甲值和韧性从两个解放槽中的护甲或饰品累加。
- Patchouli 手册资源保留，但 `mods.toml` 中 Patchouli 是可选依赖。

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
- 铁剑放入解放槽后，攻击伤害提高。
- 护甲放入解放槽后，护甲值变化。
- `freehands:iron_trinket` 可装备并提供铁级能力。
- 无 Patchouli 时不阻止启动；有 Patchouli 时手册可读。

