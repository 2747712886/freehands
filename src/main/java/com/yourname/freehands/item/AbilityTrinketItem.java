package com.yourname.freehands.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.yourname.freehands.FreeHands;
import com.yourname.freehands.ability.FreeHandAbility;
import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.DataMapHooks;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;

/**
 * 能力饰品：放入解放槽后提供整套对应材质的挖掘/战斗/防御能力，并自带锹铲平、斧去皮/刮锈/去蜡、
 * 剪刀雕南瓜、熄营火、催熟藤蔓等右键交互。
 * <p>
 * 1.21.1 移植要点：去皮/铲平改用原版公开静态助手 {@code AxeItem.getAxeStrippingState} /
 * {@code ShovelItem.getShovelPathingState}，去蜡改用 NeoForge {@code DataMapHooks.getBlockUnwaxed}
 * （均无反射，且兼容数据驱动的模组方块）；雕南瓜因原版 {@code PumpkinBlock.useItemOn} 在 1.21.1
 * 只认 {@code Items.SHEARS}，故在此显式复刻其行为以保留饰品雕南瓜能力。
 */
public class AbilityTrinketItem extends Item implements ICurioItem {
    private final FreeHandAbility ability;

    public AbilityTrinketItem(Properties properties, FreeHandAbility ability) {
        super(properties);
        this.ability = ability;
    }

    public FreeHandAbility ability() {
        return ability;
    }

    // ==================== Curios 装备规则 ====================

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return FreeHands.FREE_HAND_SLOT.equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

    // ==================== 挖掘 / 掉落 ====================

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
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

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && entity instanceof Player player) {
            FreeHandEvents.damageFreeHandItem(stack, player, 1);
        }
        return true;
    }

    // ==================== 右键交互 ====================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 朝下面不执行任何交互
        if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // 斧：去皮 → 刮锈 → 去蜡
        BlockState stripped = AxeItem.getAxeStrippingState(state);
        if (stripped != null) {
            return applyModifiedState(context, stripped, SoundEvents.AXE_STRIP, 0);
        }
        Optional<BlockState> scraped = WeatheringCopper.getPrevious(state);
        if (scraped.isPresent()) {
            return applyModifiedState(context, scraped.get(), SoundEvents.AXE_SCRAPE, 3005);
        }
        Block unwaxed = DataMapHooks.getBlockUnwaxed(state.getBlock());
        if (unwaxed != null) {
            return applyModifiedState(context, unwaxed.withPropertiesOf(state), SoundEvents.AXE_WAX_OFF, 3004);
        }

        // 锹：铲平（需方块上方为空气）
        if (level.isEmptyBlock(pos.above())) {
            BlockState path = ShovelItem.getShovelPathingState(state);
            if (path != null) {
                return applyModifiedState(context, path, SoundEvents.SHOVEL_FLATTEN, 0);
            }
        }

        // 剪刀：雕南瓜（1.21.1 原版仅认真剪刀，饰品自行复刻）
        if (state.getBlock() instanceof PumpkinBlock) {
            return carvePumpkin(context);
        }

        // 熄灭点燃的营火
        if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
            if (!level.isClientSide()) {
                level.levelEvent(null, 1009, pos, 0);
            }
            CampfireBlock.dowse(context.getPlayer(), level, pos, state);
            return applyModifiedState(context, state.setValue(CampfireBlock.LIT, false), null, 0);
        }

        // 催熟生长类植物（如洞穴藤蔓）
        if (state.getBlock() instanceof GrowingPlantHeadBlock growingPlant && !growingPlant.isMaxAge(state)) {
            return applyModifiedState(context, growingPlant.getMaxAgeState(state), SoundEvents.GROWING_PLANT_CROP, 0);
        }

        return InteractionResult.PASS;
    }

    /**
     * 复刻原版 {@code PumpkinBlock.useItemOn} 的雕刻行为：朝向取自点击面（顶/底面时取玩家反向），
     * 播放雕刻音、掉落 4 个南瓜种子、扣 1 点耐久。
     */
    private InteractionResult carvePumpkin(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }

        Direction clickedFace = context.getClickedFace();
        Direction facing = clickedFace.getAxis() == Direction.Axis.Y
                ? (player != null ? player.getDirection().getOpposite() : clickedFace)
                : clickedFace;
        level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
        BlockState carved = Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, facing);
        level.setBlock(pos, carved, 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, carved));

        ItemEntity seeds = new ItemEntity(level,
                pos.getX() + 0.5D + facing.getStepX() * 0.65D,
                pos.getY() + 0.1D,
                pos.getZ() + 0.5D + facing.getStepZ() * 0.65D,
                new ItemStack(Items.PUMPKIN_SEEDS, 4));
        seeds.setDeltaMovement(facing.getStepX() * 0.05D, 0.05D, facing.getStepZ() * 0.05D);
        level.addFreshEntity(seeds);

        if (player != null) {
            FreeHandEvents.damageFreeHandItem(context.getItemInHand(), player, 1);
        }
        return InteractionResult.sidedSuccess(false);
    }

    /**
     * 应用修改后的方块状态：服务端 setBlock + gameEvent + 可选音效/粒子事件 + 扣 1 耐久。
     */
    private InteractionResult applyModifiedState(UseOnContext context, BlockState modifiedState, SoundEvent sound, int levelEvent) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!level.isClientSide()) {
            level.setBlock(pos, modifiedState, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, modifiedState));
            if (sound != null) {
                level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            if (levelEvent != 0) {
                level.levelEvent(player, levelEvent, pos, 0);
            }
            if (player != null) {
                FreeHandEvents.damageFreeHandItem(context.getItemInHand(), player, 1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // ==================== 工具能力 / 附魔 / 属性 ====================

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        // 与 1.20.1 一致：镐挖掘 + 锹铲平 + 剪刀雕南瓜；不含 SHEARS_HARVEST（否则树叶被当剪刀掉落），
        // 不含 HOE_TILL（配方无锄，饰品不应锄地）。
        return itemAbility == ItemAbilities.PICKAXE_DIG
                || itemAbility == ItemAbilities.SHOVEL_FLATTEN
                || itemAbility == ItemAbilities.SHEARS_CARVE;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        if (ability.armor() > 0.0D) {
            modifiers.put(Attributes.ARMOR, new AttributeModifier(id, ability.armor(), AttributeModifier.Operation.ADD_VALUE));
        }
        if (ability.armorToughness() > 0.0D) {
            modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, ability.armorToughness(), AttributeModifier.Operation.ADD_VALUE));
        }
        if (ability.knockbackResistance() > 0.0D) {
            modifiers.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, ability.knockbackResistance(), AttributeModifier.Operation.ADD_VALUE));
        }
        return modifiers;
    }

    // 1.21.1 移除了 canApplyAtEnchantingTable：可附魔性改由 data/minecraft/tags/item/enchantable/*.json 声明
    @Override
    public int getEnchantmentValue() {
        return ability.enchantmentValue();
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, Item.TooltipContext tooltipContext, ItemStack stack) {
        tooltips.add(Component.translatable("tooltip.freehands.slot.free_hand"));
        return tooltips;
    }
}
