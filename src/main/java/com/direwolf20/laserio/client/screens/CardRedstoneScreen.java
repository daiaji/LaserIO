package com.direwolf20.laserio.client.screens;

import com.direwolf20.laserio.client.screens.widgets.ChannelButton;
import com.direwolf20.laserio.client.screens.widgets.NumberButton;
import com.direwolf20.laserio.client.screens.widgets.ToggleButton;
import com.direwolf20.laserio.common.LaserIO;
import com.direwolf20.laserio.common.containers.CardHolderContainer;
import com.direwolf20.laserio.common.containers.CardRedstoneContainer;
import com.direwolf20.laserio.common.containers.customslot.CardHolderSlot;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardRedstone;
import com.direwolf20.laserio.common.network.data.OpenNodePayload;
import com.direwolf20.laserio.common.network.data.UpdateRedstoneCardPayload;
import com.direwolf20.laserio.util.MiscTools;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class CardRedstoneScreen extends AbstractContainerScreen<CardRedstoneContainer> {
    private final ResourceLocation GUI = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/redstonecard.png");
    private static final ResourceLocation CARD_HOLDER_GUI = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/cardholder_node.png");

    protected final CardRedstoneContainer container;
    protected byte currentChannel; // Network Channel
    protected byte currentMode;
    protected byte currentRedstoneChannel; // Signal Logic Channel
    protected boolean currentInterval;
    protected byte currentIntervalLowerBound;
    protected byte currentIntervalUpperBound;
    protected byte currentIntervalOutput;
    protected boolean currentStrong;
    protected byte currentOutputMode;
    protected byte currentLogicOperation;
    protected byte currentLogicOperationChannel;
    protected final ItemStack card;
    protected Map<String, Button> buttons = new HashMap<>();
    private boolean showCardHolderUI;

    public CardRedstoneScreen(CardRedstoneContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.container = container;
        this.card = container.cardItem;
        this.showCardHolderUI = !container.cardHolder.isEmpty();
    }

    public boolean isCardHolderUIShown() {
        return showCardHolderUI;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        toggleHolderSlots();
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        Button modeButton = buttons.get("mode");
        if (MiscTools.inBounds(modeButton.getX(), modeButton.getY(), modeButton.getWidth(), modeButton.getHeight(), mouseX, mouseY)) {
            MutableComponent[] translatableComponents = new MutableComponent[2];
            translatableComponents[0] = Component.translatable("screen.laserio.input");
            translatableComponents[1] = Component.translatable("screen.laserio.output");
            guiGraphics.renderTooltip(font, translatableComponents[currentMode], mouseX, mouseY);
        }

        if (currentMode == 0) { // Input Mode
            Button intervalToggleButton = buttons.get("intervalToggle");
            if (MiscTools.inBounds(intervalToggleButton.getX(), intervalToggleButton.getY(), intervalToggleButton.getWidth(), intervalToggleButton.getHeight(), mouseX, mouseY)) {
                guiGraphics.renderTooltip(font, Component.translatable("screen.laserio.redstone.interval"), mouseX, mouseY);
            }
            if (currentInterval) {
                Button intervalLowerBoundButton = buttons.get("intervalLowerBound");
                if (MiscTools.inBounds(intervalLowerBoundButton.getX(), intervalLowerBoundButton.getY(), intervalLowerBoundButton.getWidth(), intervalLowerBoundButton.getHeight(), mouseX, mouseY)) {
                    guiGraphics.renderTooltip(font, Component.translatable("screen.laserio.redstone.interval.lower_bound"), mouseX, mouseY);
                }
                Button intervalUpperBoundButton = buttons.get("intervalUpperBound");
                if (MiscTools.inBounds(intervalUpperBoundButton.getX(), intervalUpperBoundButton.getY(), intervalUpperBoundButton.getWidth(), intervalUpperBoundButton.getHeight(), mouseX, mouseY)) {
                    guiGraphics.renderTooltip(font, Component.translatable("screen.laserio.redstone.interval.upper_bound"), mouseX, mouseY);
                }
                Button intervalOutputButton = buttons.get("intervalOutput");
                if (MiscTools.inBounds(intervalOutputButton.getX(), intervalOutputButton.getY(), intervalOutputButton.getWidth(), intervalOutputButton.getHeight(), mouseX, mouseY)) {
                    guiGraphics.renderTooltip(font, Component.translatable("screen.laserio.redstone.interval.output"), mouseX, mouseY);
                }
            }
        } else { // Output Mode
            Button strongButton = buttons.get("strong");
            if (MiscTools.inBounds(strongButton.getX(), strongButton.getY(), strongButton.getWidth(), strongButton.getHeight(), mouseX, mouseY)) {
                MutableComponent[] translatableComponents = new MutableComponent[2];
                translatableComponents[0] = Component.translatable("screen.laserio.weak");
                translatableComponents[1] = Component.translatable("screen.laserio.strong");
                guiGraphics.renderTooltip(font, translatableComponents[currentStrong ? 1 : 0], mouseX, mouseY);
            }
            Button outputModeButton = buttons.get("outputMode");
            if (MiscTools.inBounds(outputModeButton.getX(), outputModeButton.getY(), outputModeButton.getWidth(), outputModeButton.getHeight(), mouseX, mouseY)) {
                MutableComponent[] translatableComponents = new MutableComponent[3];
                translatableComponents[0] = Component.translatable("screen.laserio.redstone.output_mode.normal");
                translatableComponents[1] = Component.translatable("screen.laserio.redstone.output_mode.complementary");
                translatableComponents[2] = Component.translatable("screen.laserio.redstone.output_mode.not");
                guiGraphics.renderTooltip(font, translatableComponents[currentOutputMode], mouseX, mouseY);
            }
            Button logicOperationButton = buttons.get("logicOperation");
            if (MiscTools.inBounds(logicOperationButton.getX(), logicOperationButton.getY(), logicOperationButton.getWidth(), logicOperationButton.getHeight(), mouseX, mouseY)) {
                MutableComponent[] translatableComponents = new MutableComponent[4];
                translatableComponents[0] = Component.translatable("screen.laserio.redstone.logic_operation.none");
                translatableComponents[1] = Component.translatable("screen.laserio.redstone.logic_operation.or");
                translatableComponents[2] = Component.translatable("screen.laserio.redstone.logic_operation.and");
                translatableComponents[3] = Component.translatable("screen.laserio.redstone.logic_operation.xor");
                guiGraphics.renderTooltip(font, translatableComponents[currentLogicOperation], mouseX, mouseY);
            }
            if (currentLogicOperation != 0) {
                Button logicOperationChannelButton = buttons.get("logicOperationChannel");
                if (MiscTools.inBounds(logicOperationChannelButton.getX(), logicOperationChannelButton.getY(), logicOperationChannelButton.getWidth(), logicOperationChannelButton.getHeight(), mouseX, mouseY)) {
                    guiGraphics.renderTooltip(font, Component.translatable("screen.laserio.redstonechannel").append(String.valueOf(currentLogicOperationChannel)), mouseX, mouseY);
                }
            }
        }

        // Render Network Channel Tip
        Button channelButton = buttons.get("channel");
        if (MiscTools.inBounds(channelButton.getX(), channelButton.getY(), channelButton.getWidth(), channelButton.getHeight(), mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("screen.laserio.channel").append(String.valueOf(currentChannel)), mouseX, mouseY);
        }
        // Render Redstone Logic Channel Tip
        Button redstoneChannelButton = buttons.get("redstoneChannel");
        if (MiscTools.inBounds(redstoneChannelButton.getX(), redstoneChannelButton.getY(), redstoneChannelButton.getWidth(), redstoneChannelButton.getHeight(), mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("screen.laserio.redstonechannel").append(String.valueOf(currentRedstoneChannel)), mouseX, mouseY);
        }
    }

    public void addModeButton() {
        ResourceLocation[] modeTextures = new ResourceLocation[2];
        modeTextures[0] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstoneinput.png");
        modeTextures[1] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstoneoutput.png");
        buttons.put("mode", new ToggleButton(getGuiLeft() + 5, getGuiTop() + 5, 16, 16, modeTextures, currentMode, (button) -> {
            currentMode = CardRedstone.nextTransferMode(card);
            ((ToggleButton) button).setTexturePosition(currentMode);
            modeChange();
        }));
    }

    public void addChannelButton() {
        // Network Channel (Bottom Left)
        buttons.put("channel", new ChannelButton(getGuiLeft() + 5, getGuiTop() + 65, 16, 16, currentChannel, (button) -> {
            currentChannel = BaseCard.nextChannel(card);
            ((ChannelButton) button).setChannel(currentChannel);
        }));
    }

    public void addRedstoneChannelButton() {
        // Redstone Logic Channel (Top Right area)
        buttons.put("redstoneChannel", new ChannelButton(getGuiLeft() + 105, getGuiTop() + 5, 16, 16, currentRedstoneChannel, (button) -> {
            currentRedstoneChannel = CardRedstone.nextRedstoneChannel(card);
            ((ChannelButton) button).setChannel(currentRedstoneChannel);
        }));
    }

    public void addIntervalToggleButton() {
        ResourceLocation[] intervalTextures = new ResourceLocation[2];
        intervalTextures[0] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstoneintervalfalse.png");
        intervalTextures[1] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstoneintervaltrue.png");
        buttons.put("intervalToggle", new ToggleButton(getGuiLeft() + 5, getGuiTop() + 25, 16, 16, intervalTextures, currentInterval ? 1 : 0, (button) -> {
            currentInterval = !currentInterval;
            ((ToggleButton) button).setTexturePosition(currentInterval ? 1 : 0);
            intervalChange();
        }));
    }

    public void addIntervalLowerBoundButton() {
        buttons.put("intervalLowerBound", new NumberButton(getGuiLeft() + 25, getGuiTop() + 25, 16, 16, currentIntervalLowerBound, (button) -> {
            changeIntervalLowerBound(-1);
        }));
    }

    public void addIntervalUpperBoundButton() {
        buttons.put("intervalUpperBound", new NumberButton(getGuiLeft() + 45, getGuiTop() + 25, 16, 16, currentIntervalUpperBound, (button) -> {
            changeIntervalUpperBound(-1);
        }));
    }

    public void addIntervalOutputButton() {
        buttons.put("intervalOutput", new NumberButton(getGuiLeft() + 65, getGuiTop() + 25, 16, 16, currentIntervalOutput, (button) -> {
            changeIntervalOutput(-1);
        }));
    }

    public void addStrongButton() {
        ResourceLocation[] strongTextures = new ResourceLocation[2];
        strongTextures[0] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstonelow.png");
        strongTextures[1] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstonehigh.png");
        buttons.put("strong", new ToggleButton(getGuiLeft() + 5, getGuiTop() + 25, 16, 16, strongTextures, currentStrong ? 1 : 0, (button) -> {
            currentStrong = !currentStrong;
            ((ToggleButton) button).setTexturePosition(currentStrong ? 1 : 0);
        }));
    }

    public void addOutputModeButton() {
        ResourceLocation[] outputModeTextures = new ResourceLocation[3];
        outputModeTextures[0] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstonenormal.png");
        outputModeTextures[1] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstonecomplementary.png");
        outputModeTextures[2] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstonenot.png");
        buttons.put("outputMode", new ToggleButton(getGuiLeft() + 155, getGuiTop() + 5, 16, 16, outputModeTextures, currentOutputMode, (button) -> {
            currentOutputMode = (byte) (currentOutputMode == 2 ? 0 : currentOutputMode + 1);
            ((ToggleButton) button).setTexturePosition(currentOutputMode);
        }));
    }

    public void addLogicOperationButton() {
        ResourceLocation[] logicOperationTextures = new ResourceLocation[4];
        logicOperationTextures[0] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstonenologicoperation.png");
        logicOperationTextures[1] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstoneor.png");
        logicOperationTextures[2] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstoneand.png");
        logicOperationTextures[3] = ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "textures/gui/buttons/redstonexor.png");
        buttons.put("logicOperation", new ToggleButton(getGuiLeft() + 155, getGuiTop() + 25, 16, 16, logicOperationTextures, currentLogicOperation, (button) -> {
            currentLogicOperation = (byte) (currentLogicOperation == 3 ? 0 : currentLogicOperation + 1);
            ((ToggleButton) button).setTexturePosition(currentLogicOperation);
            logicOperationChange();
        }));
    }

    public void addLogicOperationChannelButton() {
        buttons.put("logicOperationChannel", new ChannelButton(getGuiLeft() + 135, getGuiTop() + 25, 16, 16, currentLogicOperationChannel, (button) -> {
            currentLogicOperationChannel = CardRedstone.nextRedstoneChannelOperation(card);
            ((ChannelButton) button).setChannel(currentLogicOperationChannel);
        }));
    }

    @Override
    public void init() {
        super.init();
        currentMode = CardRedstone.getTransferMode(card);
        currentChannel = BaseCard.getChannel(card);
        currentRedstoneChannel = CardRedstone.getRedstoneChannel(card);
        currentInterval = CardRedstone.getInterval(card);
        currentIntervalLowerBound = CardRedstone.getIntervalLowerBound(card);
        currentIntervalUpperBound = CardRedstone.getIntervalUpperBound(card);
        currentIntervalOutput = CardRedstone.getIntervalOutput(card);
        currentStrong = CardRedstone.getStrong(card);
        currentOutputMode = CardRedstone.getOutputMode(card);
        currentLogicOperation = CardRedstone.getLogicOperation(card);
        currentLogicOperationChannel = CardRedstone.getRedstoneChannelOperation(card);

        addModeButton();
        addChannelButton();
        addRedstoneChannelButton();
        addIntervalToggleButton();
        addIntervalLowerBoundButton();
        addIntervalUpperBoundButton();
        addIntervalOutputButton();
        addStrongButton();
        addOutputModeButton();
        addLogicOperationButton();
        addLogicOperationChannelButton();

        if (container.direction != -1) {
            buttons.put("return", new ExtendedButton(getGuiLeft() - 25, getGuiTop() + 1, 25, 20, Component.literal("<--"), (button) -> {
                openNode();
            }));
        }

        for (Map.Entry<String, Button> button : buttons.entrySet()) {
            addRenderableWidget(button.getValue());
        }

        modeChange();
    }

    public void modeChange() {
        Button strongButton = buttons.get("strong");
        Button outputModeButton = buttons.get("outputMode");
        Button intervalToggleButton = buttons.get("intervalToggle");
        Button intervalLowerBoundButton = buttons.get("intervalLowerBound");
        Button intervalUpperBoundButton = buttons.get("intervalUpperBound");
        Button intervalOutputButton = buttons.get("intervalOutput");
        Button logicOperationButton = buttons.get("logicOperation");
        Button logicOperationChannelButton = buttons.get("logicOperationChannel");

        if (currentMode == 0) { // Input
            if (!renderables.contains(intervalToggleButton))
                addRenderableWidget(intervalToggleButton);
            removeWidget(strongButton);
            removeWidget(outputModeButton);
            removeWidget(logicOperationButton);
            removeWidget(logicOperationChannelButton);
            intervalChange();
        } else { // Output
            if (!renderables.contains(strongButton))
                addRenderableWidget(strongButton);
            if (!renderables.contains(outputModeButton))
                addRenderableWidget(outputModeButton);
            if (!renderables.contains(logicOperationButton))
                addRenderableWidget(logicOperationButton);
            if (!renderables.contains(logicOperationChannelButton))
                addRenderableWidget(logicOperationChannelButton);
            removeWidget(intervalToggleButton);
            removeWidget(intervalLowerBoundButton);
            removeWidget(intervalUpperBoundButton);
            removeWidget(intervalOutputButton);
            logicOperationChange();
        }
    }

    public void intervalChange() {
        Button intervalLowerBoundButton = buttons.get("intervalLowerBound");
        Button intervalUpperBoundButton = buttons.get("intervalUpperBound");
        Button intervalOutputButton = buttons.get("intervalOutput");
        if (currentInterval) {
            if (!renderables.contains(intervalLowerBoundButton))
                addRenderableWidget(intervalLowerBoundButton);
            if (!renderables.contains(intervalUpperBoundButton))
                addRenderableWidget(intervalUpperBoundButton);
            if (!renderables.contains(intervalOutputButton))
                addRenderableWidget(intervalOutputButton);
        } else {
            removeWidget(intervalLowerBoundButton);
            removeWidget(intervalUpperBoundButton);
            removeWidget(intervalOutputButton);
        }
    }

    public void logicOperationChange() {
        Button logicOperationChannelButton = buttons.get("logicOperationChannel");
        if (currentLogicOperation != 0) {
            if (!renderables.contains(logicOperationChannelButton))
                addRenderableWidget(logicOperationChannelButton);
        } else {
            removeWidget(logicOperationChannelButton);
        }
    }

    public void changeIntervalLowerBound(int change) {
        if (Screen.hasShiftDown()) change *= 15;
        if (change < 0) {
            currentIntervalLowerBound = (byte) (Math.max(currentIntervalLowerBound + change, 0));
        } else {
            currentIntervalLowerBound = (byte) (Math.min(currentIntervalLowerBound + change, currentIntervalUpperBound));
        }
    }

    public void changeIntervalUpperBound(int change) {
        if (Screen.hasShiftDown()) change *= 15;
        if (change < 0) {
            currentIntervalUpperBound = (byte) (Math.max(currentIntervalUpperBound + change, currentIntervalLowerBound));
        } else {
            currentIntervalUpperBound = (byte) (Math.min(currentIntervalUpperBound + change, 15));
        }
    }

    public void changeIntervalOutput(int change) {
        if (Screen.hasShiftDown()) change *= 15;
        if (change < 0) {
            currentIntervalOutput = (byte) (Math.max(currentIntervalOutput + change, 0));
        } else {
            currentIntervalOutput = (byte) (Math.min(currentIntervalOutput + change, 15));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI, relX, relY, 0, 0, this.imageWidth, this.imageHeight);

        if (showCardHolderUI) {
            RenderSystem.setShaderTexture(0, CARD_HOLDER_GUI);
            guiGraphics.blit(CARD_HOLDER_GUI, getGuiLeft() - 100, getGuiTop() + 24, 0, 0, this.imageWidth, this.imageHeight);
        }
    }

    public void toggleHolderSlots() {
        int startIndex = CardRedstoneContainer.SLOTS;
        int endIndex = startIndex + CardHolderContainer.SLOTS;
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= container.slots.size()) continue;
            Slot slot = container.getSlot(i);
            if (slot instanceof CardHolderSlot cardHolderSlot) {
                cardHolderSlot.setEnabled(showCardHolderUI);
            }
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        if (showCardHolderUI)
            return mouseX < (double) guiLeftIn - 100 || mouseY < (double) guiTopIn || mouseX >= (double) (guiLeftIn + this.imageWidth) || mouseY >= (double) (guiTopIn + this.imageHeight);
        return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        saveSettings();
        super.onClose();
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        InputConstants.Key mouseKey = InputConstants.getKey(p_keyPressed_1_, p_keyPressed_2_);
        if (p_keyPressed_1_ == 256 || minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
            onClose();
            return true;
        }
        return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }

    public boolean mouseReleased(double p_mouseReleased_1_, double p_mouseReleased_3_, int p_mouseReleased_5_) {
        return super.mouseReleased(p_mouseReleased_1_, p_mouseReleased_3_, p_mouseReleased_5_);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta, double deltaY) {
        return super.mouseScrolled(mouseX, mouseY, delta, deltaY);
    }

    public void saveSettings() {
        PacketDistributor.sendToServer(new UpdateRedstoneCardPayload(
                currentMode,
                currentChannel,
                currentRedstoneChannel,
                currentInterval,
                currentIntervalLowerBound,
                currentIntervalUpperBound,
                currentIntervalOutput,
                currentStrong,
                currentOutputMode,
                currentLogicOperation,
                currentLogicOperationChannel
        ));
    }

    public void openNode() {
        saveSettings();
        PacketDistributor.sendToServer(new OpenNodePayload(container.sourceContainer, container.direction));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void setIntervalLowerBound(NumberButton button, int btn) {
        if (btn == 0)
            changeIntervalLowerBound(1);
        else if (btn == 1)
            changeIntervalLowerBound(-1);
        button.setValue(currentIntervalLowerBound);
        button.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    public void setIntervalUpperBound(NumberButton button, int btn) {
        if (btn == 0)
            changeIntervalUpperBound(1);
        else if (btn == 1)
            changeIntervalUpperBound(-1);
        button.setValue(currentIntervalUpperBound);
        button.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    public void setIntervalOutput(NumberButton button, int btn) {
        if (btn == 0)
            changeIntervalOutput(1);
        else if (btn == 1)
            changeIntervalOutput(-1);
        button.setValue(currentIntervalOutput);
        button.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    @Override
    public boolean mouseClicked(double x, double y, int btn) {
        // Network Channel
        ChannelButton channelButton = ((ChannelButton) buttons.get("channel"));
        if (MiscTools.inBounds(channelButton.getX(), channelButton.getY(), channelButton.getWidth(), channelButton.getHeight(), x, y)) {
            if (btn == 0)
                currentChannel = BaseCard.nextChannel(card);
            else if (btn == 1)
                currentChannel = BaseCard.previousChannel(card);
            channelButton.setChannel(currentChannel);
            channelButton.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        // Redstone Logic Channel
        ChannelButton redstoneChannelButton = ((ChannelButton) buttons.get("redstoneChannel"));
        if (MiscTools.inBounds(redstoneChannelButton.getX(), redstoneChannelButton.getY(), redstoneChannelButton.getWidth(), redstoneChannelButton.getHeight(), x, y)) {
            if (btn == 0)
                currentRedstoneChannel = CardRedstone.nextRedstoneChannel(card);
            else if (btn == 1)
                currentRedstoneChannel = CardRedstone.previousRedstoneChannel(card);
            redstoneChannelButton.setChannel(currentRedstoneChannel);
            redstoneChannelButton.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        // Interval Buttons
        NumberButton intervalLowerBoundButton = ((NumberButton) buttons.get("intervalLowerBound"));
        if (currentMode == 0 && currentInterval && MiscTools.inBounds(intervalLowerBoundButton.getX(), intervalLowerBoundButton.getY(), intervalLowerBoundButton.getWidth(), intervalLowerBoundButton.getHeight(), x, y)) {
            setIntervalLowerBound(intervalLowerBoundButton, btn);
            return true;
        }
        NumberButton intervalUpperBoundButton = ((NumberButton) buttons.get("intervalUpperBound"));
        if (currentMode == 0 && currentInterval && MiscTools.inBounds(intervalUpperBoundButton.getX(), intervalUpperBoundButton.getY(), intervalUpperBoundButton.getWidth(), intervalUpperBoundButton.getHeight(), x, y)) {
            setIntervalUpperBound(intervalUpperBoundButton, btn);
            return true;
        }
        NumberButton intervalOutputButton = ((NumberButton) buttons.get("intervalOutput"));
        if (currentMode == 0 && currentInterval && MiscTools.inBounds(intervalOutputButton.getX(), intervalOutputButton.getY(), intervalOutputButton.getWidth(), intervalOutputButton.getHeight(), x, y)) {
            setIntervalOutput(intervalOutputButton, btn);
            return true;
        }
        
        // Logic Op Channel
        ChannelButton logicOperationChannelButton = ((ChannelButton) buttons.get("logicOperationChannel"));
        if (currentMode == 1 && currentLogicOperation != 0 && MiscTools.inBounds(logicOperationChannelButton.getX(), logicOperationChannelButton.getY(), logicOperationChannelButton.getWidth(), logicOperationChannelButton.getHeight(), x, y)) {
            if (btn == 0)
                currentLogicOperationChannel = CardRedstone.nextRedstoneChannelOperation(card);
            else if (btn == 1)
                currentLogicOperationChannel = CardRedstone.previousRedstoneChannelOperation(card);
            logicOperationChannelButton.setChannel(currentLogicOperationChannel);
            logicOperationChannelButton.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        
        return super.mouseClicked(x, y, btn);
    }
}