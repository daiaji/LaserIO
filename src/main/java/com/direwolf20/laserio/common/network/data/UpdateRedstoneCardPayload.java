package com.direwolf20.laserio.common.network.data;

import com.direwolf20.laserio.common.LaserIO;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateRedstoneCardPayload(
        byte mode,
        byte channel,         // 基础网络频道 (用于节点连接)
        byte redstoneChannel, // 红石逻辑频道 (用于信号传输)
        boolean strong
) implements CustomPacketPayload {
    public static final Type<UpdateRedstoneCardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "update_redstone_card"));

    @Override
    public Type<UpdateRedstoneCardPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, UpdateRedstoneCardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, UpdateRedstoneCardPayload::mode,
            ByteBufCodecs.BYTE, UpdateRedstoneCardPayload::channel,
            ByteBufCodecs.BYTE, UpdateRedstoneCardPayload::redstoneChannel, // [新增]
            ByteBufCodecs.BOOL, UpdateRedstoneCardPayload::strong,
            UpdateRedstoneCardPayload::new
    );
}