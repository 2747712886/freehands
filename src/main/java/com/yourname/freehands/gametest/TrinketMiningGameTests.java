package com.yourname.freehands.gametest;

import com.yourname.freehands.FreeHands;
import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.yourname.freehands.gametest.GameTestSupport.enchant;
import static com.yourname.freehands.gametest.GameTestSupport.enchantHolder;
import static com.yourname.freehands.gametest.GameTestSupport.enchantLevel;
import static com.yourname.freehands.gametest.GameTestSupport.equipTrinket;
import static com.yourname.freehands.gametest.GameTestSupport.newServerPlayer;
import static com.yourname.freehands.gametest.GameTestSupport.spawnPlayer;

/**
 * 饰品挖掘、采集掉落与附魔测试（NeoForge 1.21.1）：镐类采集、耐久损耗、草/树叶掉落规则、
 * 可附魔性，以及虚拟主手在嵌套挖掘中的稳定性。
 */
@GameTestHolder(FreeHands.MODID)
@PrefixGameTestTemplate(false)
public final class TrinketMiningGameTests {
    private TrinketMiningGameTests() {
    }

    @GameTest(template = "empty")
    public static void ironTrinketMinesStoneAndDropsCobblestone(GameTestHelper helper) {
        BlockPos stonePos = new BlockPos(1, 2, 1);
        Player player = spawnPlayer(helper);
        equipTrinket(player, ModItems.IRON_TRINKET.get().getDefaultInstance());
        helper.setBlock(stonePos, Blocks.STONE);

        helper.runAfterDelay(2, () -> {
            BlockPos absoluteStonePos = helper.absolutePos(stonePos);
            VirtualMainHandContext.beginMining(player, Blocks.STONE.defaultBlockState());
            boolean canHarvest = Blocks.STONE.defaultBlockState().canHarvestBlock(helper.getLevel(), absoluteStonePos, player);
            if (canHarvest) {
                Blocks.STONE.playerDestroy(helper.getLevel(), player, absoluteStonePos,
                        Blocks.STONE.defaultBlockState(), null, player.getMainHandItem());
            }
            VirtualMainHandContext.endMining(player);
            helper.assertTrue(canHarvest, "An Iron Trinket should harvest stone as a pickaxe");
            helper.assertItemEntityPresent(Items.COBBLESTONE, stonePos, 2.0D);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketLosesDurabilityWhenMiningStone(GameTestHelper helper) {
        BlockPos stonePos = new BlockPos(1, 2, 1);
        Player player = spawnPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(stonePos, Blocks.STONE);

        helper.runAfterDelay(2, () -> {
            trinket.mineBlock(helper.getLevel(), Blocks.STONE.defaultBlockState(), helper.absolutePos(stonePos), player);
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "Mining stone with an Iron Trinket should consume one durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketOnGrassDropsSeedsNotGrassBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        Player player = spawnPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(pos, Blocks.SHORT_GRASS);

        helper.runAfterDelay(2, () -> {
            BlockPos absolute = helper.absolutePos(pos);
            VirtualMainHandContext.beginMining(player, Blocks.SHORT_GRASS.defaultBlockState());
            Blocks.SHORT_GRASS.playerDestroy(helper.getLevel(), player, absolute,
                    Blocks.SHORT_GRASS.defaultBlockState(), null, player.getMainHandItem());
            VirtualMainHandContext.endMining(player);
            helper.assertItemEntityNotPresent(Items.SHORT_GRASS, pos, 2.0D);
            helper.assertTrue(trinket.getDamageValue() == 0,
                    "An Iron Trinket should not participate in breaking grass");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void shearsInFreeHandOnGrassDropsGrassBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        Player player = spawnPlayer(helper);
        ItemStack shears = new ItemStack(Items.SHEARS);
        equipTrinket(player, shears);
        helper.setBlock(pos, Blocks.SHORT_GRASS);

        helper.runAfterDelay(2, () -> {
            BlockPos absolute = helper.absolutePos(pos);
            VirtualMainHandContext.beginMining(player, Blocks.SHORT_GRASS.defaultBlockState());
            Blocks.SHORT_GRASS.playerDestroy(helper.getLevel(), player, absolute,
                    Blocks.SHORT_GRASS.defaultBlockState(), null, player.getMainHandItem());
            VirtualMainHandContext.endMining(player);
            helper.assertItemEntityPresent(Items.SHORT_GRASS, pos, 2.0D);
            helper.assertTrue(shears.getDamageValue() == 0,
                    "Shears should not lose durability when breaking grass");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketOnLeavesDoesNotDropLeavesBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        Player player = spawnPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(pos, Blocks.OAK_LEAVES);

        helper.runAfterDelay(2, () -> {
            BlockPos absolute = helper.absolutePos(pos);
            VirtualMainHandContext.beginMining(player, Blocks.OAK_LEAVES.defaultBlockState());
            Blocks.OAK_LEAVES.playerDestroy(helper.getLevel(), player, absolute,
                    Blocks.OAK_LEAVES.defaultBlockState(), null, player.getMainHandItem());
            VirtualMainHandContext.endMining(player);
            helper.assertItemEntityNotPresent(Items.OAK_LEAVES, pos, 2.0D);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketCanBeEnchanted(GameTestHelper helper) {
        Player player = spawnPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(trinket.isEnchantable(),
                    "An Iron Trinket must be enchantable");
            helper.assertTrue(trinket.getItem().getEnchantmentValue() > 0,
                    "An Iron Trinket must have a positive enchantability value");

            // 1.21.1 附魔可行性改由 #minecraft:enchantable/* 物品标签数据驱动，isEnchantable()/enchant() 都不走这层判定
            helper.assertTrue(trinket.supportsEnchantment(enchantHolder(helper.getLevel(), Enchantments.EFFICIENCY)),
                    "An Iron Trinket must accept Efficiency (#minecraft:enchantable/mining)");
            helper.assertTrue(trinket.supportsEnchantment(enchantHolder(helper.getLevel(), Enchantments.PROTECTION)),
                    "An Iron Trinket must accept Protection (#minecraft:enchantable/armor)");
            helper.assertTrue(trinket.supportsEnchantment(enchantHolder(helper.getLevel(), Enchantments.SHARPNESS)),
                    "An Iron Trinket must accept Sharpness (#minecraft:enchantable/sharp_weapon + sword)");

            enchant(helper.getLevel(), trinket, Enchantments.EFFICIENCY, 1);
            helper.assertTrue(enchantLevel(trinket, Enchantments.EFFICIENCY) == 1,
                    "An Iron Trinket should accept Efficiency I; actual level: " + enchantLevel(trinket, Enchantments.EFFICIENCY));

            enchant(helper.getLevel(), trinket, Enchantments.PROTECTION, 1);
            helper.assertTrue(enchantLevel(trinket, Enchantments.PROTECTION) == 1,
                    "An Iron Trinket should accept Protection I; actual level: " + enchantLevel(trinket, Enchantments.PROTECTION));

            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void virtualMiningSurvivesNestedDestroyBlock(GameTestHelper helper) {
        ServerPlayer player = newServerPlayer(helper);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        equipTrinket(player, pickaxe);

        helper.runAfterDelay(2, () -> {
            VirtualMainHandContext.beginMining(player, Blocks.STONE.defaultBlockState());
            try {
                VirtualMainHandContext.beginMining(player, Blocks.STONE.defaultBlockState());
                try {
                    helper.assertTrue(player.getMainHandItem() == pickaxe,
                            "Nested destroyBlock calls must retain the free-hand virtual main hand");
                } finally {
                    VirtualMainHandContext.endMining(player);
                }

                helper.assertTrue(player.getMainHandItem() == pickaxe,
                        "Completing an inner destroyBlock call must not clear the outer virtual main hand");
            } finally {
                VirtualMainHandContext.endMining(player);
            }
            helper.succeed();
        });
    }
}
