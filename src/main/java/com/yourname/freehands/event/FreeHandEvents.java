package com.yourname.freehands.event;

import com.google.common.collect.Multimap;
import com.yourname.freehands.FreeHands;
import com.yourname.freehands.ability.FreeHandAbility;
import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.item.AbilityTrinketItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FreeHands.MODID)
public final class FreeHandEvents {
    private static final UUID FREE_HAND_ARMOR_UUID = UUID.fromString("e005fb78-f2f6-48f1-908f-8cb8b18d4996");
    private static final UUID FREE_HAND_TOUGHNESS_UUID = UUID.fromString("f076c0b5-c03e-44ad-9205-92f3316e7852");
    private static final Map<CombatHit, Float> CRITICAL_HIT_MULTIPLIERS = new HashMap<>();

    private FreeHandEvents() {
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        boolean virtualMining = VirtualMainHandContext.getVirtualMainHand(event.getEntity()).isPresent();
        boolean canHarvest = virtualMining
                ? bestMiningStack(event.getEntity(), event.getTargetBlock()).isPresent()
                : selectedMiningStack(event.getEntity(), event.getTargetBlock()).isPresent();
        if (canHarvest) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        selectedMiningStack(event.getEntity(), event.getState()).ifPresent(stack -> {
            float bestSpeed = miningSpeed(stack, event.getState());
            if (bestSpeed > event.getNewSpeed()) {
                event.setNewSpeed(bestSpeed);
            }
        });
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        CombatHit hit = new CombatHit(event.getEntity().getUUID(), target.getUUID());
        if (event.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW
                || (event.isVanillaCritical() && event.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY)) {
            CRITICAL_HIT_MULTIPLIERS.put(hit, event.getDamageModifier());
        } else {
            CRITICAL_HIT_MULTIPLIERS.remove(hit);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player)) {
            return;
        }

        Optional<FreeHandStack> attackerStack = bestAttackStack(player, event.getEntity());
        if (attackerStack.isPresent()) {
            float criticalMultiplier = CRITICAL_HIT_MULTIPLIERS.getOrDefault(new CombatHit(player.getUUID(), event.getEntity().getUUID()), 1.0F);
            CRITICAL_HIT_MULTIPLIERS.remove(new CombatHit(player.getUUID(), event.getEntity().getUUID()));
            event.setAmount((float) (event.getAmount() + attackDamage(attackerStack.get().stack(), event.getEntity()) * criticalMultiplier));
            damageFreeHandItem(attackerStack.get().stack(), player, 1);
            applyWeaponSideEffects(attackerStack.get().stack(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getAmount() <= 0.0F) {
            return;
        }

        List<ItemStack> armorStacks = freeHandStacks(player).stream()
                .filter(FreeHandEvents::providesArmor)
                .toList();
        if (armorStacks.isEmpty()) {
            return;
        }

        if (!event.getSource().is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            int protection = EnchantmentHelper.getDamageProtection(armorStacks, event.getSource());
            if (protection > 0) {
                event.setAmount(CombatRules.getDamageAfterMagicAbsorb(event.getAmount(), protection));
            }
        }

        if (!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            int durabilityDamage = Math.max(1, (int) (event.getAmount() / 4.0F));
            for (ItemStack armorStack : armorStacks) {
                damageFreeHandItem(armorStack, player, durabilityDamage);
            }
        }

    }

    @SubscribeEvent
    public static void onLootingLevel(LootingLevelEvent event) {
        if (event.getDamageSource() == null) {
            return;
        }

        Entity attacker = event.getDamageSource().getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }

        bestAttackStack(player, event.getEntity()).ifPresent(attackerStack -> {
            int looting = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, attackerStack.stack());
            if (looting > event.getLootingLevel()) {
                event.setLootingLevel(looting);
            }
        });
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
        CRITICAL_HIT_MULTIPLIERS.keySet().removeIf(hit -> hit.playerId().equals(event.player.getUUID()));
    }

    public static Optional<ItemStack> selectedMiningStack(Player player, BlockState state) {
        Optional<FreeHandStack> bestStack = bestMiningStack(player, state);
        if (bestStack.isEmpty() || !shouldUseFreeHandForMining(player.getMainHandItem(), bestStack.get().stack(), state)) {
            return Optional.empty();
        }
        return Optional.of(bestStack.get().stack());
    }

    public static Optional<ItemStack> selectedUseStack(Player player) {
        return freeHandUseStacks(player).stream()
                .findFirst();
    }

    public static List<ItemStack> freeHandUseStacks(Player player) {
        if (!player.getMainHandItem().isEmpty()) {
            return List.of();
        }
        return freeHandStacks(player).stream()
                .filter(FreeHandEvents::canUseOnBlock)
                .toList();
    }

    private static Optional<FreeHandStack> bestMiningStack(Player player, BlockState state) {
        FreeHandStack bestStack = null;
        float bestSpeed = 0.0F;
        for (ItemStack stack : freeHandStacks(player)) {
            if (canStackHarvest(stack, state)) {
                float speed = miningSpeed(stack, state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestStack = new FreeHandStack(stack);
                }
            }
        }
        return Optional.ofNullable(bestStack);
    }

    private static boolean canStackHarvest(ItemStack stack, BlockState state) {
        Optional<FreeHandAbility> ability = trinketAbility(stack);
        if (ability.isPresent()) {
            return canHarvestWithAbility(state, ability.get());
        }
        if (stack.getItem() instanceof ShearsItem) {
            return true;
        }
        return stack.isCorrectToolForDrops(state);
    }

    private static float miningSpeed(ItemStack stack, BlockState state) {
        Optional<FreeHandAbility> ability = trinketAbility(stack);
        if (ability.isPresent()) {
            return ability.get().miningSpeed();
        }

        float speed = stack.getDestroySpeed(state);
        if (speed > 1.0F) {
            int efficiency = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, stack);
            if (efficiency > 0) {
                speed += efficiency * efficiency + 1;
            }
        }
        return speed;
    }

    private static Optional<FreeHandStack> bestAttackStack(Player player, LivingEntity target) {
        FreeHandStack bestStack = null;
        double bestDamage = 0.0D;
        for (ItemStack stack : freeHandStacks(player)) {
            double damage = attackDamage(stack, target);
            if (damage > bestDamage) {
                bestDamage = damage;
                bestStack = new FreeHandStack(stack);
            }
        }
        return Optional.ofNullable(bestStack);
    }

    private static double attackDamage(ItemStack stack, LivingEntity target) {
        Optional<FreeHandAbility> ability = trinketAbility(stack);
        if (ability.isPresent()) {
            return ability.get().attackDamage();
        }

        double damage = 0.0D;
        Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            damage += modifier.getAmount();
        }
        return damage + EnchantmentHelper.getDamageBonus(stack, target.getMobType());
    }

    private static double totalFreeHandArmor(Player player) {
        double armor = 0.0D;
        for (ItemStack stack : freeHandStacks(player)) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                armor += armorItem.getDefense();
            }
        }
        return armor;
    }

    private static double totalFreeHandArmorToughness(Player player) {
        double toughness = 0.0D;
        for (ItemStack stack : freeHandStacks(player)) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
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
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(handler -> handler.getStacksHandler(FreeHands.FREE_HAND_SLOT))
                .map(handler -> {
                    IDynamicStackHandler stacks = handler.getStacks();
                    List<ItemStack> result = new ArrayList<>();
                    for (int slot = 0; slot < stacks.getSlots(); slot++) {
                        ItemStack stack = stacks.getStackInSlot(slot);
                        if (!stack.isEmpty()) {
                            result.add(stack);
                        }
                    }
                    return result;
                })
                .orElseGet(List::of);
    }

    private static Optional<FreeHandAbility> trinketAbility(ItemStack stack) {
        if (stack.getItem() instanceof AbilityTrinketItem trinketItem) {
            return Optional.of(trinketItem.ability());
        }
        return Optional.empty();
    }

    private static boolean canUseOnBlock(ItemStack stack) {
        return stack.getItem() instanceof AbilityTrinketItem
                || stack.getItem() instanceof DiggerItem
                || stack.getItem() instanceof ShearsItem;
    }

    private static boolean providesArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || trinketAbility(stack).map(ability -> ability.armor() > 0.0D || ability.armorToughness() > 0.0D).orElse(false);
    }

    private static boolean canHarvestWithAbility(BlockState state, FreeHandAbility ability) {
        if (!state.requiresCorrectToolForDrops()) {
            if (state.is(BlockTags.REPLACEABLE)) {
                return false;
            }
            return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                        || state.is(BlockTags.MINEABLE_WITH_AXE)
                        || state.is(BlockTags.MINEABLE_WITH_HOE);
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

    private static boolean shouldUseFreeHandForMining(ItemStack heldStack, ItemStack freeHandStack, BlockState state) {
        if (!canStackHarvest(freeHandStack, state)) {
            return false;
        }
        return !hasMiningSpeedBonus(heldStack, state);
    }

    private static boolean hasMiningSpeedBonus(ItemStack stack, BlockState state) {
        return miningSpeed(stack, state) > 1.0F;
    }

    public static void damageFreeHandItem(ItemStack stack, Player player, int amount) {
        if (amount <= 0 || !stack.isDamageableItem()) {
            return;
        }
        stack.hurtAndBreak(amount, player, owner -> owner.broadcastBreakEvent(EquipmentSlot.MAINHAND));
    }

    private static void applyWeaponSideEffects(ItemStack stack, LivingEntity target) {
        int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }
    }

    private record FreeHandStack(ItemStack stack) {
    }

    private record CombatHit(
            UUID playerId,
            UUID targetId
    ) {
    }
}
