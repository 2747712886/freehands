package com.yourname.freehands.gametest;

import com.mojang.authlib.GameProfile;
import com.yourname.freehands.FreeHands;
import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.event.FreeHandEvents;
import com.yourname.freehands.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;
import java.util.UUID;

@GameTestHolder(FreeHands.MODID)
@PrefixGameTestTemplate(false)
public final class IronTrinketGameTests {
    private static final float EXPLOSION_DAMAGE = 20.0F;
    private static final float EPSILON = 0.001F;

    private IronTrinketGameTests() {
    }

    @GameTest(template = "empty")
    public static void ironTrinketProvidesFullIronArmor(GameTestHelper helper) {
        Player player = spawnPlayer(helper);
        equipTrinket(player, ModItems.IRON_TRINKET.get().getDefaultInstance());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(closeTo(player.getAttributeValue(Attributes.ARMOR), 15.0D),
                    "Iron Trinket should add the armor of a complete iron set");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void diamondTrinketProvidesFullDiamondArmor(GameTestHelper helper) {
        Player player = spawnPlayer(helper);
        equipTrinket(player, ModItems.DIAMOND_TRINKET.get().getDefaultInstance());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(closeTo(player.getAttributeValue(Attributes.ARMOR), 20.0D),
                    "Diamond Trinket should add the armor of a complete diamond set");
            helper.assertTrue(closeTo(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 8.0D),
                    "Diamond Trinket should add the toughness of a complete diamond set");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void netheriteTrinketProvidesFullNetheriteArmor(GameTestHelper helper) {
        Player player = spawnPlayer(helper);
        equipTrinket(player, ModItems.NETHERITE_TRINKET.get().getDefaultInstance());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(closeTo(player.getAttributeValue(Attributes.ARMOR), 20.0D),
                    "Netherite Trinket should add the armor of a complete netherite set");
            helper.assertTrue(closeTo(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 12.0D),
                    "Netherite Trinket should add the toughness of a complete netherite set");
            helper.assertTrue(closeTo(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.4D),
                    "Netherite Trinket should add the knockback resistance of a complete netherite set");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketReducesFixedExplosionDamage(GameTestHelper helper) {
        Player trinketPlayer = spawnPlayer(helper, 1);
        equipTrinket(trinketPlayer, ModItems.IRON_TRINKET.get().getDefaultInstance());
        Player vanillaArmorPlayer = spawnPlayer(helper, 2);
        equipIronArmorSet(vanillaArmorPlayer);

        helper.runAfterDelay(2, () -> {
            float trinketDamage = hurtWithExplosion(trinketPlayer);
            float vanillaArmorDamage = hurtWithExplosion(vanillaArmorPlayer);
            helper.assertTrue(closeTo(trinketDamage, vanillaArmorDamage),
                    "Iron Trinket should match full iron armor explosion damage; trinket "
                            + trinketDamage + ", armor set " + vanillaArmorDamage);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void blastProtectionFourFurtherReducesFixedExplosionDamage(GameTestHelper helper) {
        Player trinketPlayer = spawnPlayer(helper, 1);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        EnchantmentHelper.setEnchantments(Map.of(Enchantments.BLAST_PROTECTION, 4), trinket);
        equipTrinket(trinketPlayer, trinket);
        Player vanillaArmorPlayer = spawnPlayer(helper, 2);
        equipIronArmorSet(vanillaArmorPlayer);
        ItemStack chestplate = vanillaArmorPlayer.getItemBySlot(EquipmentSlot.CHEST);
        EnchantmentHelper.setEnchantments(Map.of(Enchantments.BLAST_PROTECTION, 4), chestplate);

        helper.runAfterDelay(2, () -> {
            float trinketDamage = hurtWithExplosion(trinketPlayer);
            float vanillaArmorDamage = hurtWithExplosion(vanillaArmorPlayer);
            helper.assertTrue(closeTo(trinketDamage, vanillaArmorDamage),
                    "Blast Protection IV on the Iron Trinket should match an enchanted full iron set; trinket "
                            + trinketDamage + ", armor set " + vanillaArmorDamage);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void playerOwnedTntDoesNotAddTrinketAttackDamage(GameTestHelper helper) {
        Player trinketPlayer = spawnPlayer(helper, 1);
        equipTrinket(trinketPlayer, ModItems.IRON_TRINKET.get().getDefaultInstance());
        Player vanillaArmorPlayer = spawnPlayer(helper, 2);
        equipIronArmorSet(vanillaArmorPlayer);

        helper.runAfterDelay(2, () -> {
            float trinketDamage = hurtWithPlayerOwnedTnt(trinketPlayer);
            float vanillaArmorDamage = hurtWithPlayerOwnedTnt(vanillaArmorPlayer);
            helper.assertTrue(closeTo(trinketDamage, vanillaArmorDamage),
                    "A player-owned TNT explosion must not add Iron Trinket attack damage; trinket "
                            + trinketDamage + ", armor set " + vanillaArmorDamage);
            helper.succeed();
        });
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
    public static void ironTrinketCanFlattenGrassBlockWithRightClick(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(pathPos, Blocks.DIRT_PATH);

        helper.runAfterDelay(2, () -> {
            useEmptyMainHandOn(player, helper, pathPos);
            helper.assertBlockPresent(Blocks.FARMLAND, pathPos);
            helper.assertTrue(trinket.getDamageValue() == 1,
                    "Tilling a dirt path with an Iron Trinket should consume one durability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ironTrinketCanCarvePumpkinWithRightClick(GameTestHelper helper) {
        BlockPos pumpkinPos = new BlockPos(1, 2, 1);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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

    @GameTest(template = "empty")
    public static void freeHandShovelCanFlattenGrassBlockWithRightClick(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
    public static void freeHandHoeCanTillDirtWithRightClick(GameTestHelper helper) {
        BlockPos dirtPos = new BlockPos(1, 2, 1);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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

    @GameTest(template = "empty")
    public static void freeHandRightClickFallsThroughToLaterTool(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(1, 2, 1);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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

    @GameTest(template = "empty")
    public static void freeHandToolRunsOnlyOnceAcrossHandPackets(GameTestHelper helper) {
        BlockPos dirtPos = new BlockPos(1, 2, 1);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"));
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        shovel.getOrCreateTag().putBoolean("Unbreakable", true);
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

    private static Player spawnPlayer(GameTestHelper helper) {
        return spawnPlayer(helper, 1);
    }

    private static Player spawnPlayer(GameTestHelper helper, int xOffset) {
        Player player = helper.makeMockPlayer();
        player.setPos(helper.absolutePos(new BlockPos(xOffset, 2, 1)).getCenter());
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static void equipTrinket(Player player, ItemStack stack) {
        equipTrinket(player, 0, stack);
    }

    private static void equipTrinket(Player player, int slot, ItemStack stack) {
        CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(handler -> handler.getStacksHandler(FreeHands.FREE_HAND_SLOT))
                .orElseThrow(() -> new IllegalStateException("Free Hand Curios inventory is unavailable"))
                .getStacks()
                .setStackInSlot(slot, stack);
    }

    private static void useEmptyMainHandOn(ServerPlayer player, GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
        player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitResult);
        player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.OFF_HAND, hitResult);
    }

    private static void equipIronArmorSet(Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }

    private static float hurtWithExplosion(Player player) {
        float healthBefore = player.getHealth();
        player.hurt(player.damageSources().explosion(null, null), EXPLOSION_DAMAGE);
        return healthBefore - player.getHealth();
    }

    private static float hurtWithPlayerOwnedTnt(Player player) {
        PrimedTnt tnt = new PrimedTnt(player.level(), player.getX(), player.getY(), player.getZ(), player);
        float healthBefore = player.getHealth();
        player.hurt(player.damageSources().explosion(tnt, player), EXPLOSION_DAMAGE);
        return healthBefore - player.getHealth();
    }


    // ── Plant / foliage drop correctness ──

    @GameTest(template = "empty")
    public static void ironTrinketOnGrassDropsSeedsNotGrassBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        Player player = spawnPlayer(helper);
        ItemStack trinket = ModItems.IRON_TRINKET.get().getDefaultInstance();
        equipTrinket(player, trinket);
        helper.setBlock(pos, Blocks.GRASS);

        helper.runAfterDelay(2, () -> {
            BlockPos absolute = helper.absolutePos(pos);
            VirtualMainHandContext.beginMining(player, Blocks.GRASS.defaultBlockState());
            Blocks.GRASS.playerDestroy(helper.getLevel(), player, absolute,
                    Blocks.GRASS.defaultBlockState(), null, player.getMainHandItem());
            VirtualMainHandContext.endMining(player);
            helper.assertItemEntityNotPresent(Items.GRASS, pos, 2.0D);
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
        helper.setBlock(pos, Blocks.GRASS);

        helper.runAfterDelay(2, () -> {
            BlockPos absolute = helper.absolutePos(pos);
            VirtualMainHandContext.beginMining(player, Blocks.GRASS.defaultBlockState());
            Blocks.GRASS.playerDestroy(helper.getLevel(), player, absolute,
                    Blocks.GRASS.defaultBlockState(), null, player.getMainHandItem());
            VirtualMainHandContext.endMining(player);
            helper.assertItemEntityPresent(Items.GRASS, pos, 2.0D);
            helper.assertTrue(shears.getDamageValue() == 0,
                    "Shears should not lose durability when breaking grass");
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

            EnchantmentHelper.setEnchantments(Map.of(Enchantments.BLOCK_EFFICIENCY, 1), trinket);
            int level = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, trinket);
            helper.assertTrue(level == 1,
                    "An Iron Trinket should accept Efficiency I; actual level: " + level);

            EnchantmentHelper.setEnchantments(Map.of(Enchantments.ALL_DAMAGE_PROTECTION, 1), trinket);
            int protLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, trinket);
            helper.assertTrue(protLevel == 1,
                    "An Iron Trinket should accept Protection I; actual level: " + protLevel);

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
    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) <= EPSILON;
    }
}
