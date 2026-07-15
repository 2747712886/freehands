package com.yourname.freehands.compat;

import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

public final class VirtualMainHandContext {
    private static final ThreadLocal<Deque<Entry>> ACTIVE_MAIN_HAND_STACKS = ThreadLocal.withInitial(ArrayDeque::new);

    private VirtualMainHandContext() {
    }

    public static void beginMining(Player player, BlockState state) {
        FreeHandEvents.selectedMiningStack(player, state)
                .ifPresent(stack -> ACTIVE_MAIN_HAND_STACKS.get().push(new Entry(player.getUUID(), stack)));
    }

    public static void endMining(Player player) {
        Deque<Entry> stacks = ACTIVE_MAIN_HAND_STACKS.get();
        if (!stacks.isEmpty() && stacks.peek().playerId().equals(player.getUUID())) {
            stacks.pop();
        }
        if (stacks.isEmpty()) {
            ACTIVE_MAIN_HAND_STACKS.remove();
        }
    }

    public static Optional<ItemStack> beginUsing(Player player) {
        return FreeHandEvents.selectedUseStack(player).map(stack -> {
            beginUsing(player, stack);
            return stack;
        });
    }

    public static void beginUsing(Player player, ItemStack stack) {
        ACTIVE_MAIN_HAND_STACKS.get().push(new Entry(player.getUUID(), stack));
    }

    public static void endUsing(Player player) {
        endMining(player);
    }

    public static Optional<ItemStack> getVirtualMainHand(Player player) {
        Deque<Entry> stacks = ACTIVE_MAIN_HAND_STACKS.get();
        if (stacks.isEmpty()) {
            return Optional.empty();
        }

        Entry stack = stacks.peek();
        return stack.playerId().equals(player.getUUID()) ? Optional.of(stack.stack()) : Optional.empty();
    }

    private record Entry(UUID playerId, ItemStack stack) {
    }
}
