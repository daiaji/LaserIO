package com.direwolf20.laserio.datagen;

import com.direwolf20.laserio.common.LaserIO;
import com.direwolf20.laserio.common.items.upgrades.OverclockerCard;
import com.direwolf20.laserio.setup.Registration;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

public class LaserIOItemModels extends ItemModelProvider {
    public LaserIOItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, LaserIO.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        //Block Items
        withExistingParent(Registration.LaserConnector_ITEM.getId().getPath(), modLoc("block/laser_connector"));
        withExistingParent(Registration.LaserNode_ITEM.getId().getPath(), modLoc("block/laser_node"));
        withExistingParent(Registration.LaserConnectorAdv_ITEM.getId().getPath(), modLoc("block/laser_connector_advanced"));

        //Item items
        singleTexture(Registration.Laser_Wrench.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/laser_wrench"));
        singleTexture(Registration.Card_Holder.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/card_holder"));
        //singleTexture(Registration.Card_Item.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/card_item"));
        //singleTexture(Registration.Card_Fluid.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/card_fluid"));
        //singleTexture(Registration.Card_Energy.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/card_energy"));
        singleTexture(Registration.Filter_Basic.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/filter_basic"));
        singleTexture(Registration.Filter_Count.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/filter_count"));
        singleTexture(Registration.Filter_Tag.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/filter_tag"));
        singleTexture(Registration.Filter_Mod.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/filter_mod"));
        singleTexture(Registration.Filter_NBT.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/filter_nbt"));
        singleTexture(Registration.Logic_Chip.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/logic_chip"));
        singleTexture(Registration.Logic_Chip_Raw.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/logic_chip_raw"));
        
        // 逻辑超频卡 (Tier -1)
        singleTexture(Registration.Overclocker_Card.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/overclocker_card"));
        
        singleTexture(Registration.Overclocker_Node.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/overclocker_node"));
        singleTexture(Registration.Card_Cloner.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/card_cloner"));

        // [新增] 动态生成能量超频卡模型
        for (DeferredHolder<Item, OverclockerCard> card : Registration.ENERGY_OVERCLOCKER_CARDS) {
            // 统一使用 energy_overclocker_card 贴图
            singleTexture(card.getId().getPath(), mcLoc("item/generated"), "layer0", modLoc("item/energy_overclocker_card"));
        }
    }
}