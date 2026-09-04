package com.yourname.freehands.gametest;

import com.yourname.freehands.FreeHands;
import com.yourname.freehands.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.yourname.freehands.gametest.GameTestSupport.closeTo;
import static com.yourname.freehands.gametest.GameTestSupport.enchant;
import static com.yourname.freehands.gametest.GameTestSupport.enchantHolder;
import static com.yourname.freehands.gametest.GameTestSupport.equipIronArmorSet;
import static com.yourname.freehands.gametest.GameTestSupport.equipTrinket;
import static com.yourname.freehands.gametest.GameTestSupport.hurtWithExplosion;
import static com.yourname.freehands.gametest.GameTestSupport.hurtWithPlayerOwnedTnt;
import static com.yourname.freehands.gametest.GameTestSupport.spawnPlayer;

/**
 * 护甲减伤与战斗规则测试（NeoForge 1.21.1）：解放槽饰品提供的护甲值、韧性与爆炸减伤须与对应整套原版盔甲一致；
 * 另覆盖间接伤害不叠武器攻击、以及 1.21.1 战利品结算的抢夺等级须计入解放槽武器。
 * 附魔改用 1.21.1 的 ResourceKey + ItemStack.enchant。
 */
@GameTestHolder(FreeHands.MODID)
@PrefixGameTestTemplate(false)
public final class TrinketArmorGameTests {
    private TrinketArmorGameTests() {
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
        enchant(helper.getLevel(), trinket, Enchantments.BLAST_PROTECTION, 4);
        equipTrinket(trinketPlayer, trinket);
        Player vanillaArmorPlayer = spawnPlayer(helper, 2);
        equipIronArmorSet(vanillaArmorPlayer);
        ItemStack chestplate = vanillaArmorPlayer.getItemBySlot(EquipmentSlot.CHEST);
        enchant(helper.getLevel(), chestplate, Enchantments.BLAST_PROTECTION, 4);

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

    /**
     * 抢夺兼容：1.21.1 战利品结算用 {@code EnchantmentHelper.getEnchantmentLevel(LOOTING, attacker)}
     * 取抢夺等级，而该方法只读原版装备槽，看不到解放槽武器，故由 EnchantmentHelperMixin 抬高原版结果。
     */
    @GameTest(template = "empty")
    public static void freeHandSwordLootingCountsForMobDrops(GameTestHelper helper) {
        Player player = spawnPlayer(helper, 1);
        Holder<Enchantment> looting = enchantHolder(helper.getLevel(), Enchantments.LOOTING);
        helper.assertTrue(EnchantmentHelper.getEnchantmentLevel(looting, player) == 0,
                "A player with nothing equipped must start without looting");

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        enchant(helper.getLevel(), sword, Enchantments.LOOTING, 3);
        equipTrinket(player, sword);
        helper.assertTrue(EnchantmentHelper.getEnchantmentLevel(looting, player) == 3,
                "A Looting III sword in the free-hand slot must raise the looting level used for mob loot");
        helper.assertTrue(EnchantmentHelper.getEnchantmentLevel(
                        enchantHolder(helper.getLevel(), Enchantments.SHARPNESS), player) == 0,
                "The looting lift must not leak into other enchantment lookups");
        helper.succeed();
    }
}
