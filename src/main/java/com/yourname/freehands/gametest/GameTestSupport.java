package com.yourname.freehands.gametest;

import com.mojang.authlib.GameProfile;
import com.yourname.freehands.FreeHands;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * GameTest 共享辅助工具（NeoForge 1.21.1）：玩家生成、饰品装备、右键模拟、爆炸伤害、附魔读写、
 * Ultimine 按键注入，以及测试用网络/玩家替身。
 * <p>
 * 本类不含任何 {@code @GameTest} 用例，仅供各测试类复用。
 */
final class GameTestSupport {
    static final float EXPLOSION_DAMAGE = 20.0F;
    static final float EPSILON = 0.001F;

    private GameTestSupport() {
    }

    // ==================== 玩家生成 ====================

    static Player spawnPlayer(GameTestHelper helper) {
        return spawnPlayer(helper, 1);
    }

    static Player spawnPlayer(GameTestHelper helper, int xOffset) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(xOffset, 2, 1)).getCenter());
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    /**
     * 连锁测试用的真实 ServerPlayer。
     * <p>
     * 必须是 ServerPlayer 本体而非子类：Architectury 的 {@code PlayerHooks.isFake} 判定条件是
     * {@code player instanceof ServerPlayer && player.getClass() != ServerPlayer.class}，
     * 子类会被 FTB Ultimine 当作伪玩家而直接跳过全部连锁逻辑。
     * 挥臂等广播包由 {@link SilentServerGamePacketListener} 吞掉，不需要子类覆写。
     */
    static ServerPlayer spawnServerPlayer(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "freehands-ultimine-test"), ClientInformation.createDefault());
        player.connection = new SilentServerGamePacketListener(helper.getLevel().getServer(), player);
        player.setPos(helper.absolutePos(new BlockPos(1, 2, 4)).getCenter());
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    /**
     * 普通 ServerPlayer（带 ClientInformation），用于不需要静默连接的右键测试。
     */
    static ServerPlayer newServerPlayer(GameTestHelper helper) {
        return new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-server-player"), ClientInformation.createDefault());
    }

    // ==================== 饰品装备 ====================

    static void equipTrinket(Player player, ItemStack stack) {
        equipTrinket(player, 0, stack);
    }

    static void equipTrinket(Player player, int slot, ItemStack stack) {
        CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.getStacksHandler(FreeHands.FREE_HAND_SLOT))
                .orElseThrow(() -> new IllegalStateException("Free Hand Curios inventory is unavailable"))
                .getStacks()
                .setStackInSlot(slot, stack);
    }

    static void equipIronArmorSet(Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }

    // ==================== 右键模拟 ====================

    static void useEmptyMainHandOn(ServerPlayer player, GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
        player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitResult);
        player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.OFF_HAND, hitResult);
    }

    // ==================== 爆炸伤害 ====================

    static float hurtWithExplosion(Player player) {
        float healthBefore = player.getHealth();
        player.hurt(player.damageSources().explosion(null, null), EXPLOSION_DAMAGE);
        return healthBefore - player.getHealth();
    }

    static float hurtWithPlayerOwnedTnt(Player player) {
        PrimedTnt tnt = new PrimedTnt(player.level(), player.getX(), player.getY(), player.getZ(), player);
        float healthBefore = player.getHealth();
        player.hurt(player.damageSources().explosion(tnt, player), EXPLOSION_DAMAGE);
        return healthBefore - player.getHealth();
    }

    // ==================== 附魔读写（1.21.1：ResourceKey + Holder） ====================

    static Holder<Enchantment> enchantHolder(ServerLevel level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    static void enchant(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key, int lvl) {
        stack.enchant(enchantHolder(level, key), lvl);
    }

    static int enchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    // ==================== Ultimine 按键注入 ====================

    /**
     * 通过反射按下 Ultimine 连锁键。未安装 FTB Ultimine（NeoForge 版）时返回 false，测试应跳过。
     * <p>
     * NeoForge 版内部 API 已核对：{@code getInstance()}/{@code getOrCreatePlayerData(Player)}/
     * {@code setKeyPressed(ServerPlayer, boolean)}/{@code isPressed()}/{@code cachedPos}/
     * {@code cachedPositions()} 与 1.20.1 Forge 版一致，仅单例从私有字段改为公开 {@code getInstance()}。
     * 连锁玩家必须是 {@code ServerPlayer} 本体（见 {@link #spawnServerPlayer}），否则 FTB Ultimine
     * 会因 {@code PlayerHooks.isFake} 判定为伪玩家而完全跳过连锁。
     */
    static boolean pressUltimineKey(ServerPlayer player, BlockPos targetPos) {
        try {
            Class<?> ultimineClass = Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine");
            // 1.21.1：FTB Ultimine 单例由私有字段 instance 改为公开静态 getInstance()
            Object instance = ultimineClass.getMethod("getInstance").invoke(null);
            if (instance == null) {
                return false;
            }

            aimAtBlock(player, targetPos);
            ultimineClass.getMethod("setKeyPressed", ServerPlayer.class, boolean.class).invoke(instance, player, true);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to invoke FTB Ultimine's public key API", exception);
        }
    }

    static void aimAtBlock(ServerPlayer player, BlockPos targetPos) {
        player.setPos(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 3.5D);
        player.setYRot(180.0F);
        player.setYHeadRot(180.0F);
        player.setYBodyRot(180.0F);
        player.setXRot(28.5F);
    }

    // ==================== 数值比较 ====================

    static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) <= EPSILON;
    }

    // ==================== 测试用网络/玩家替身 ====================

    static final class SilentServerGamePacketListener extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        private int soundPackets;

        SilentServerGamePacketListener(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
            super(server, new SilentConnection(), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet) {
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket) {
                soundPackets++;
            }
        }

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet,
                         net.minecraft.network.PacketSendListener listener) {
            send(packet);
        }

        void clearSoundPackets() {
            soundPackets = 0;
        }

        int soundPacketCount() {
            return soundPackets;
        }
    }

    static final class SilentConnection extends net.minecraft.network.Connection {
        SilentConnection() {
            super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(this);
        }

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet) {
        }

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet,
                         net.minecraft.network.PacketSendListener listener) {
        }
    }
}
