package com.yourname.freehands.compat;

import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

/**
 * 虚拟主手上下文管理器：通过 ThreadLocal 栈结构管理解放槽工具的"虚拟主手"状态。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>在挖掘时提供虚拟工具堆叠，使得 getMainHandItem() 返回解放槽物品</li>
 *   <li>在右键交互时提供虚拟工具堆叠，使得 useItemOn 使用解放槽物品</li>
 *   <li>支持嵌套调用（如 Ultimine 递归破坏方块），通过栈结构保证内外层状态正确</li>
 * </ul>
 */
public final class VirtualMainHandContext {
    private static final ThreadLocal<Deque<Entry>> ACTIVE_MAIN_HAND_STACKS = ThreadLocal.withInitial(ArrayDeque::new);

    private VirtualMainHandContext() {
    }

    /**
     * 开始虚拟挖掘：选择最佳挖掘工具并压入栈中。
     * 若已有虚拟工具且为嵌套调用（如 Ultimine 递归），保留外层工具。
     */
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

    /**
     * 结束虚拟挖掘：弹出栈顶元素，若栈为空则清理 ThreadLocal。
     */
    public static void endMining(Player player) {
        Deque<Entry> stacks = ACTIVE_MAIN_HAND_STACKS.get();
        if (!stacks.isEmpty() && stacks.peek().playerId().equals(player.getUUID())) {
            stacks.pop();
        }
        if (stacks.isEmpty()) {
            ACTIVE_MAIN_HAND_STACKS.remove();
        }
    }

    /**
     * 开始虚拟右键交互：将指定物品压入栈中，标记为 useForRightClick=true。
     */
    public static void beginUsing(Player player, ItemStack stack) {
        ACTIVE_MAIN_HAND_STACKS.get().push(new Entry(player.getUUID(), stack, true));
    }

    /**
     * 结束虚拟右键交互：复用 endMining 逻辑（弹出栈顶）。
     */
    public static void endUsing(Player player) {
        endMining(player);
    }

    /**
     * 获取当前玩家的虚拟主手物品（栈顶元素）。
     */
    public static Optional<ItemStack> getVirtualMainHand(Player player) {
        Deque<Entry> stacks = ACTIVE_MAIN_HAND_STACKS.get();
        if (stacks.isEmpty()) {
            return Optional.empty();
        }

        Entry stack = stacks.peek();
        return stack.playerId().equals(player.getUUID()) ? Optional.of(stack.stack()) : Optional.empty();
    }

    /**
     * 判断当前玩家是否正在使用虚拟工具进行右键交互。
     */
    public static boolean isUsing(Player player) {
        Deque<Entry> stacks = ACTIVE_MAIN_HAND_STACKS.get();
        if (stacks.isEmpty()) {
            return false;
        }
        Entry stack = stacks.peek();
        return stack.playerId().equals(player.getUUID()) && stack.useForRightClick();
    }

    /**
     * 内部记录类：存储虚拟主手条目（玩家ID、物品堆叠、是否用于右键）。
     */
    private record Entry(UUID playerId, ItemStack stack, boolean useForRightClick) {
    }
}
