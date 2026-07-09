package com.yourname.freehands.event;

import com.google.common.collect.Multimap;
import com.yourname.freehands.FreeHands;
import com.yourname.freehands.ability.FreeHandAbility;
import com.yourname.freehands.item.AbilityTrinketItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FreeHands.MODID)
public final class FreeHandEvents {
    private static final UUID FREE_HAND_ARMOR_UUID = UUID.fromString("e005fb78-f2f6-48f1-908f-8cb8b18d4996");
    private static final UUID FREE_HAND_TOUGHNESS_UUID = UUID.fromString("f076c0b5-c03e-44ad-9205-92f3316e7852");

    private FreeHandEvents() {
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (canAnyFreeHandHarvest(event.getEntity(), event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        float bestSpeed = bestMiningSpeed(event.getEntity(), event.getState());
        if (bestSpeed > event.getNewSpeed()) {
            event.setNewSpeed(bestSpeed);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }

        double freeHandDamage = bestFreeHandAttackDamage(player);
        double heldDamage = attackDamage(player.getMainHandItem());
        double bonus = Math.max(0.0D, freeHandDamage - heldDamage);
        if (bonus > 0.0D) {
            event.setAmount((float) (event.getAmount() + bonus));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        double armor = totalFreeHandArmor(event.player);
        double toughness = totalFreeHandArmorToughness(event.player);
        updateTransientModifier(event.player.getAttribute(Attributes.ARMOR), FREE_HAND_ARMOR_UUID, "Free hand armor", armor);
        updateTransientModifier(event.player.getAttribute(Attributes.ARMOR_TOUGHNESS), FREE_HAND_TOUGHNESS_UUID, "Free hand armor toughness", toughness);
    }

    private static boolean canAnyFreeHandHarvest(Player player, BlockState state) {
        return freeHandStacks(player).stream().anyMatch(stack -> canStackHarvest(stack, state));
    }

    private static float bestMiningSpeed(Player player, BlockState state) {
        float best = 0.0F;
        for (ItemStack stack : freeHandStacks(player)) {
            if (canStackHarvest(stack, state)) {
                best = Math.max(best, miningSpeed(stack, state));
            }
        }
        return best;
    }

    private static boolean canStackHarvest(ItemStack stack, BlockState state) {
        Optional<FreeHandAbility> ability = trinketAbility(stack);
        if (ability.isPresent()) {
            return canHarvestWithAbility(state, ability.get());
        }
        return stack.isCorrectToolForDrops(state);
    }

    private static float miningSpeed(ItemStack stack, BlockState state) {
        Optional<FreeHandAbility> ability = trinketAbility(stack);
        if (ability.isPresent()) {
            return ability.get().miningSpeed();
        }
        return stack.getDestroySpeed(state);
    }

    private static double bestFreeHandAttackDamage(Player player) {
        double best = 0.0D;
        for (ItemStack stack : freeHandStacks(player)) {
            best = Math.max(best, attackDamage(stack));
        }
        return best;
    }

    private static double attackDamage(ItemStack stack) {
        Optional<FreeHandAbility> ability = trinketAbility(stack);
        if (ability.isPresent()) {
            return ability.get().attackDamage();
        }

        double damage = 0.0D;
        Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            damage += modifier.getAmount();
        }
        return damage;
    }

    private static double totalFreeHandArmor(Player player) {
        double armor = 0.0D;
        for (ItemStack stack : freeHandStacks(player)) {
            Optional<FreeHandAbility> ability = trinketAbility(stack);
            if (ability.isPresent()) {
                armor += ability.get().armor();
            } else if (stack.getItem() instanceof ArmorItem armorItem) {
                armor += armorItem.getDefense();
            }
        }
        return armor;
    }

    private static double totalFreeHandArmorToughness(Player player) {
        double toughness = 0.0D;
        for (ItemStack stack : freeHandStacks(player)) {
            Optional<FreeHandAbility> ability = trinketAbility(stack);
            if (ability.isPresent()) {
                toughness += ability.get().armorToughness();
            } else if (stack.getItem() instanceof ArmorItem armorItem) {
                toughness += armorItem.getToughness();
            }
        }
        return toughness;
    }

    private static void updateTransientModifier(AttributeInstance attribute, UUID uuid, String name, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(uuid);
        if (existing != null) {
            attribute.removeModifier(existing);
        }
        if (amount > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static List<ItemStack> freeHandStacks(Player player) {
        return CuriosApi.getCuriosHelper()
                .findCurios(player, FreeHands.FREE_HAND_SLOT)
                .stream()
                .map(SlotResult::stack)
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    private static Optional<FreeHandAbility> trinketAbility(ItemStack stack) {
        if (stack.getItem() instanceof AbilityTrinketItem trinketItem) {
            return Optional.of(trinketItem.ability());
        }
        return Optional.empty();
    }

    private static boolean canHarvestWithAbility(BlockState state, FreeHandAbility ability) {
        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return ability.harvestLevel() >= 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return ability.harvestLevel() >= 2;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return ability.harvestLevel() >= 1;
        }
        return ability.harvestLevel() >= 0;
    }
}
