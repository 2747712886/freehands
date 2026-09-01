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
        Optional<ItemStack> selectedStack = FreeHandEvents.selectedMiningStack(player, state);
        if (selectedStack.isPresent()) {
            ACTIVE_MAIN_HAND_STACKS.get().push(new Entry(player.getUUID(), selectedStack.get(), false));
            return;
        }

        // Mods such as Ultimine call destroyBlock recursively. Keep the outer
        // virtual tool visible until every nested block break has completed.
        getVirtualMainHand(player)
                .ifPresent(stack -> ACTIVE_MAIN_HAND_STACKS.get().push(new Entry(player.getUUID(), stack, false)));
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

    public static void beginUsing(Player player, ItemStack stack) {
        ACTIVE_MAIN_HAND_STACKS.get().push(new Entry(player.getUUID(), stack, true));
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

    public static boolean isUsing(Player player) {
        Deque<Entry> stacks = ACTIVE_MAIN_HAND_STACKS.get();
        if (stacks.isEmpty()) {
            return false;
        }
        Entry stack = stacks.peek();
        return stack.playerId().equals(player.getUUID()) && stack.useForRightClick();
    }

    private record Entry(UUID playerId, ItemStack stack, boolean useForRightClick) {
    }
}
