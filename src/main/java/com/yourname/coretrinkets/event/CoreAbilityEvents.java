package com.yourname.coretrinkets.event;

import com.yourname.coretrinkets.CoreTrinkets;
import com.yourname.coretrinkets.core.CoreAbility;
import com.yourname.coretrinkets.item.CoreTrinketItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = CoreTrinkets.MODID)
public final class CoreAbilityEvents {
    private CoreAbilityEvents() {
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        Optional<CoreAbility> ability = equippedAbility(event.getEntity());
        if (ability.isPresent() && canHarvestWithCore(event.getTargetBlock(), ability.get())) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Optional<CoreAbility> ability = equippedAbility(event.getEntity());
        if (ability.isEmpty() || !canHarvestWithCore(event.getState(), ability.get())) {
            return;
        }

        ItemStack held = event.getEntity().getMainHandItem();
        if (held.isEmpty() || event.getNewSpeed() < ability.get().miningSpeed()) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), ability.get().miningSpeed()));
        }
    }

    private static Optional<CoreAbility> equippedAbility(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, stack -> stack.getItem() instanceof CoreTrinketItem)
                .map(slotResult -> ((CoreTrinketItem) slotResult.stack().getItem()).ability());
    }

    private static boolean canHarvestWithCore(BlockState state, CoreAbility ability) {
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
