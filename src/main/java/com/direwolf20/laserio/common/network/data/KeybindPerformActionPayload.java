package com.direwolf20.laserio.common.network.data;

import com.direwolf20.laserio.common.LaserIO;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KeybindPerformActionPayload(Action action) implements CustomPacketPayload {
    public static final Type<KeybindPerformActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "keybind_perform_action"));

    public static final StreamCodec<FriendlyByteBuf, KeybindPerformActionPayload> STREAM_CODEC = StreamCodec.composite(
            Action.STREAM_CODEC, KeybindPerformActionPayload::action,
            KeybindPerformActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        OPEN_CARD_HOLDER,
        TOGGLE_CARD_HOLDER_PULLING;

        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Action decode(FriendlyByteBuf buf) {
                return values()[buf.readByte()];
            }

            @Override
            public void encode(FriendlyByteBuf buf, Action action) {
                buf.writeByte(action.ordinal());
            }
        };
    }
}