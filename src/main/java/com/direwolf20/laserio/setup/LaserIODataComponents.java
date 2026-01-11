package com.direwolf20.laserio.setup;

import com.direwolf20.laserio.common.LaserIO;
import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class LaserIODataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.createDataComponents(LaserIO.MODID);

    // --- Base / Wrench ---
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> BOUND_GLOBAL_POS = register("bound_global_pos", GlobalPos.CODEC, GlobalPos.STREAM_CODEC);

    // --- Card Holder ---
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CARD_HOLDER_ACTIVE = register("card_holder_active", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> CARD_HOLDER_UUID = register("card_holder_uuid", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);
    
    // 使用 ItemContainerContents 存储内容 (CacheEncoding 优化)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> ITEMSTACK_HANDLER = COMPONENTS.register("itemstack_handler", () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding().build());

    // --- Card Cloner ---
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CARD_CLONER_ITEM_TYPE = register("card_cloner_item_type", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> CLONER_NODE_DATA = register("cloner_node_data", CompoundTag.CODEC, ByteBufCodecs.COMPOUND_TAG);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CLONER_PASTE_NETWORK_ONLY = register("cloner_paste_network_only", Codec.BOOL, ByteBufCodecs.BOOL);

    // --- Card Settings ---
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> CARD_TRANSFER_MODE = register("card_transfer_mode", Codec.BYTE, ByteBufCodecs.BYTE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> CARD_CHANNEL = register("card_channel", Codec.BYTE, ByteBufCodecs.BYTE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CARD_EXTRACT_SPEED = register("card_extract_speed", Codec.INT, ByteBufCodecs.VAR_INT);
    // 注意：这里包含了 Backoff 功能
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> CARD_MAX_BACKOFF = register("card_max_backoff", Codec.BYTE, ByteBufCodecs.BYTE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Short>> CARD_PRIORITY = register("card_priority", Codec.SHORT, ByteBufCodecs.SHORT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> CARD_SNEAKY = register("card_sneaky", Codec.BYTE, ByteBufCodecs.BYTE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CARD_REGULATE = register("card_regulate", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CARD_ROUND_ROBIN = register("card_round_robin", Codec.INT, ByteBufCodecs.VAR_INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> CARD_REDSTONE_MODE = register("card_redstone_mode", Codec.BYTE, ByteBufCodecs.BYTE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CARD_EXACT = register("card_exact", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> CARD_REDSTONE_CHANNEL = register("card_redstone_channel", Codec.BYTE, ByteBufCodecs.BYTE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CARD_AND_MODE = register("card_and_mode", Codec.BOOL, ByteBufCodecs.BOOL);

    // --- Specific Card Settings ---
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY_CARD_EXTRACT_AMT = register("energy_card_extract_amt", Codec.INT, ByteBufCodecs.VAR_INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY_CARD_EXTRACT_SPEED = register("energy_card_extract_speed", Codec.INT, ByteBufCodecs.VAR_INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY_CARD_INSERT_LIMIT = register("energy_card_insert_limit", Codec.INT, ByteBufCodecs.VAR_INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY_CARD_EXTRACT_LIMIT = register("energy_card_extract_limit", Codec.INT, ByteBufCodecs.VAR_INT);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLUID_CARD_EXTRACT_AMT = register("fluid_card_extract_amt", Codec.INT, ByteBufCodecs.VAR_INT);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> ITEM_CARD_EXTRACT_AMT = register("item_card_extract_amt", Codec.BYTE, ByteBufCodecs.BYTE);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> REDSTONE_CARD_STRONG = register("redstone_card_strong", Codec.BOOL, ByteBufCodecs.BOOL);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHEMICAL_CARD_EXTRACT_AMT = register("chemical_card_extract_amt", Codec.INT, ByteBufCodecs.VAR_INT);

    // --- Filters ---
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FILTER_ALLOW = register("filter_allow", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FILTER_COMPARE = register("filter_compare", Codec.BOOL, ByteBufCodecs.BOOL);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> FILTER_COUNT_MBAMT = register("filter_amount_mbamt", Codec.INT.listOf(), ByteBufCodecs.INT.apply(ByteBufCodecs.list()));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> FILTER_COUNT_SLOT_COUNTS = register("filter_amount_slot_counts", Codec.INT.listOf(), ByteBufCodecs.INT.apply(ByteBufCodecs.list()));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> FILTER_TAG_TAGS = register("filter_tag_tags", Codec.STRING.listOf(), ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()));

    // --- Helper Methods ---
    
    // 辅助方法：注册没有 StreamCodec 的组件 (仅服务端/持久化)
    private static @NotNull <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, final Codec<T> codec) {
        return register(name, codec, null);
    }

    // 辅助方法：注册带 StreamCodec 的组件 (网络同步)
    private static @NotNull <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, final Codec<T> codec, @Nullable final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return COMPONENTS.register(name, () -> {
            DataComponentType.Builder<T> builder = DataComponentType.<T>builder().persistent(codec);
            if (streamCodec != null) {
                builder.networkSynchronized(streamCodec);
            }
            return builder.build();
        });
    }
}