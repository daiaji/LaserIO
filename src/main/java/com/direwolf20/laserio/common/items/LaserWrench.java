package com.direwolf20.laserio.common.items;

import com.direwolf20.laserio.common.blockentities.LaserConnectorAdvBE;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.direwolf20.laserio.common.blocks.baseblocks.BaseLaserBlock;
import com.direwolf20.laserio.setup.Config;
import com.direwolf20.laserio.setup.LaserIODataComponents;
import com.direwolf20.laserio.util.MiscTools;
import com.direwolf20.laserio.util.VectorHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import static com.direwolf20.laserio.util.MiscTools.tooltipMaker;

public class LaserWrench extends Item {
    public static final BlockPos NULL_CONNECTION_POS = new BlockPos(0, -1000, 0);

    public LaserWrench() {
        super(new Item.Properties().stacksTo(1));
    }

    public static GlobalPos storeConnectionPos(ItemStack wrench, Level level, BlockPos pos) {
        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
        wrench.set(LaserIODataComponents.BOUND_GLOBAL_POS, globalPos);
        return globalPos;
    }

    public static GlobalPos getConnectionPos(ItemStack wrench, Level level) {
        if (level == null) return null;
        return wrench.getOrDefault(LaserIODataComponents.BOUND_GLOBAL_POS, GlobalPos.of(level.dimension(), NULL_CONNECTION_POS));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack wrench = player.getItemInHand(hand);
        if (level.isClientSide()) //No client
            return InteractionResultHolder.success(wrench);

        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, ClipContext.Fluid.NONE, Config.MAX_INTERACTION_RANGE.get());
        if (lookingAt == null || !(level.getBlockState(lookingAt.getBlockPos()).getBlock() instanceof BaseLaserBlock)) {
            if (player.isShiftKeyDown()) {
                storeConnectionPos(wrench, level, NULL_CONNECTION_POS);
            }
            return InteractionResultHolder.pass(wrench);
        }
        
        BlockPos targetPos = lookingAt.getBlockPos();
        BlockEntity targetBE = level.getBlockEntity(targetPos);
        if (!(targetBE instanceof BaseLaserBE))
            return InteractionResultHolder.pass(wrench);

        GlobalPos sourceGlobalPos = getConnectionPos(wrench, level);
        Level sourceLevel = MiscTools.getLevel(level.getServer(), sourceGlobalPos);
        
        if (sourceLevel == null) {
             storeConnectionPos(wrench, level, targetPos);
             return InteractionResultHolder.pass(wrench);
        }

        BlockPos sourcePos = sourceGlobalPos.pos();

        if (player.isShiftKeyDown()) {
            // 如果点击的是已经选中的方块，则取消选择
            if (targetPos.equals(sourcePos) && level.equals(sourceLevel)) {
                storeConnectionPos(wrench, level, NULL_CONNECTION_POS);
            } else {
                // 否则，存储新位置
                storeConnectionPos(wrench, level, targetPos);
            }
            return InteractionResultHolder.pass(wrench);
        } else {
            // 避免连接自己
            if (targetPos.equals(sourcePos) && level.equals(sourceLevel)) {
                return InteractionResultHolder.pass(wrench);
            }
            
            BlockEntity sourceBE = sourceLevel.getBlockEntity(sourcePos);
            // 如果源方块无效，清除选择
            if (!(sourceBE instanceof BaseLaserBE)) {
                storeConnectionPos(wrench, level, NULL_CONNECTION_POS);
                return InteractionResultHolder.pass(wrench);
            }
            
            // 高级连接器逻辑
            if (targetBE instanceof LaserConnectorAdvBE targetAdv && sourceBE instanceof LaserConnectorAdvBE sourceAdv) {
                targetAdv.handleAdvancedConnection(sourceAdv);
                return InteractionResultHolder.success(wrench);
            }
            
            // 距离检查
            if (!targetPos.closerThan(sourcePos, Config.MAX_NODES_DISTANCE.get()) || !level.equals(sourceLevel)) {
                player.displayClientMessage(Component.translatable("message.laserio.laser_wrench.exceeded_maximum_connection_range", Config.MAX_NODES_DISTANCE.get()), true);
                return InteractionResultHolder.pass(wrench);
            }
            
            // 建立连接
            ((BaseLaserBE) targetBE).handleConnection((BaseLaserBE) sourceBE);
        }

        return InteractionResultHolder.success(wrench);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        if (!Screen.hasShiftDown()) {
            tooltip.add(tooltipMaker("laserio.tooltip.item.show_details", ChatFormatting.GRAY));
        } else {
            MutableComponent toWrite = tooltipMaker("laserio.tooltip.item.laser_wrench.select_node", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.keys.shift_right_click", ChatFormatting.WHITE));
            tooltip.add(toWrite);
            toWrite = tooltipMaker("laserio.tooltip.item.laser_wrench.connect_node", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.keys.right_click", ChatFormatting.WHITE));
            tooltip.add(toWrite);
            toWrite = tooltipMaker("laserio.tooltip.item.laser_wrench.autoconnect_node", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.laser_wrench.autoconnect_node.keys", ChatFormatting.WHITE));
            tooltip.add(toWrite);
        }
    }
}