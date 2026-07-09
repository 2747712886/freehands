# 解放双手 / Free Hands

`解放双手` 是一个 Minecraft Forge `1.20.1` 模组，基于 Forge `47.4.10` 开发。

模组通过 Curios 增加两个“解放槽”，允许玩家把原版工具、武器、护甲和本模组饰品装备到槽位中，并在不占用主副手的情况下获得对应功能。

## 当前目标

- 模组 ID：`freehands`
- 主包名：`com.yourname.freehands`
- 当前开发目录：`D:\mod\freehands`
- 项目目录：`D:\mod\freehands`
- GitHub 仓库计划改名为：`2747712886/freehands`

## 已实现内容

- 新增 Curios 槽位类型 `free_hand`，大小为 `2`。
- 中文显示名为“解放槽”。
- `free_hand` 槽允许装备原版工具、武器、护甲和 `freehands:iron_trinket`。
- 装备工具时，挖掘等级和挖掘速度按槽内最适合当前方块的工具补足。
- 装备武器或工具时，攻击伤害按槽内最高攻击力补足。
- 装备护甲时，护甲值和韧性从两个槽位累加。
- 原 `iron_core` 已重构为 `iron_trinket` / “铁饰品”。
- Patchouli 手册改为可选内容，未安装 Patchouli 不应阻止模组启动。

## 不再作为目标

以下旧规划已取消，不要按旧核心升级路线继续扩展：

- 核心升级主线。
- 特殊合成升级系统。
- 核心附魔扩展。
- 自定义能量单位。
- FE、Mana、AE 等能量兼容层。

## 依赖

必需：

- Minecraft `1.20.1`
- Forge `47.4.10`
- Curios `5.14.1+1.20.1`

可选：

- Patchouli `1.20.1-84.1-FORGE`

## 构建

从项目目录运行：

```powershell
$env:GRADLE_USER_HOME='D:\mod\.gradle-user-home'
.\gradlew.bat build --no-daemon --max-workers=1 --console=plain
```

构建产物位于：

```text
build/libs/freehands-0.1.0.jar
```

## IntelliJ IDEA

打开项目目录后，先生成运行配置：

```powershell
$env:GRADLE_USER_HOME='D:\mod\.gradle-user-home'
.\gradlew.bat genIntellijRuns
```

然后在 IDEA 的 Gradle 面板或运行配置中启动：

```text
runClient
```

## 手动测试

- 客户端能正常启动。
- Curios UI 显示两个“解放槽”。
- 铁镐可放入解放槽，空手能挖铁镐等级方块。
- 铁剑可放入解放槽，攻击伤害提高。
- 护甲可放入解放槽，护甲值变化。
- 铁饰品可放入解放槽，并提供铁级挖掘、攻击和基础防御。
- 未安装 Patchouli 时，模组仍能启动。
- 安装 Patchouli 时，手册内容可读取。

