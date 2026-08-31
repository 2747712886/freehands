package com.yourname.freehands.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.yourname.freehands.FreeHands;
import com.yourname.freehands.ability.FreeHandAbility;
import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class AbilityTrinketItem extends Item implements ICurioItem {
    private final FreeHandAbility ability;

    public AbilityTrinketItem(Properties properties, FreeHandAbility ability) {
        super(properties);
        this.ability = ability;
    }

    public FreeHandAbility ability() {
        return ability;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return FreeHands.FREE_HAND_SLOT.equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        InteractionResult axeResult = tryAxeStrip(context, state);
        if (axeResult != InteractionResult.PASS) {
            return axeResult;
        }
        axeResult = tryAxeScrape(context, state);
        if (axeResult != InteractionResult.PASS) {
            return axeResult;
        }
        axeResult = tryAxeWaxOff(context, state);
        if (axeResult != InteractionResult.PASS) {
            return axeResult;
        }

        InteractionResult flattenResult = modifyBlock(context, state, ToolActions.SHOVEL_FLATTEN,
                SoundEvents.SHOVEL_FLATTEN, 0, true);
        if (flattenResult != InteractionResult.PASS) {
            return flattenResult;
        }


        if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
            if (!level.isClientSide()) {
                level.levelEvent(null, 1009, pos, 0);
            }
            CampfireBlock.dowse(player, level, pos, state);
            return applyModifiedState(context, state.setValue(CampfireBlock.LIT, false));
        }

        if (state.getBlock() instanceof GrowingPlantHeadBlock growingPlant && !growingPlant.isMaxAge(state)) {
            level.playSound(player, pos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
            return applyModifiedState(context, growingPlant.getMaxAgeState(state));
        }
        return InteractionResult.PASS;
    }

    @SuppressWarnings("unchecked")
    private InteractionResult tryAxeStrip(UseOnContext context, BlockState state) {
        try {
            java.lang.reflect.Field field = net.minecraft.world.item.AxeItem.class.getDeclaredField("STRIPPABLES");
            field.setAccessible(true);
            java.util.Map<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> strippables =
                    (java.util.Map<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block>) field.get(null);
            net.minecraft.world.level.block.Block stripped = strippables.get(state.getBlock());
            if (stripped != null) {
                return applyModifiedState(context, stripped.defaultBlockState(), SoundEvents.AXE_STRIP);
            }
        } catch (Exception ignored) {
        }
        return InteractionResult.PASS;
    }

    @SuppressWarnings("unchecked")
    private InteractionResult tryAxeScrape(UseOnContext context, BlockState state) {
        java.util.Optional<BlockState> previous = net.minecraft.world.level.block.WeatheringCopper.getPrevious(state);
        if (previous.isPresent()) {
            return applyModifiedState(context, previous.get(), SoundEvents.AXE_SCRAPE);
        }
        return InteractionResult.PASS;
    }

    @SuppressWarnings("unchecked")
    private InteractionResult tryAxeWaxOff(UseOnContext context, BlockState state) {
        try {
            java.lang.reflect.Field field = net.minecraft.world.item.HoneycombItem.class.getDeclaredField("WAXABLES");
            field.setAccessible(true);
            java.util.function.Supplier<com.google.common.collect.BiMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block>> supplier =
                    (java.util.function.Supplier<com.google.common.collect.BiMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block>>) field.get(null);
            com.google.common.collect.BiMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> waxables = supplier.get();
            net.minecraft.world.level.block.Block unwaxed = waxables.inverse().get(state.getBlock());
            if (unwaxed != null) {
                return applyModifiedState(context, unwaxed.defaultBlockState(), SoundEvents.AXE_WAX_OFF);
            }
        } catch (Exception ignored) {
        }
        return InteractionResult.PASS;
    }

    private InteractionResult modifyBlock(UseOnContext context, BlockState state, ToolAction action,
                                          SoundEvent sound, int levelEvent, boolean requiresAirAbove) {
        BlockState modifiedState = state.getToolModifiedState(context, action, false);
        if (modifiedState == null || (requiresAirAbove && !context.getLevel().isEmptyBlock(context.getClickedPos().above()))) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        level.playSound(context.getPlayer(), context.getClickedPos(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (levelEvent != 0) {
            level.levelEvent(context.getPlayer(), levelEvent, context.getClickedPos(), 0);
        }
        return applyModifiedState(context, modifiedState);
    }

    private InteractionResult applyModifiedState(UseOnContext context, BlockState modifiedState) {
        return applyModifiedState(context, modifiedState, null);
    }

    private InteractionResult applyModifiedState(UseOnContext context, BlockState modifiedState, SoundEvent sound) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!level.isClientSide()) {
            level.setBlock(pos, modifiedState, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, modifiedState));
            if (sound != null) {
                level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            if (player != null) {
                FreeHandEvents.damageFreeHandItem(context.getItemInHand(), player, 1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(toolAction)
                || ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(toolAction)
                || toolAction == ToolActions.SHEARS_CARVE;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        if (ability.armor() > 0.0D) {
            modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid, "Free hand trinket armor", ability.armor(), AttributeModifier.Operation.ADDITION));
        }
        if (ability.armorToughness() > 0.0D) {
            modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "Free hand trinket armor toughness", ability.armorToughness(), AttributeModifier.Operation.ADDITION));
        }
        if (ability.knockbackResistance() > 0.0D) {
            modifiers.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Free hand trinket knockback resistance", ability.knockbackResistance(), AttributeModifier.Operation.ADDITION));
        }
        return modifiers;
    }

    @Override
    public int getEnchantmentValue() {
        return ability.enchantmentValue();
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return true;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        tooltips.add(Component.translatable("tooltip.freehands.slot.free_hand"));
        return tooltips;
    }
}
