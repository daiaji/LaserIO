package com.direwolf20.laserio.client.events;

import com.direwolf20.laserio.common.LaserIO;
import com.direwolf20.laserio.common.network.data.KeybindPerformActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = LaserIO.MODID, value = Dist.CLIENT)
public class KeybindHandler {
    public static final KeyMapping OPEN_CARD_HOLDER = new KeyMapping(
            "key.laserio.card_holder.open",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            LaserIO.MODID
    );

    public static final KeyMapping TOGGLE_CARD_HOLDER_PULLING = new KeyMapping(
            "key.laserio.card_holder.toggle_pulling",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            LaserIO.MODID
    );

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().player == null) return;

        if (event.getAction() == InputConstants.PRESS) {
            if (OPEN_CARD_HOLDER.isDown()) {
                PacketDistributor.sendToServer(new KeybindPerformActionPayload(KeybindPerformActionPayload.Action.OPEN_CARD_HOLDER));
            } else if (TOGGLE_CARD_HOLDER_PULLING.isDown()) {
                PacketDistributor.sendToServer(new KeybindPerformActionPayload(KeybindPerformActionPayload.Action.TOGGLE_CARD_HOLDER_PULLING));
            }
        }
    }
}