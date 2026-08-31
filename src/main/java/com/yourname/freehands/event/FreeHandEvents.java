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
import net.minecraft.core.BlockPos;
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
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
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
    // 一次物理右键客户端会先后发送主手、副手两个 use 包（两手皆空时本地都判定 PASS）。
    // 记录解放槽动作已生效的位置，副手空包到达时跳过，避免同一次右键执行两次动作。
    private static final Map<UUID, BlockPos> FREE_HAND_HANDLED_POS = new HashMap<>();

    private FreeHandEvents() {
    }

    public static void recordFreeHandHandled(Player player, BlockPos pos) {
        FREE_HAND_HANDLED_POS.put(player.getUUID(), pos.immutable());
    }

    public static boolean isFreeHandHandled(Player player, BlockPos pos) {
        BlockPos recorded = FREE_HAND_HANDLED_POS.get(player.getUUID());
        return recorded != null && recorded.equals(pos);
    }

    public static boolean consumeFreeHandHandled(Player player, BlockPos pos) {
        BlockPos recorded = FREE_HAND_HANDLED_POS.get(player.getUUID());
        if (recorded != null && recorded.equals(pos)) {
            FREE_HAND_HANDLED_POS.remove(player.getUUID());
            return true;
        }
        return false;
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
            int looting = attackerStack.stack().getEnchantmentLevel(Enchantments.MOB_LOOTING);
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
        // 与链式右键(ultimineUseStacks)一致：铲平(锹/饰品)优先于锄地，
        // 避免单块回退因槽位顺序让锄头直接把泥土锄成耕地、跳过土径中间态。
        return freeHandStacks(player).stream()
                .filter(FreeHandEvents::canUseOnBlock)
                .sorted(Comparator.comparingInt(FreeHandEvents::rightClickPriority))
                .toList();
    }

    public static List<ItemStack> ultimineUseStacks(Player player) {
        List<ItemStack> result = freeHandUseStacks(player).stream()
                .sorted(Comparator.comparingInt(FreeHandEvents::rightClickPriority))
                .collect(java.util.stream.Collectors.toList());
        CuriosApi.getCuriosInventory(player).resolve().ifPresent(handler -> handler.getCurios().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(FreeHands.FREE_HAND_SLOT))
                .sorted(Comparator.<Map.Entry<String, ICurioStacksHandler>>comparingInt(entry ->
                        CuriosApi.getSlot(entry.getKey(), player.level())
                                .map(ISlotType::getOrder)
                                .orElse(Integer.MAX_VALUE))
                        .thenComparing(Map.Entry::getKey))
                .forEach(entry -> {
                    IDynamicStackHandler stacks = entry.getValue().getStacks();
                    for (int slot = 0; slot < stacks.getSlots(); slot++) {
                        ItemStack stack = stacks.getStackInSlot(slot);
                        if (!stack.isEmpty()
                                && canUseOnBlock(stack)
                                && CuriosApi.getItemStackSlots(stack, player).containsKey(entry.getKey())) {
                            result.add(stack);
                        }
                    }
                }));
        return result;
    }


    private static int rightClickPriority(ItemStack stack) {
        if (stack.canPerformAction(net.minecraftforge.common.ToolActions.SHOVEL_FLATTEN)) {
            return 0;
        }
        if (stack.canPerformAction(net.minecraftforge.common.ToolActions.AXE_STRIP)) {
            return 1;
        }
        if (stack.canPerformAction(net.minecraftforge.common.ToolActions.HOE_TILL)) {
            return 2;
        }
        return 3;
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
            int efficiency = stack.getEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY);
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
        int fireAspect = stack.getEnchantmentLevel(Enchantments.FIRE_ASPECT);
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
