package com.direwolf20.laserio.common.blocks.baseblocks;

import com.direwolf20.laserio.common.blockentities.LaserConnectorAdvBE;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.direwolf20.laserio.common.items.LaserWrench;
import com.direwolf20.laserio.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class BaseLaserBlock extends Block {
    public BaseLaserBlock() {
        super(Properties.of()
                .sound(SoundType.METAL)
                .strength(2.0f)
                .noOcclusion()
                .forceSolidOn()
        );
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        // 我们检查实体是否为 ServerPlayer，以避免假玩家触发此逻辑
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        ItemStack heldItemOffhand = player.getOffhandItem();
        if (!(heldItemOffhand.getItem() instanceof LaserWrench)) {
            return;
        }

        // 获取目标方块（刚放置的）
        BlockEntity targetBE = level.getBlockEntity(pos);
        if (!(targetBE instanceof BaseLaserBE)) {
            return;
        }

        // 获取源方块位置（扳手中存储的）
        GlobalPos sourceGlobalPos = LaserWrench.getConnectionPos(heldItemOffhand, level);
        if (sourceGlobalPos == null) return;
        
        Level sourceLevel = level.getServer().getLevel(sourceGlobalPos.dimension());
        if (sourceLevel == null) return;
        
        BlockPos sourcePos = sourceGlobalPos.pos();
        BlockEntity sourceBE = sourceLevel.getBlockEntity(sourcePos);

        if (sourceBE instanceof BaseLaserBE) {
            // 逻辑：如果是两个高级连接器，允许跨维度/无限距离连接
            if (targetBE instanceof LaserConnectorAdvBE targetAdv && sourceBE instanceof LaserConnectorAdvBE sourceAdv) {
                targetAdv.handleAdvancedConnection(sourceAdv);
            } 
            // 逻辑：普通连接，检查距离和维度
            else if (!pos.closerThan(sourcePos, Config.MAX_NODES_DISTANCE.get()) || !level.dimension().equals(sourceGlobalPos.dimension())) {
                player.displayClientMessage(Component.translatable("message.laserio.laser_wrench.exceeded_maximum_connection_range", Config.MAX_NODES_DISTANCE.get()), true);
            } 
            // 逻辑：建立连接
            else {
                ((BaseLaserBE) targetBE).addConnection(sourcePos, (BaseLaserBE) sourceBE);
            }
        }

        // 更新扳手存储的坐标为当前放置的方块，以便链式连接
        LaserWrench.storeConnectionPos(heldItemOffhand, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (newState.getBlock() != this) {
            BlockEntity be = worldIn.getBlockEntity(pos);
            if (be instanceof BaseLaserBE baseLaserBE) {
                baseLaserBE.disconnectAllNodes();
            }
        }
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }
}