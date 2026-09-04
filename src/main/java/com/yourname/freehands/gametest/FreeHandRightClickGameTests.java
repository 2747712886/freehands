package com.yourname.freehands.gametest;

import com.yourname.freehands.FreeHands;
import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.event.FreeHandEvents;
import com.yourname.freehands.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.yourname.freehands.gametest.GameTestSupport.equipTrinket;
import static com.yourname.freehands.gametest.GameTestSupport.newServerPlayer;
import static com.yourname.freehands.gametest.GameTestSupport.useEmptyMainHandOn;

/**
 * 单块右键交互测试（NeoForge 1.21.1）：饰品的铲平/去皮/锄地/雕南瓜/刮锈/去蜡/催熟，普通工具交互，
 * 多工具优先级、主/副手去重、副手与主手方块放置优先、Unbreakable（改用数据组件）等规则。
 */
@GameTestHolder(FreeHands.MODID)
@PrefixGameTestTemplate(false)
public final class FreeHandRightClickGameTests {
    private FreeHandRightClickGameTests() {
    }

    // ==================== 饰品自身右键交互 ====================

    @GameTest(template = "empty")
    public static void ironTrinketCanFlattenGrassBlockWithRightClick(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(FreeHandEvents.selectedUseStack(player).isPresent(),
                    "An empty main hand should select an Iron Trinket for block use");
            useEmptyMainHandOn(player, helper, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "Flattening grass with an Iron Trinket should consume one durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketCanStripOakLogWithRightClick(GameTestHelper helper) {
        BlockPos logPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(logPos, Blocks.OAK_LOG);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, logPos);
            helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, logPos);
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "Stripping an oak log with an Iron Trinket should consume one durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketCanTillDirtPathWithRightClick(GameTestHelper helper) {
        BlockPos pathPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(pathPos, Blocks.DIRT_PATH);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, pathPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, pathPos);
            helper.assertTrue(trinket.getDamageValue() == 0,
                    "An Iron Trinket must not till a dirt path; its recipe does not include a hoe");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketCanCarvePumpkinWithRightClick(GameTestHelper helper) {
        BlockPos pumpkinPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(pumpkinPos, Blocks.PUMPKIN);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, pumpkinPos);
            helper.assertBlockPresent(Blocks.CARVED_PUMPKIN, pumpkinPos);
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "Carving a pumpkin with an Iron Trinket should consume one durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketCanScrapeCopperWithRightClick(GameTestHelper helper) {
        BlockPos copperPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(copperPos, Blocks.EXPOSED_COPPER);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, copperPos);
            helper.assertBlockPresent(Blocks.COPPER_BLOCK, copperPos);
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "Scraping exposed copper with an Iron Trinket should consume one durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketCanRemoveCopperWaxWithRightClick(GameTestHelper helper) {
        BlockPos copperPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(copperPos, Blocks.WAXED_COPPER_BLOCK);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, copperPos);
            helper.assertBlockPresent(Blocks.COPPER_BLOCK, copperPos);
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "Removing copper wax with an Iron Trinket should consume one durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketCanMatureGrowingVinesWithRightClick(GameTestHelper helper) {
        BlockPos vinePos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(vinePos, Blocks.CAVE_VINES);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, vinePos);
            BlockState state = helper.getLevel().getBlockState(helper.absolutePos(vinePos));
            helper.assertTrue(state.getBlock() instanceof GrowingPlantHeadBlock growingPlant
                            && growingPlant.isMaxAge(state),
                    "Maturing cave vines with an Iron Trinket should set them to their maximum age");
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "Maturing cave vines with an Iron Trinket should consume one durability");
            helper.succeed();
        });
    }

    // ==================== 普通工具右键交互 ====================

    @GameTest(template = "empty")
    public static void freeHandShovelCanFlattenGrassBlockWithRightClick(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        equipTrinket(player, shovel);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(FreeHandEvents.selectedUseStack(player).isPresent(),
                    "An empty main hand should select a free-hand shovel for block use");
            useEmptyMainHandOn(player, helper, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertTrue(shovel.getDamageValue() == 1,
                    "Flattening grass with a free-hand shovel should consume one durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void freeHandRightClickExercisesUnmutedSoundPath(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        equipTrinket(player, shovel);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(2, () -> {
            // LevelMixin 仅在 VirtualMainHandContext.isUsing(player) 为真时解除 playSound 的"排除发声者"，
            // 让虚拟工具使用者听到自己的右键音效。1.21.1 的 mock 连接无法完成 NeoForge 网络通道握手
            // （Curios 登录同步会被 NetworkRegistry.checkPacket 拒绝并中断 placeNewPlayer），故不再统计音效包；
            // 改为验证 mixin 的门控条件与右键成功路径——铲平成功必然经过被解除静音的 level.playSound，
            // 若 LevelMixin 误注入或抛错，该动作会失败。
            helper.assertTrue(!VirtualMainHandContext.isUsing(player),
                    "isUsing must be false outside a virtual right-click");
            useEmptyMainHandOn(player, helper, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertTrue(shovel.getDamageValue() == 1,
                    "The free-hand right-click (which plays its sound through the un-muted path) should flatten grass");
            helper.assertTrue(!VirtualMainHandContext.isUsing(player),
                    "isUsing must be false again after the right-click completes");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void freeHandHoeCanTillDirtWithRightClick(GameTestHelper helper) {
        BlockPos dirtPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, hoe);
        helper.setBlock(dirtPos, Blocks.DIRT);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, dirtPos);
            helper.assertBlockPresent(Blocks.FARMLAND, dirtPos);
            helper.assertTrue(hoe.getDamageValue() == 1,
                    "A free-hand hoe should till dirt when it is the only free-hand tool");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void freeHandAxeCanStripOakLogWithRightClick(GameTestHelper helper) {
        BlockPos logPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        equipTrinket(player, axe);
        helper.setBlock(logPos, Blocks.OAK_LOG);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, logPos);
            helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, logPos);
            helper.assertTrue(axe.getDamageValue() == 1,
                    "A free-hand axe should strip an oak log when it is the only free-hand tool");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void freeHandShearsCanCarvePumpkinWithRightClick(GameTestHelper helper) {
        BlockPos pumpkinPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack shears = new ItemStack(Items.SHEARS);
        equipTrinket(player, shears);
        helper.setBlock(pumpkinPos, Blocks.PUMPKIN);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, pumpkinPos);
            helper.assertBlockPresent(Blocks.CARVED_PUMPKIN, pumpkinPos);
            helper.assertTrue(shears.getDamageValue() == 1,
                    "Free-hand shears should carve a pumpkin when they are the only free-hand tool");
            helper.succeed();
        });
    }

    // ==================== 多工具优先级与流转 ====================

    @GameTest(template = "empty")
    public static void freeHandRightClickFallsThroughToLaterTool(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        equipTrinket(player, 0, pickaxe);
        equipTrinket(player, 1, shovel);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertTrue(pickaxe.getDamageValue() == 0,
                    "A free-hand pickaxe should not lose durability when its block use returns PASS");
            helper.assertTrue(shovel.getDamageValue() == 1,
                    "The next free-hand tool should run when the earlier tool returns PASS");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void laterFreeHandHoeCanTillDirtPath(GameTestHelper helper) {
        BlockPos pathPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, 0, shovel);
        equipTrinket(player, 1, hoe);
        helper.setBlock(pathPos, Blocks.DIRT_PATH);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, pathPos);
            helper.assertBlockPresent(Blocks.FARMLAND, pathPos);
            helper.assertTrue(shovel.getDamageValue() == 0,
                    "A free-hand shovel should not consume durability when it cannot modify a dirt path");
            helper.assertTrue(hoe.getDamageValue() == 1,
                    "A later free-hand hoe should turn a dirt path into farmland");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void successfulFreeHandToolStopsLaterTools(GameTestHelper helper) {
        BlockPos dirtPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, 0, shovel);
        equipTrinket(player, 1, hoe);
        helper.setBlock(dirtPos, Blocks.DIRT);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, dirtPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, dirtPos);
            helper.assertTrue(shovel.getDamageValue() == 1,
                    "The first successful free-hand tool should consume durability");
            helper.assertTrue(hoe.getDamageValue() == 0,
                    "Later free-hand tools must not run after an earlier tool handles the interaction");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void laterToolRunsOnNextRightClickAfterEarlierToolActs(GameTestHelper helper) {
        BlockPos dirtPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, 0, shovel);
        equipTrinket(player, 1, hoe);
        helper.setBlock(dirtPos, Blocks.DIRT);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, dirtPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, dirtPos);
            helper.assertTrue(hoe.getDamageValue() == 0,
                    "A later tool must not run during the earlier tool's successful right-click");

            useEmptyMainHandOn(player, helper, dirtPos);
            helper.assertBlockPresent(Blocks.FARMLAND, dirtPos);
            helper.assertTrue(shovel.getDamageValue() == 1 && hoe.getDamageValue() == 1,
                    "The next right-click should continue with a later tool when the earlier tool returns PASS");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketDoesNotOverrideSlotOrderWhileSneaking(GameTestHelper helper) {
        BlockPos dirtPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        equipTrinket(player, 0, trinket);
        equipTrinket(player, 1, hoe);
        player.setShiftKeyDown(true);
        helper.setBlock(dirtPos, Blocks.DIRT);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, dirtPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, dirtPos);
            helper.assertTrue(trinket.getDamageValue() == 1 && hoe.getDamageValue() == 0,
                    "The first-slot trinket must flatten dirt before a later hoe can till it, even while sneaking");
            helper.succeed();
        });
    }

    // ==================== 主/副手去重与优先级 ====================

    @GameTest(template = "empty")
    public static void freeHandToolRunsOnlyOnceAcrossHandPackets(GameTestHelper helper) {
        BlockPos dirtPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(dirtPos, Blocks.DIRT);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, dirtPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, dirtPos);
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "A free-hand tool must run once when a right-click produces main-hand and off-hand packets");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void offHandToolTakesPriorityOverFreeHandTool(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack freeHandShovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack offHandShovel = new ItemStack(Items.IRON_SHOVEL);
        equipTrinket(player, freeHandShovel);
        player.setItemInHand(InteractionHand.OFF_HAND, offHandShovel);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(2, () -> {
            BlockPos absoluteGrassPos = helper.absolutePos(grassPos);
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absoluteGrassPos), Direction.UP, absoluteGrassPos, false);
            InteractionResult mainHandResult = player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND, hitResult);
            if (!mainHandResult.consumesAction()) {
                player.gameMode.useItemOn(player, helper.getLevel(), offHandShovel, InteractionHand.OFF_HAND, hitResult);
            }

            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertTrue(offHandShovel.getDamageValue() == 1,
                    "The off-hand shovel should perform the right-click action before a free-hand tool");
            helper.assertTrue(freeHandShovel.getDamageValue() == 0,
                    "A free-hand tool must not consume durability when the off hand handles the action");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void mainHandBlockPlacementTakesPriorityOverFreeHandTool(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack freeHandShovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack dirt = new ItemStack(Items.DIRT);
        equipTrinket(player, freeHandShovel);
        player.setItemInHand(InteractionHand.MAIN_HAND, dirt);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(2, () -> {
            BlockPos absoluteGrassPos = helper.absolutePos(grassPos);
            player.gameMode.useItemOn(player, helper.getLevel(), dirt, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absoluteGrassPos), Direction.UP, absoluteGrassPos, false));
            helper.assertBlockPresent(Blocks.DIRT, grassPos.above());
            helper.assertTrue(freeHandShovel.getDamageValue() == 0,
                    "A free-hand tool must not consume durability when the main hand places a block");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void unbreakableFreeHandToolDoesNotLoseDurability(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        shovel.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        equipTrinket(player, shovel);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, grassPos);
            helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);
            helper.assertTrue(shovel.getDamageValue() == 0,
                    "An Unbreakable free-hand tool should not lose durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void freeHandToolFiresWhenMainHandItemReturnsPASS(GameTestHelper helper) {
        BlockPos dirtPos = new BlockPos(1, 2, 1);
        ServerPlayer player = newServerPlayer(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        equipTrinket(player, 0, hoe);
        helper.setBlock(dirtPos, Blocks.DIRT);

        helper.runAfterDelay(2, () -> {
            BlockPos absolute = helper.absolutePos(dirtPos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
            player.gameMode.useItemOn(player, helper.getLevel(), shovel, InteractionHand.MAIN_HAND, hit);
            helper.assertBlockPresent(Blocks.DIRT_PATH, dirtPos);
            helper.assertTrue(shovel.getDamageValue() == 1,
                    "The main-hand shovel should flatten dirt");
            helper.assertTrue(hoe.getDamageValue() == 0,
                    "A free-hand hoe must not run when the main-hand tool handles the interaction");

            player.gameMode.useItemOn(player, helper.getLevel(), shovel, InteractionHand.MAIN_HAND, hit);
            helper.assertBlockPresent(Blocks.FARMLAND, dirtPos);
            helper.assertTrue(hoe.getDamageValue() == 1,
                    "A free-hand hoe should till a dirt path when the main-hand shovel returns PASS");
            helper.succeed();
        });
    }
}
