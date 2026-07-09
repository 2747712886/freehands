# Core Trinkets Agent Notes

## 项目概览

这是一个 Minecraft Forge `1.20.1` 模组项目，Forge 版本为 `47.4.10`，项目目录固定为 `D:\mod\coretrinkets`。

模组名称为 `Core Trinkets`，模组 ID 为 `coretrinkets`。当前第一阶段目标是实现“核心饰品”玩法：通过 Curios API 增加一个 `core` 饰品槽，玩家装备核心饰品后获得对应能力。

当前已实现的 MVP 内容：

- 强依赖 Curios `5.14.1+1.20.1`。
- 强依赖 Patchouli `1.20.1-85-FORGE`。
- 新增 `iron_core` 铁核心饰品。
- 新增 Curios `core` 槽位，并分配给玩家。
- 铁核心只能装备到 `core` 槽。
- 铁核心提供铁级挖掘支持、攻击力、防御值和韧性属性。
- 新增基础 Patchouli 手册内容，介绍核心系统和铁核心。

后续规划但尚未实现：

- 核心强化/升级主线。
- 特殊合成方式。
- 核心附魔兼容系统。
- 独立能量单位体系。
- 与 FE、Mana、AE 等能量系统的适配层。
- 更多核心类型和能力分支。

## 项目结构

- `src/main/java/com/yourname/coretrinkets`：Java 源码。
- `src/main/resources/assets/coretrinkets`：模型、语言等客户端资源。
- `src/main/resources/data/coretrinkets`：配方、Curios 槽位、Patchouli 手册等数据。
- `src/main/resources/data/curios/tags/items`：Curios 物品槽位标签。
- `build.gradle`：ForgeGradle、Curios、Patchouli 依赖配置。
- `gradle.properties`：Minecraft、Forge、模组元数据和依赖版本。

## 常用命令

在 `D:\mod\coretrinkets` 下运行：

```powershell
$env:GRADLE_USER_HOME='D:\mod\.gradle-user-home'
.\gradlew.bat build --no-daemon --max-workers=1 --console=plain
```

启动开发客户端：

```powershell
$env:GRADLE_USER_HOME='D:\mod\.gradle-user-home'
.\gradlew.bat runClient
```

生成 IntelliJ IDEA 运行配置：

```powershell
$env:GRADLE_USER_HOME='D:\mod\.gradle-user-home'
.\gradlew.bat genIntellijRuns
```

## 开发约束

- 不要把新模组直接放在 `D:\mod` 根目录；每个模组应使用自己的子目录。
- 本项目所有源码和资源修改应限定在 `D:\mod\coretrinkets` 内。
- 文件移动、删除、重命名前必须先明确目标路径。
- 不要使用 `Remove-Item` 对数组、通配符或目录递归执行删除。
- 如需删除多个文件，必须逐个文件单独删除。
- 不要删除 `D:\mod` 根目录旧 MDK 文件，除非用户明确要求；这些清理项应交给用户确认。
- 修改玩法能力时优先保持 MVP 可进游戏验证，不要一次性引入复杂系统。
- 新增依赖时必须确认其 Forge `1.20.1` 兼容版本，并更新 `mods.toml` 依赖声明。

## 当前验证状态

最近一次已通过：

- `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain`
- `.\gradlew.bat prepareRunClient --no-daemon --max-workers=1 --console=plain`

尚需进游戏手动验证：

- Curios UI 是否显示 `core` 槽。
- `iron_core` 是否只能放入 `core` 槽。
- 装备铁核心后空手/低级工具挖掘铁级方块是否符合预期。
- 装备铁核心后的攻击、防御属性是否符合预期。
- Patchouli 手册是否可打开并显示核心系统条目。
