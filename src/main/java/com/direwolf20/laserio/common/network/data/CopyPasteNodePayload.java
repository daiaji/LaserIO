package com.direwolf20.laserio.common.network.data;

import com.direwolf20.laserio.common.LaserIO;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CopyPasteNodePayload(BlockPos pos, byte mode) implements CustomPacketPayload {
    public static final Type<CopyPasteNodePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "copy_paste_node"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CopyPasteNodePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CopyPasteNodePayload::pos,
            ByteBufCodecs.BYTE, CopyPasteNodePayload::mode,
            CopyPasteNodePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}