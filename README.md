# Free Hands · 解放双手

一个 Minecraft Forge `1.20.1` 模组，通过 Curios 增加两个解放槽，在不占用主副手的情况下使用工具、武器、护甲和饰品。
A Minecraft Forge 1.20.1 mod — two Curios slots for tools, weapons, armor, and trinkets, hands-free.

Forge `47.4.10` · Curios `5.14.1`

## 解放槽

两个 `free_hand` Curios 槽位。可放入原版工具、武器、护甲、剪刀及本模组饰品。
Two `free_hand` Curios slots for vanilla tools, weapons, armor, shears, and trinkets.

## 饰品

| | 耐久 | 挖掘 | 攻击 | 护甲 | 韧性 | 击退 | 附魔值 |
|---|---|---|---|---|---|---|---|
| 铁 Iron | 2048 | 铁级 | 铁剑 | 全套铁 | — | — | 14 |
| 钻石 Diamond | 4096 | 钻石级 | 钻石剑 | 全套钻石 | 全套钻石 | — | 10 |
| 下界合金 Netherite | 8192 | 下界合金级 | 下界合金剑 | 全套下界合金 | 全套下界合金 | 全套 | 15 |

饰品可在附魔台使用所有工具和护甲附魔。
Trinkets accept all tool and armor enchantments at the enchanting table.

## 挖掘

解放槽中每方块自动选择最优工具。主手无对应速度优势时，被选工具充当虚拟主手参与原版破坏流程，物品栏不变。
Best free-hand tool auto-selected per block. When the main hand has no speed bonus, the tool acts as a virtual main hand — drops, enchantments, durability all follow vanilla.

剪刀在解放槽中对植物、蜘蛛网等掉落与原版一致。饰品不接管瞬破植物，也不干预有特定工具战利品表的方块。
Shears in free hand give correct drops for plants, cobwebs, etc. Trinkets skip instant-break plants and tool-specific loot.

## 右键方块

优先级：放置方块 → 主手 → 副手 → 解放槽（按槽位顺序）。
Priority: block placement → main hand → off hand → free hand (slot order).

前一步返回 `PASS` 才继续下一项；主手/副手均 `PASS` 时解放槽接管。同一物理右键只执行一次饰品工具。
Only `PASS` advances; free-hand fallback on dual `PASS`. One physical click runs free-hand tools once.

手持物品返回 `PASS` 时解放槽同样接管，如铲子铲不动土径则锄头耕地。
Main-hand item returning `PASS` still falls through — e.g. shovel on dirt path, hoe tills.

成功时摆动主手，返回 `CONSUME`。
On success: swings MAIN_HAND, returns CONSUME.

## 战斗

解放槽中伤害最高的武器额外叠加基础伤害、锋利/亡灵/节肢杀手、火焰附加和抢夺。额外伤害继承暴击倍率，仅实际参与叠伤的武器消耗耐久。
Highest-damage free-hand weapon adds base damage + Sharpness/Smite/Bane + Fire Aspect + Looting. Bonus inherits crit; only contributor loses durability.

## 防御

解放槽护甲通过 transient attribute 添加护甲值和韧性。受伤时消耗耐久，保护类附魔参与减伤。饰品通过 Curios 属性接口提供防御。
Free-hand armor adds points/toughness via transient attributes. Damage consumes durability; Protection enchants reduce damage. Trinkets use Curios attribute interface.

## 耐久

使用原版 `Unbreakable` 标记，没有自定义耐久白名单。
Vanilla `Unbreakable` tag only; no custom whitelist.

## 依赖

| | 版本 | 必须 |
|---|---|---|
| Minecraft | 1.20.1 | 是 |
| Forge | 47.4.10 | 是 |
| Curios | 5.14.1+1.20.1 | 是 |

## 构建

```powershell
$env:GRADLE_USER_HOME='E:\mod\.gradle-user-home'
.\gradlew.bat build --no-daemon --max-workers=1 --console=plain
```

产物 `build/libs/freehands-0.1.0.jar`

启动客户端：

```powershell
.\gradlew.bat runClient
```

自动化测试（38 项）：

```powershell
.\gradlew.bat runGameTestServer --no-daemon --max-workers=1 --console=plain
```

## 许可证

保留所有权利。All rights reserved.