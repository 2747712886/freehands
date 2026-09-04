package com.yourname.freehands.gametest;

import com.yourname.freehands.FreeHands;
import com.yourname.freehands.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.yourname.freehands.gametest.GameTestSupport.equipTrinket;
import static com.yourname.freehands.gametest.GameTestSupport.pressUltimineKey;
import static com.yourname.freehands.gametest.GameTestSupport.spawnServerPlayer;

/**
 * FTB Ultimine 连锁兼容测试（NeoForge 1.21.1）。
 * <p>
 * {@code run/dev-mods} 里有 FTB Ultimine（NeoForge 版）三件套时，这些用例真跑连锁断言；
 * 缺失时 {@code pressUltimineKey} 返回 false，用例直接通过（跳过），干净构建不受影响。
 * 依赖可用 {@code ./gradlew downloadDevelopmentMods} 重新拉取。
 */
@GameTestHolder(FreeHands.MODID)
@PrefixGameTestTemplate(false)
public final class UltimineChainGameTests {
    private UltimineChainGameTests() {
    }

    private static PlayerInteractEvent.RightClickBlock postRightClick(ServerPlayer player, InteractionHand hand,
                                                                      BlockPos absolutePos, BlockHitResult hit) {
        PlayerInteractEvent.RightClickBlock event =
                new PlayerInteractEvent.RightClickBlock(player, hand, absolutePos, hit);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    // ==================== 连锁挖掘 ====================

    @GameTest(template = "empty")
    public static void ultimineDamagesFreeHandPickaxeForEveryBrokenBlock(GameTestHelper helper) {
        BlockPos firstStonePos = new BlockPos(1, 2, 1);
        BlockPos secondStonePos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        equipTrinket(player, pickaxe);
        helper.setBlock(firstStonePos, Blocks.STONE);
        helper.setBlock(secondStonePos, Blocks.STONE);

        helper.runAfterDelay(2, () -> {
            BlockPos absoluteFirstStonePos = helper.absolutePos(firstStonePos);
            if (!pressUltimineKey(player, absoluteFirstStonePos)) {
                helper.succeed();
                return;
            }

            player.gameMode.destroyBlock(absoluteFirstStonePos);
            helper.assertBlockNotPresent(Blocks.STONE, firstStonePos);
            helper.assertBlockNotPresent(Blocks.STONE, secondStonePos);
            helper.assertTrue(pickaxe.getDamageValue() == 2,
                    "Ultimine must consume one free-hand pickaxe durability for every broken stone block");
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "Ultimine must not move the free-hand pickaxe into the physical main hand");
            helper.succeed();
        });
    }

    // ==================== 连锁右键：单工具 ====================

    @GameTest(template = "empty")
    public static void ultimineChainsFreeHandShovelRightClick(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        BlockPos adjacentGrassPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        equipTrinket(player, shovel);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);
        helper.setBlock(adjacentGrassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteGrassPos = helper.absolutePos(grassPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteGrassPos), Direction.UP, absoluteGrassPos, false);
            if (!pressUltimineKey(player, absoluteGrassPos)) {
                helper.succeed();
                return;
            }

            PlayerInteractEvent.RightClickBlock event = postRightClick(player, InteractionHand.MAIN_HAND, absoluteGrassPos, hit);
            helper.assertTrue(event.isCanceled(),
                    "Ultimine must consume a free-hand shovel right-click after it chains the block action");
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, adjacentGrassPos);
            helper.assertTrue(shovel.getDamageValue() == 2,
                    "Ultimine must consume one free-hand shovel durability for every flattened block");
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "A free-hand tool must not move into the physical main hand");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ultimineChainsFreeHandHoeOnDirt(GameTestHelper helper) {
        BlockPos firstDirtPos = new BlockPos(1, 2, 1);
        BlockPos secondDirtPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, hoe);
        helper.setBlock(firstDirtPos, Blocks.DIRT);
        helper.setBlock(secondDirtPos, Blocks.DIRT);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteFirstDirtPos = helper.absolutePos(firstDirtPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteFirstDirtPos), Direction.UP, absoluteFirstDirtPos, false);
            if (!pressUltimineKey(player, absoluteFirstDirtPos)) {
                helper.succeed();
                return;
            }

            PlayerInteractEvent.RightClickBlock event = postRightClick(player, InteractionHand.MAIN_HAND, absoluteFirstDirtPos, hit);
            helper.assertTrue(event.isCanceled(),
                    "Ultimine must consume a free-hand hoe right-click after it chains the till action");
            helper.assertBlockPresent(Blocks.FARMLAND, firstDirtPos);
            helper.assertBlockPresent(Blocks.FARMLAND, secondDirtPos);
            helper.assertTrue(hoe.getDamageValue() == 2,
                    "Ultimine must consume one free-hand hoe durability for every tilled block");
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "A free-hand tool must not move into the physical main hand");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ultimineChainsFreeHandHoeOnDirtPath(GameTestHelper helper) {
        BlockPos firstPathPos = new BlockPos(1, 2, 1);
        BlockPos secondPathPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, hoe);
        helper.setBlock(firstPathPos, Blocks.DIRT_PATH);
        helper.setBlock(secondPathPos, Blocks.DIRT_PATH);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteFirstPathPos = helper.absolutePos(firstPathPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteFirstPathPos), Direction.UP, absoluteFirstPathPos, false);
            if (!pressUltimineKey(player, absoluteFirstPathPos)) {
                helper.succeed();
                return;
            }

            postRightClick(player, InteractionHand.MAIN_HAND, absoluteFirstPathPos, hit);
            helper.assertBlockPresent(Blocks.FARMLAND, firstPathPos);
            helper.assertBlockPresent(Blocks.FARMLAND, secondPathPos);
            helper.assertTrue(hoe.getDamageValue() == 2,
                    "Ultimine must consume one free-hand hoe durability for every tilled dirt path");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ultimineChainsIronTrinketFlattensGrass(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        BlockPos adjacentGrassPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);
        helper.setBlock(adjacentGrassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteGrassPos = helper.absolutePos(grassPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteGrassPos), Direction.UP, absoluteGrassPos, false);
            if (!pressUltimineKey(player, absoluteGrassPos)) {
                helper.succeed();
                return;
            }

            PlayerInteractEvent.RightClickBlock event = postRightClick(player, InteractionHand.MAIN_HAND, absoluteGrassPos, hit);
            helper.assertTrue(event.isCanceled(),
                    "Ultimine must consume an Iron Trinket right-click after it chains the flatten action");
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, adjacentGrassPos);
            helper.assertTrue(trinket.getDamageValue() == 2,
                    "Ultimine must consume one trinket durability for every flattened block");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ultimineChainTriggersThroughRealUseItemOnFlow(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        BlockPos adjacentGrassPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);
        helper.setBlock(adjacentGrassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteGrassPos = helper.absolutePos(grassPos);
            if (!pressUltimineKey(player, absoluteGrassPos)) {
                helper.succeed();
                return;
            }

            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteGrassPos), Direction.UP, absoluteGrassPos, false);
            player.gameMode.useItemOn(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                    InteractionHand.MAIN_HAND, hit);
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, adjacentGrassPos);
            helper.assertTrue(trinket.getDamageValue() == 2,
                    "The trinket must chain-flatten both grass blocks through the real useItemOn flow");
            helper.succeed();
        });
    }

    // ==================== 连锁右键：去重与两步流转 ====================

    @GameTest(template = "empty")
    public static void ultimineChainActsOnlyOncePerPhysicalClick(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        BlockPos adjacentGrassPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, 0, trinket);
        equipTrinket(player, 1, hoe);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);
        helper.setBlock(adjacentGrassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteGrassPos = helper.absolutePos(grassPos);
            if (!pressUltimineKey(player, absoluteGrassPos)) {
                helper.succeed();
                return;
            }

            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteGrassPos), Direction.UP, absoluteGrassPos, false);
            postRightClick(player, InteractionHand.MAIN_HAND, absoluteGrassPos, hit);
            postRightClick(player, InteractionHand.OFF_HAND, absoluteGrassPos, hit);
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, adjacentGrassPos);
            helper.assertTrue(trinket.getDamageValue() == 2,
                    "The trinket must flatten both grass blocks on the first physical right-click");
            helper.assertTrue(hoe.getDamageValue() == 0,
                    "The off-hand twin packet must not let the hoe till during the flatten click");

            postRightClick(player, InteractionHand.MAIN_HAND, absoluteGrassPos, hit);
            postRightClick(player, InteractionHand.OFF_HAND, absoluteGrassPos, hit);
            helper.assertBlockPresent(Blocks.FARMLAND, grassPos);
            helper.assertBlockPresent(Blocks.FARMLAND, adjacentGrassPos);
            helper.assertTrue(hoe.getDamageValue() == 2,
                    "The second physical right-click must till both dirt paths via the hoe");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ultimineTrinketFlattensDirtBeforeHoeTills(GameTestHelper helper) {
        BlockPos firstDirtPos = new BlockPos(1, 2, 1);
        BlockPos secondDirtPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, 0, trinket);
        equipTrinket(player, 1, hoe);
        helper.setBlock(firstDirtPos, Blocks.DIRT);
        helper.setBlock(secondDirtPos, Blocks.DIRT);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteFirstDirtPos = helper.absolutePos(firstDirtPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteFirstDirtPos), Direction.UP, absoluteFirstDirtPos, false);
            if (!pressUltimineKey(player, absoluteFirstDirtPos)) {
                helper.succeed();
                return;
            }

            postRightClick(player, InteractionHand.MAIN_HAND, absoluteFirstDirtPos, hit);
            helper.assertTrue(helper.getBlockState(firstDirtPos).is(Blocks.DIRT_PATH),
                    "The first chain right-click on dirt must flatten it to a dirt path via the trinket");
            helper.assertBlockPresent(Blocks.DIRT_PATH, secondDirtPos);
            helper.assertTrue(trinket.getDamageValue() == 2,
                    "The trinket must flatten both dirt blocks before the hoe can till");
            helper.assertTrue(hoe.getDamageValue() == 0,
                    "The hoe must not till during the trinket's flatten right-click");

            postRightClick(player, InteractionHand.MAIN_HAND, absoluteFirstDirtPos, hit);
            helper.assertTrue(helper.getBlockState(firstDirtPos).is(Blocks.FARMLAND),
                    "A later chain right-click on the dirt path must till it to farmland via the hoe");
            helper.assertBlockPresent(Blocks.FARMLAND, secondDirtPos);
            helper.assertTrue(hoe.getDamageValue() == 2,
                    "The hoe must till both dirt paths on the second right-click");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ultimineHoeInEarlierSlotStillFlattensFirstViaTrinket(GameTestHelper helper) {
        BlockPos firstDirtPos = new BlockPos(1, 2, 1);
        BlockPos secondDirtPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, 0, hoe);
        equipTrinket(player, 1, trinket);
        helper.setBlock(firstDirtPos, Blocks.DIRT);
        helper.setBlock(secondDirtPos, Blocks.DIRT);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteFirstDirtPos = helper.absolutePos(firstDirtPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteFirstDirtPos), Direction.UP, absoluteFirstDirtPos, false);
            if (!pressUltimineKey(player, absoluteFirstDirtPos)) {
                helper.succeed();
                return;
            }

            postRightClick(player, InteractionHand.MAIN_HAND, absoluteFirstDirtPos, hit);
            helper.assertTrue(helper.getBlockState(firstDirtPos).is(Blocks.DIRT_PATH),
                    "The trinket must flatten dirt first even when the hoe occupies an earlier free-hand slot");
            helper.assertBlockPresent(Blocks.DIRT_PATH, secondDirtPos);
            helper.assertTrue(trinket.getDamageValue() == 2 && hoe.getDamageValue() == 0,
                    "The chain must prefer the flattening trinket over the hoe regardless of slot order");
            helper.succeed();
        });
    }

    // ==================== 连锁右键：去皮与雕南瓜 ====================

    @GameTest(template = "empty")
    public static void ultimineChainsFreeHandAxeStripsLogs(GameTestHelper helper) {
        BlockPos firstLogPos = new BlockPos(1, 2, 1);
        BlockPos secondLogPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        equipTrinket(player, axe);
        helper.setBlock(firstLogPos, Blocks.OAK_LOG);
        helper.setBlock(secondLogPos, Blocks.OAK_LOG);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteFirstLogPos = helper.absolutePos(firstLogPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteFirstLogPos), Direction.UP, absoluteFirstLogPos, false);
            if (!pressUltimineKey(player, absoluteFirstLogPos)) {
                helper.succeed();
                return;
            }

            PlayerInteractEvent.RightClickBlock event = postRightClick(player, InteractionHand.MAIN_HAND, absoluteFirstLogPos, hit);
            helper.assertTrue(event.isCanceled(),
                    "Ultimine must consume a free-hand axe right-click after it chains the strip action");
            helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, firstLogPos);
            helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, secondLogPos);
            helper.assertTrue(axe.getDamageValue() == 2,
                    "Ultimine must consume one free-hand axe durability for every stripped log");
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "A free-hand tool must not move into the physical main hand");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ultimineChainsFreeHandShearsCarvePumpkins(GameTestHelper helper) {
        BlockPos firstPumpkinPos = new BlockPos(1, 2, 1);
        BlockPos secondPumpkinPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack shears = new ItemStack(Items.SHEARS);
        equipTrinket(player, shears);
        helper.setBlock(firstPumpkinPos, Blocks.PUMPKIN);
        helper.setBlock(secondPumpkinPos, Blocks.PUMPKIN);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteFirstPumpkinPos = helper.absolutePos(firstPumpkinPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteFirstPumpkinPos), Direction.UP, absoluteFirstPumpkinPos, false);
            if (!pressUltimineKey(player, absoluteFirstPumpkinPos)) {
                helper.succeed();
                return;
            }

            PlayerInteractEvent.RightClickBlock event = postRightClick(player, InteractionHand.MAIN_HAND, absoluteFirstPumpkinPos, hit);
            helper.assertTrue(event.isCanceled(),
                    "Ultimine must consume a free-hand shears right-click after the chain fallback carves pumpkins");
            helper.assertBlockPresent(Blocks.CARVED_PUMPKIN, firstPumpkinPos);
            helper.assertBlockPresent(Blocks.CARVED_PUMPKIN, secondPumpkinPos);
            helper.assertTrue(shears.getDamageValue() == 2,
                    "The chain fallback must carve every pumpkin via the shears' own useOn");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ultimineChainsIronTrinketStripsLogs(GameTestHelper helper) {
        BlockPos firstLogPos = new BlockPos(1, 2, 1);
        BlockPos secondLogPos = new BlockPos(2, 2, 1);
        ServerPlayer player = spawnServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(firstLogPos, Blocks.OAK_LOG);
        helper.setBlock(secondLogPos, Blocks.OAK_LOG);

        helper.runAfterDelay(90, () -> {
            BlockPos absoluteFirstLogPos = helper.absolutePos(firstLogPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteFirstLogPos), Direction.UP, absoluteFirstLogPos, false);
            if (!pressUltimineKey(player, absoluteFirstLogPos)) {
                helper.succeed();
                return;
            }

            PlayerInteractEvent.RightClickBlock event = postRightClick(player, InteractionHand.MAIN_HAND, absoluteFirstLogPos, hit);
            helper.assertTrue(event.isCanceled(),
                    "Ultimine must consume an Iron Trinket right-click after it chains the strip action");
            helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, firstLogPos);
            helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, secondLogPos);
            helper.assertTrue(trinket.getDamageValue() == 2,
                    "The trinket must strip both logs through its axe chain mode");
            helper.succeed();
        });
    }
}
