package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 音效播放兼容 Mixin：确保解放槽虚拟工具用户能听到右键交互音效。
 * <p>
 * 原版 playSound 会排除发声者（excludedPlayer），但虚拟工具用户没有物理手持物品，
 * 需要解除排除，让客户端正确播放音效。
 */
@Mixin(Level.class)
public abstract class LevelMixin {
    /**
     * 修改 BlockPos 版 playSound 的 excludedPlayer 参数。
     */
    @ModifyVariable(
            method = "playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Player freehands$includeVirtualToolUserInSound(Player excludedPlayer) {
        return freehands$includeVirtualToolUser(excludedPlayer);
    }

    /**
     * 修改坐标版 playSound 的 excludedPlayer 参数。
     */
    @ModifyVariable(
            method = "playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;"
                    + "Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Player freehands$includeVirtualToolUserInPositionalSound(Player excludedPlayer) {
        return freehands$includeVirtualToolUser(excludedPlayer);
    }

    /**
     * 若 excludedPlayer 正在使用虚拟工具，则解除排除（设为 null）。
     */
    @Unique
    private static Player freehands$includeVirtualToolUser(Player excludedPlayer) {
        if (excludedPlayer != null && VirtualMainHandContext.isUsing(excludedPlayer)) {
            return null;
        }
        return excludedPlayer;
    }
}
