package com.direwolf20.laserio.common.network.data;

import com.direwolf20.laserio.common.LaserIO;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateRedstoneCardPayload(
        byte mode,
        byte channel,
        byte redstoneChannel,
        boolean interval,
        byte intervalLowerBound,
        byte intervalUpperBound,
        byte intervalOutput,
        boolean strong,
        byte outputMode,
        byte logicOperation,
        byte logicOperationChannel
) implements CustomPacketPayload {
    
    public static final Type<UpdateRedstoneCardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "update_redstone_card"));

    @Override
    public Type<UpdateRedstoneCardPayload> type() {
        return TYPE;
    }

    // [Fix] 由于字段数量(11个)超过了 StreamCodec.composite 的限制(通常为6-8个)，
    // 必须手动实现 encode 和 decode 方法，而不能使用自动组合器。
    public static final StreamCodec<FriendlyByteBuf, UpdateRedstoneCardPayload> STREAM_CODEC = StreamCodec.ofMember(
            UpdateRedstoneCardPayload::write,
            UpdateRedstoneCardPayload::decode
    );

    /**
     * 手动将数据写入缓冲区
     */
    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(this.mode);
        buffer.writeByte(this.channel);
        buffer.writeByte(this.redstoneChannel);
        buffer.writeBoolean(this.interval);
        buffer.writeByte(this.intervalLowerBound);
        buffer.writeByte(this.intervalUpperBound);
        buffer.writeByte(this.intervalOutput);
        buffer.writeBoolean(this.strong);
        buffer.writeByte(this.outputMode);
        buffer.writeByte(this.logicOperation);
        buffer.writeByte(this.logicOperationChannel);
    }

    /**
     * 手动从缓冲区读取数据并构造对象
     */
    public static UpdateRedstoneCardPayload decode(FriendlyByteBuf buffer) {
        return new UpdateRedstoneCardPayload(
                buffer.readByte(),    // mode
                buffer.readByte(),    // channel
                buffer.readByte(),    // redstoneChannel
                buffer.readBoolean(), // interval
                buffer.readByte(),    // intervalLowerBound
                buffer.readByte(),    // intervalUpperBound
                buffer.readByte(),    // intervalOutput
                buffer.readBoolean(), // strong
                buffer.readByte(),    // outputMode
                buffer.readByte(),    // logicOperation
                buffer.readByte()     // logicOperationChannel
        );
    }
}