package com.yourname.freehands.event;

import com.yourname.freehands.FreeHands;
import com.yourname.freehands.ability.FreeHandAbility;
import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.item.AbilityTrinketItem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.apache.commons.lang3.mutable.MutableFloat;
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

/**
 * 解放槽事件处理器：管理挖掘速度、战斗伤害、护甲值、掉落物等所有解放槽相关逻辑（NeoForge 1.21.1）。
 */
@EventBusSubscriber(modid = FreeHands.MODID)
public final class FreeHandEvents {
    // ==================== 常量与状态 ====================

    private static final ResourceLocation FREE_HAND_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(FreeHands.MODID, "free_hand_armor");
    private static final ResourceLocation FREE_HAND_TOUGHNESS_ID =
            ResourceLocation.fromNamespaceAndPath(FreeHands.MODID, "free_hand_armor_toughness");

    /** 暴击倍率缓存：用于在受伤事件中应用正确的暴击伤害。 */
    private static final Map<CombatHit, Float> CRITICAL_HIT_MULTIPLIERS = new HashMap<>();

    /**
     * 右键去重状态：一次物理右键客户端会先后发送主手、副手两个 use 包（两手皆空时本地都判定 PASS）。
     * 记录解放槽动作已生效的位置，副手空包到达时跳过，避免同一次右键执行两次动作。
     */
    private static final Map<UUID, BlockPos> FREE_HAND_HANDLED_POS = new HashMap<>();

    private FreeHandEvents() {
    }

    // ==================== 公共 API：右键去重 ====================

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

    // ==================== 事件订阅：挖掘相关 ====================

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

    // ==================== 事件订阅：战斗相关 ====================

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        CombatHit hit = new CombatHit(event.getEntity().getUUID(), target.getUUID());
        if (event.isCriticalHit()) {
            CRITICAL_HIT_MULTIPLIERS.put(hit, event.getDamageMultiplier());
        } else {
            CRITICAL_HIT_MULTIPLIERS.remove(hit);
        }
    }

    /**
     * 1.20.1 的 LivingHurtEvent 在 1.21.1 改名为 LivingIncomingDamageEvent（减伤前），语义一致：
     * 把解放槽最高武器的攻击伤害（含暴击倍率与附魔加成）叠加到本次伤害，并扣该武器耐久、触发火焰附加。
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player)) {
            return;
        }

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        Optional<FreeHandStack> attackerStack = bestAttackStack(player, target, source);
        if (attackerStack.isEmpty()) {
            return;
        }

        ItemStack stack = attackerStack.get().stack();
        CombatHit hit = new CombatHit(player.getUUID(), target.getUUID());
        float criticalMultiplier = CRITICAL_HIT_MULTIPLIERS.getOrDefault(hit, 1.0F);
        CRITICAL_HIT_MULTIPLIERS.remove(hit);
        float added = (float) (attackDamage(stack, target, player, source) * criticalMultiplier);
        event.setAmount(event.getAmount() + added);
        damageFreeHandItem(stack, player, 1);
        applyWeaponSideEffects(stack, target);
    }

    /**
     * 护甲减伤与耐久：1.21.1 的 LivingDamageEvent.Pre（伤害最终结算前）。
     * 基础护甲值已由 transient attribute modifier 走原版减伤管线；此处补充解放槽护甲的"保护类附魔"减伤
     * （原版只读真实装备，看不到解放槽护甲），并按减伤后数值扣护甲耐久。
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || event.getNewDamage() <= 0.0F
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        List<ItemStack> armorStacks = freeHandStacks(player).stream()
                .filter(FreeHandEvents::providesArmor)
                .toList();
        if (armorStacks.isEmpty()) {
            return;
        }

        DamageSource source = event.getSource();
        if (!source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            float protection = freeHandProtection(serverLevel, player, armorStacks, source);
            if (protection > 0.0F) {
                event.setNewDamage(CombatRules.getDamageAfterMagicAbsorb(event.getNewDamage(), protection));
            }
        }

        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            int durabilityDamage = Math.max(1, (int) (event.getNewDamage() / 4.0F));
            for (ItemStack armorStack : armorStacks) {
                damageFreeHandItem(armorStack, player, durabilityDamage);
            }
        }
    }

    // ==================== 事件订阅：每刻更新 ====================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        double armor = totalFreeHandArmor(player);
        double toughness = totalFreeHandArmorToughness(player);
        updateTransientModifier(player.getAttribute(Attributes.ARMOR), FREE_HAND_ARMOR_ID, armor);
        updateTransientModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), FREE_HAND_TOUGHNESS_ID, toughness);
        CRITICAL_HIT_MULTIPLIERS.keySet().removeIf(hit -> hit.playerId().equals(player.getUUID()));
    }

    // ==================== 公共 API：物品选择 ====================

    public static Optional<ItemStack> selectedMiningStack(Player player, BlockState state) {
        Optional<FreeHandStack> bestStack = bestMiningStack(player, state);
        if (bestStack.isEmpty()
                || !shouldUseFreeHandForMining(player.getMainHandItem(), bestStack.get().stack(), state)) {
            return Optional.empty();
        }
        return Optional.of(bestStack.get().stack());
    }

    public static Optional<ItemStack> selectedUseStack(Player player) {
        return freeHandUseStacks(player).stream().findFirst();
    }

    /**
     * 获取所有可用于右键交互的解放槽物品（按优先级排序）：铲平优先于锄地，
     * 避免单块回退因槽位顺序让锄头直接把泥土锄成耕地、跳过土径中间态。
     */
    public static List<ItemStack> freeHandUseStacks(Player player) {
        return freeHandStacks(player).stream()
                .filter(FreeHandEvents::canUseOnBlock)
                .sorted(Comparator.comparingInt(FreeHandEvents::rightClickPriority))
                .toList();
    }

    /**
     * 获取 Ultimine 连锁模式下可用的所有解放槽物品（含其他槽位的工具）。
     */
    public static List<ItemStack> ultimineUseStacks(Player player) {
        List<ItemStack> result = new ArrayList<>(freeHandUseStacks(player));
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getCurios().entrySet().stream()
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
        result.sort(Comparator.comparingInt(FreeHandEvents::rightClickPriority));
        return result;
    }

    // ==================== 公共 API：耐久损耗 ====================

    /**
     * 对解放槽物品造成耐久损耗，尊重原版 Unbreakable（isDamageableItem 已处理）。
     */
    public static void damageFreeHandItem(ItemStack stack, Player player, int amount) {
        if (amount <= 0 || !stack.isDamageableItem()) {
            return;
        }
        stack.hurtAndBreak(amount, player, EquipmentSlot.MAINHAND);
    }

    // ==================== 私有方法：右键优先级 ====================

    /**
     * 右键交互优先级：铲平 > 去皮 > 锄地 > 其他。确保泥土先变成土径，再变成耕地。
     */
    private static int rightClickPriority(ItemStack stack) {
        if (stack.canPerformAction(ItemAbilities.SHOVEL_FLATTEN)) {
            return 0;
        }
        if (stack.canPerformAction(ItemAbilities.AXE_STRIP)) {
            return 1;
        }
        if (stack.canPerformAction(ItemAbilities.HOE_TILL)) {
            return 2;
        }
        return 3;
    }

    // ==================== 私有方法：挖掘计算 ====================

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
            int efficiency = enchantLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0) {
                speed += efficiency * efficiency + 1;
            }
        }
        return speed;
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

    // ==================== 私有方法：战斗计算 ====================

    private static Optional<FreeHandStack> bestAttackStack(Player player, LivingEntity target, DamageSource source) {
        FreeHandStack bestStack = null;
        double bestDamage = 0.0D;
        for (ItemStack stack : freeHandStacks(player)) {
            double damage = attackDamage(stack, target, player, source);
            if (damage > bestDamage) {
                bestDamage = damage;
                bestStack = new FreeHandStack(stack);
            }
        }
        return Optional.ofNullable(bestStack);
    }

    /**
     * 解放槽武器的攻击伤害：饰品用能力值；普通武器 = ATTACK_DAMAGE 属性修饰符之和 + 附魔伤害加成
     * （1.21.1 移除了 getDamageBonus，改用 EnchantmentHelper.modifyDamage 以 0 基础值取得加成部分）。
     */
    private static double attackDamage(ItemStack stack, LivingEntity target, Player attacker, DamageSource source) {
        Optional<FreeHandAbility> ability = trinketAbility(stack);
        if (ability.isPresent()) {
            return ability.get().attackDamage();
        }

        double damage = 0.0D;
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE)
                    && (entry.slot() == EquipmentSlotGroup.MAINHAND || entry.slot() == EquipmentSlotGroup.ANY)) {
                damage += entry.modifier().amount();
            }
        }
        if (attacker != null && source != null && attacker.level() instanceof ServerLevel serverLevel) {
            damage += EnchantmentHelper.modifyDamage(serverLevel, stack, target, source, 0.0F);
        }
        return damage;
    }

    /**
     * 复刻原版 getDamageProtection，但只针对解放槽护甲栈：逐个附魔调用 modifyDamageProtection 累加保护值。
     */
    private static float freeHandProtection(ServerLevel serverLevel, Player player, List<ItemStack> armorStacks, DamageSource source) {
        MutableFloat protection = new MutableFloat(0.0F);
        for (ItemStack armorStack : armorStacks) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : armorStack.getEnchantments().entrySet()) {
                entry.getKey().value().modifyDamageProtection(
                        serverLevel, entry.getIntValue(), armorStack, player, source, protection);
            }
        }
        return protection.floatValue();
    }

    private static void applyWeaponSideEffects(ItemStack stack, LivingEntity target) {
        int fireAspect = enchantLevel(stack, Enchantments.FIRE_ASPECT);
        if (fireAspect > 0) {
            target.igniteForSeconds(fireAspect * 4);
        }
    }

    // ==================== 私有方法：护甲计算 ====================

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

    private static void updateTransientModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null) {
            attribute.removeModifier(existing);
        }
        if (amount > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // ==================== 私有方法：解放槽物品获取与附魔读取 ====================

    private static List<ItemStack> freeHandStacks(Player player) {
        return CuriosApi.getCuriosInventory(player)
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

    /**
     * 读取物品上某个附魔的等级（1.21.1 附魔为 ResourceKey，无需注册表 Holder 即可按 key 匹配遍历）。
     */
    private static int enchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    /**
     * 解放槽物品上某个附魔的最高等级。
     * <p>
     * 1.21.1 移除了 {@code LootingLevelEvent}，抢夺等级改由战利品结算时查询
     * {@code EnchantmentHelper.getEnchantmentLevel(LOOTING, attacker)}，而该查询只覆盖原版装备槽，
     * 故由 {@code EnchantmentHelperMixin} 调用本方法抬高原版结果。
     */
    public static int freeHandEnchantmentLevel(Player player, ResourceKey<Enchantment> key) {
        int level = 0;
        for (ItemStack stack : freeHandStacks(player)) {
            level = Math.max(level, enchantLevel(stack, key));
        }
        return level;
    }

    /**
     * 判断物品是否可用于方块交互：饰品、挖掘工具、剪刀，或声明了标准 ItemAbility 的自定义工具。
     */
    private static boolean canUseOnBlock(ItemStack stack) {
        if (stack.getItem() instanceof AbilityTrinketItem
                || stack.getItem() instanceof DiggerItem
                || stack.getItem() instanceof ShearsItem) {
            return true;
        }
        return stack.canPerformAction(ItemAbilities.HOE_TILL)
                || stack.canPerformAction(ItemAbilities.AXE_STRIP)
                || stack.canPerformAction(ItemAbilities.SHOVEL_FLATTEN)
                || stack.canPerformAction(ItemAbilities.SHEARS_CARVE);
    }

    private static boolean providesArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || trinketAbility(stack).map(ability ->
                        ability.armor() > 0.0D || ability.armorToughness() > 0.0D).orElse(false);
    }

    // ==================== 内部记录类 ====================

    private record FreeHandStack(ItemStack stack) {
    }

    private record CombatHit(UUID playerId, UUID targetId) {
    }
}
