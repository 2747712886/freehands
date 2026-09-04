package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * FTB Ultimine 连锁右键兼容 Mixin（NeoForge 1.21.1，FTB Ultimine 2101.1.x）。
 * <p>
 * 在原版 Ultimine 处理完主逻辑后，尝试用解放槽物品执行额外的连锁交互：
 * <ul>
 *   <li>副手物品（当主手已处理时跳过）</li>
 *   <li>解放槽中的普通工具（走 Ultimine 原生连锁）</li>
 *   <li>饰品或其他模组物品（走逐位置 useItemOn 兜底分支）</li>
 * </ul>
 * 1.20.1→1.21.1：FTB Ultimine 内部 API 基本不变（仍用 Architectury {@code EventResult}、
 * {@code blockRightClick}/{@code getOrCreatePlayerData}/{@code isPressed}/{@code cachedPos}/{@code cachedPositions}），
 * 仅单例由私有字段 {@code instance} 改为公开 {@code getInstance()}，工具动作由 ToolAction 改为 ItemAbility。
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbultimine.FTBUltimine", remap = false)
public abstract class FTBUltimineMixin {
    @Unique
    private static final ThreadLocal<Boolean> freehands$dispatching = ThreadLocal.withInitial(() -> false);

    /**
     * 在 Ultimine 的 blockRightClick 返回后注入，尝试用解放槽物品补充执行。
     */
    @Inject(method = "blockRightClick", at = @At("RETURN"), cancellable = true)
    private void freehands$retryWithOffHandAndAccessories(Player player, InteractionHand hand, BlockPos pos,
                                                          net.minecraft.core.Direction direction,
                                                          CallbackInfoReturnable<?> callback) {
        if (!(player instanceof ServerPlayer serverPlayer) || freehands$dispatching.get()) {
            return;
        }

        Object result = callback.getReturnValue();
        boolean originalHandled = freehands$handled(result);

        // 若 Ultimine 原生已处理，记录位置并返回（供副手包去重）
        if (originalHandled) {
            if (hand == InteractionHand.MAIN_HAND) {
                FreeHandEvents.recordFreeHandHandled(serverPlayer, pos);
            }
            return;
        }

        // 副手空包去重：若主手已通过解放槽处理过此位置，跳过
        if (hand == InteractionHand.OFF_HAND && FreeHandEvents.isFreeHandHandled(serverPlayer, pos)) {
            return;
        }

        freehands$dispatching.set(true);
        try {
            // 1. 主手时尝试副手物品
            if (hand == InteractionHand.MAIN_HAND) {
                ItemStack offStack = serverPlayer.getOffhandItem();
                if (!offStack.isEmpty() && freehands$isSupportedRightClickTool(offStack)) {
                    result = freehands$invokeBlockRightClick(player, InteractionHand.OFF_HAND, pos, direction);
                    if (freehands$handled(result)) {
                        FreeHandEvents.recordFreeHandHandled(serverPlayer, pos);
                        serverPlayer.swing(InteractionHand.MAIN_HAND, true);
                        freehands$setReturnValue(callback, result);
                        return;
                    }
                }
            }

            // 2. 尝试解放槽物品链式交互
            var stacks = FreeHandEvents.ultimineUseStacks(serverPlayer);
            for (var accessoryStack : stacks) {
                boolean accessoryHandled = false;
                Object handledResult = null;

                if (!(accessoryStack.getItem() instanceof com.yourname.freehands.item.AbilityTrinketItem)) {
                    // 普通工具优先走 Ultimine 原生连锁（锄/剥/铲/收作物）
                    VirtualMainHandContext.beginUsing(serverPlayer, accessoryStack);
                    try {
                        result = freehands$invokeBlockRightClick(player, InteractionHand.MAIN_HAND, pos, direction);
                    } finally {
                        VirtualMainHandContext.endUsing(serverPlayer);
                    }
                    if (freehands$handled(result)) {
                        accessoryHandled = true;
                        handledResult = result;
                    }
                }

                if (!accessoryHandled) {
                    // 2101.1.15 的内置处理器（斧→锹→锄→作物）已统一改为 getMainHandItem().useOn，
                    // 但顺序写死在 RightClickDispatcher 里、且没有剪刀雕南瓜等动作的处理器；
                    // 因此饰品的多动作与优先级仍由逐位置 useItemOn 兜底分支承担，
                    // 普通工具未被 Ultimine 处理时（如雕南瓜、其他模组的自定义交互）也走这里。
                    if (freehands$useStackOnCachedChain(serverPlayer, accessoryStack, pos)) {
                        accessoryHandled = true;
                        handledResult = freehands$interruptResult();
                    }
                }

                if (accessoryHandled) {
                    FreeHandEvents.recordFreeHandHandled(serverPlayer, pos);
                    // Ultimine 内部 swing 只广播给其他玩家（updateSelf=false），
                    // 客户端又无法预知解放槽动作，须由服务端把挥臂动画发给玩家自己。
                    serverPlayer.swing(InteractionHand.MAIN_HAND, true);
                    freehands$setReturnValue(callback, handledResult);
                    return;
                }
            }
        } finally {
            freehands$dispatching.remove();
        }
    }

    /**
     * 对 Ultimine 缓存的连锁位置逐个执行完整 useItemOn。
     * 通过方块状态变化判定是否生效（不能依赖返回值：interruptFalse 映射为 FAIL）。
     */
    @SuppressWarnings("unchecked")
    @Unique
    private boolean freehands$useStackOnCachedChain(ServerPlayer player, ItemStack stack, BlockPos clickedPos) {
        java.util.Collection<BlockPos> positions;
        try {
            Class<?> ultimineClass = Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine");
            // 1.21.1：单例由私有字段 instance 改为公开静态 getInstance()
            Object instance = ultimineClass.getMethod("getInstance").invoke(null);
            Object data = ultimineClass.getMethod("getOrCreatePlayerData", Player.class).invoke(instance, player);
            if (!(boolean) data.getClass().getMethod("isPressed").invoke(data)) {
                return false;
            }
            java.lang.reflect.Field cachedPosField = data.getClass().getDeclaredField("cachedPos");
            cachedPosField.setAccessible(true);
            if (!clickedPos.equals(cachedPosField.get(data))) {
                // 缓存位置来自上一次点击（本次调用未走到 updateBlocks），不使用旧缓存
                return false;
            }
            positions = (java.util.Collection<BlockPos>) data.getClass().getMethod("cachedPositions").invoke(data);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read FTB Ultimine chain positions", exception);
        }

        if (positions == null || positions.isEmpty()) {
            return false;
        }

        boolean anyHandled = false;
        for (BlockPos chainPos : positions) {
            net.minecraft.world.level.block.state.BlockState before = player.level().getBlockState(chainPos);
            net.minecraft.world.phys.BlockHitResult chainHit = new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(chainPos),
                    net.minecraft.core.Direction.UP, chainPos, false);
            VirtualMainHandContext.beginUsing(player, stack);
            try {
                player.gameMode.useItemOn(player, player.level(), stack, InteractionHand.MAIN_HAND, chainHit);
                if (player.level().getBlockState(chainPos) != before) {
                    anyHandled = true;
                }
            } finally {
                VirtualMainHandContext.endUsing(player);
            }
        }
        return anyHandled;
    }

    /**
     * 创建表示"已处理"的 EventResult 对象（interruptFalse）。
     */
    @Unique
    private static Object freehands$interruptResult() {
        try {
            return Class.forName("dev.architectury.event.EventResult")
                    .getMethod("interruptFalse").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create FTB Ultimine right-click result", exception);
        }
    }

    /**
     * 判断物品是否为支持的右键工具（铲平/去皮/锄地）。
     */
    @Unique
    private static boolean freehands$isSupportedRightClickTool(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.HOE_TILL)
                || stack.canPerformAction(ItemAbilities.AXE_STRIP)
                || stack.canPerformAction(ItemAbilities.SHOVEL_FLATTEN);
    }

    /**
     * 反射调用 Ultimine 的 blockRightClick 方法。
     */
    @Unique
    private Object freehands$invokeBlockRightClick(Player player, InteractionHand hand, BlockPos pos,
                                                    net.minecraft.core.Direction direction) {
        try {
            Method method = this.getClass().getMethod("blockRightClick", Player.class, InteractionHand.class,
                    BlockPos.class, net.minecraft.core.Direction.class);
            return method.invoke(this, player, hand, pos, direction);
        } catch (IllegalAccessException | NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to invoke FTB Ultimine right-click handler", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("FTB Ultimine right-click handler failed", exception.getCause());
        }
    }

    /**
     * 判断 Ultimine 返回结果是否表示"已处理"（interruptsFurtherEvaluation）。
     */
    @Unique
    private static boolean freehands$handled(Object result) {
        try {
            return (boolean) result.getClass()
                    .getMethod("interruptsFurtherEvaluation").invoke(result);
        } catch (IllegalAccessException | NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to inspect FTB Ultimine right-click result", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Unable to inspect FTB Ultimine right-click result",
                    exception.getCause());
        }
    }

    /**
     * 安全设置回调返回值（泛型擦除后的 unchecked 转换）。
     */
    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void freehands$setReturnValue(CallbackInfoReturnable<?> callback, Object result) {
        ((CallbackInfoReturnable) callback).setReturnValue(result);
    }
}
