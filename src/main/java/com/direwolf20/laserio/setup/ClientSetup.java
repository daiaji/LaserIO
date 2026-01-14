package com.direwolf20.laserio.setup;

import com.direwolf20.laserio.client.blockentityrenders.LaserConnectorBERender;
import com.direwolf20.laserio.client.blockentityrenders.LaserNodeBERender;
import com.direwolf20.laserio.client.events.ClientEvents;
import com.direwolf20.laserio.client.events.EventTooltip;
import com.direwolf20.laserio.client.events.KeybindHandler;
import com.direwolf20.laserio.client.screens.*;
import com.direwolf20.laserio.common.LaserIO;
import com.direwolf20.laserio.common.blockentities.LaserConnectorAdvBE;
import com.direwolf20.laserio.common.blockentities.LaserConnectorBE;
import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardRedstone;
import com.direwolf20.laserio.integration.mekanism.client.screens.CardChemicalScreen; // [修复] 修正导入路径
import com.direwolf20.laserio.integration.mekanism.common.items.cards.CardChemical;
import com.direwolf20.laserio.integration.mekanism.MekanismIntegration;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.awt.Color;

@EventBusSubscriber(modid = LaserIO.MODID, value = Dist.CLIENT)
public class ClientSetup {
    
    @SubscribeEvent
    public static void init(final FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(ClientEvents.class);
        NeoForge.EVENT_BUS.register(KeybindHandler.class);

        // Item Properties
        event.enqueueWork(() -> {
            ItemProperties.register(Registration.Card_Item.get(),
                    ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "mode"), (stack, level, living, id) -> {
                        return (float) BaseCard.getTransferMode(stack);
                    });
            ItemProperties.register(Registration.Card_Fluid.get(),
                    ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "mode"), (stack, level, living, id) -> {
                        return (float) BaseCard.getTransferMode(stack);
                    });
            ItemProperties.register(Registration.Card_Energy.get(),
                    ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "mode"), (stack, level, living, id) -> {
                        return (float) BaseCard.getTransferMode(stack);
                    });
            ItemProperties.register(Registration.Card_Redstone.get(),
                    ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "mode"), (stack, level, living, id) -> {
                        return (float) CardRedstone.getTransferMode(stack);
                    });
            
            if (MekanismIntegration.isLoaded()) {
                ItemProperties.register(Registration.Card_Chemical.get(),
                        ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "mode"), (stack, level, living, id) -> {
                            return (float) CardChemical.getTransferMode(stack);
                        });
            }
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.LaserNode_Container.get(), LaserNodeScreen::new);
        event.register(Registration.CardItem_Container.get(), CardItemScreen::new);
        event.register(Registration.CardFluid_Container.get(), CardFluidScreen::new);
        event.register(Registration.CardEnergy_Container.get(), CardEnergyScreen::new);
        event.register(Registration.CardRedstone_Container.get(), CardRedstoneScreen::new);
        if (MekanismIntegration.isLoaded()) {
            event.register(Registration.CardChemical_Container.get(), CardChemicalScreen::new);
        }
        event.register(Registration.CardHolder_Container.get(), CardHolderScreen::new);
        event.register(Registration.FilterBasic_Container.get(), FilterBasicScreen::new);
        event.register(Registration.FilterCount_Container.get(), FilterCountScreen::new);
        event.register(Registration.FilterTag_Container.get(), FilterTagScreen::new);
        event.register(Registration.FilterNBT_Container.get(), FilterNBTScreen::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeybindHandler.OPEN_CARD_HOLDER);
        event.register(KeybindHandler.TOGGLE_CARD_HOLDER_PULLING);
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.LaserConnector_BE.get(), LaserConnectorBERender::new);
        event.registerBlockEntityRenderer(Registration.LaserNode_BE.get(), LaserNodeBERender::new);
        
        event.registerBlockEntityRenderer(Registration.LaserConnectorAdv_BE.get(), 
            (BlockEntityRendererProvider<LaserConnectorAdvBE>) (Object) (BlockEntityRendererProvider<LaserConnectorBE>) LaserConnectorBERender::new);
    }

    @SubscribeEvent
    public static void registerTooltipFactory(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(EventTooltip.CopyPasteTooltipComponent.Data.class, EventTooltip.CopyPasteTooltipComponent::new);
    }

    @SubscribeEvent
    static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, index) -> {
            if (index == 2) {
                if (BaseCard.getTransferMode(stack) == (byte) 3) {
                    Color color = LaserNodeBERender.colors[BaseCard.getRedstoneChannel(stack)];
                    return color.getRGB();
                } else {
                    Color color = LaserNodeBERender.colors[BaseCard.getChannel(stack)];
                    return color.getRGB();
                }
            }
            return 0xFFFFFFFF;
        }, Registration.Card_Item.get());

        event.register((stack, index) -> {
            if (index == 2) {
                if (BaseCard.getTransferMode(stack) == (byte) 3) {
                    Color color = LaserNodeBERender.colors[BaseCard.getRedstoneChannel(stack)];
                    return color.getRGB();
                } else {
                    Color color = LaserNodeBERender.colors[BaseCard.getChannel(stack)];
                    return color.getRGB();
                }
            }
            return 0xFFFFFFFF;
        }, Registration.Card_Fluid.get());

        if (MekanismIntegration.isLoaded()) {
            event.register((stack, index) -> {
                if (index == 2) {
                    if (BaseCard.getTransferMode(stack) == (byte) 3) {
                        Color color = LaserNodeBERender.colors[BaseCard.getRedstoneChannel(stack)];
                        return color.getRGB();
                    } else {
                        Color color = LaserNodeBERender.colors[BaseCard.getChannel(stack)];
                        return color.getRGB();
                    }
                }
                return 0xFFFFFFFF;
            }, Registration.Card_Chemical.get());
        }

        event.register((stack, index) -> {
            if (index == 2) {
                if (BaseCard.getTransferMode(stack) == (byte) 3) {
                    Color color = LaserNodeBERender.colors[BaseCard.getRedstoneChannel(stack)];
                    return color.getRGB();
                } else {
                    Color color = LaserNodeBERender.colors[BaseCard.getChannel(stack)];
                    return color.getRGB();
                }
            }
            return 0xFFFFFFFF;
        }, Registration.Card_Energy.get());

        event.register((stack, index) -> {
            if (index == 2) {
                Color color = LaserNodeBERender.colors[CardRedstone.getRedstoneChannel(stack)];
                return color.getRGB();
            }
            return 0xFFFFFFFF;
        }, Registration.Card_Redstone.get());

        event.register((stack, index) -> {
            if (index == 1) return new Color(255, 0, 0, 255).getRGB();
            return 0xFFFFFFFF;
        }, Registration.LaserNode_ITEM.get());

        event.register((stack, index) -> {
            if (index == 1) return new Color(255, 0, 0, 255).getRGB();
            return 0xFFFFFFFF;
        }, Registration.LaserConnector_ITEM.get());

        event.register((stack, index) -> {
            if (index == 1) return new Color(255, 0, 0, 255).getRGB();
            return 0xFFFFFFFF;
        }, Registration.LaserConnectorAdv_ITEM.get());
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, env, pos, index) -> {
                    if (index == 0 && env != null && pos != null) {
                        if (env.getBlockEntity(pos) instanceof LaserNodeBE laserNodeBE) {
                            Color color = laserNodeBE.getColor();
                            return FastColor.ARGB32.color(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
                        }
                    }
                    return FastColor.ARGB32.color(255, 255, 0, 0);
                },
                Registration.LaserNode.get()
        );
        event.register(
                (state, env, pos, index) -> {
                    if (index == 0 && env != null && pos != null) {
                        if (env.getBlockEntity(pos) instanceof LaserConnectorBE laserConnectorBE) {
                            Color color = laserConnectorBE.getColor();
                            return FastColor.ARGB32.color(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
                        }
                    }
                    return FastColor.ARGB32.color(255, 255, 0, 0);
                },
                Registration.LaserConnector.get()
        );
        event.register(
                (state, env, pos, index) -> {
                    if (index == 0 && env != null && pos != null) {
                        if (env.getBlockEntity(pos) instanceof LaserConnectorAdvBE laserConnectorAdvBE) {
                            Color color = laserConnectorAdvBE.getColor();
                            return FastColor.ARGB32.color(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
                        }
                    }
                    return FastColor.ARGB32.color(255, 255, 0, 0);
                },
                Registration.LaserConnectorAdv.get()
        );
    }
}