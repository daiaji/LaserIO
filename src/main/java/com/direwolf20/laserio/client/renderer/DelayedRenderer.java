package com.direwolf20.laserio.client.renderer;

import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class DelayedRenderer {
    // 使用 LinkedList 作为队列实现
    private static final Queue<BaseLaserBE> beRenders = new LinkedList<>();
    private static final Set<LaserNodeBE> beConnectingRenders = new HashSet<>();

    public static void render(PoseStack matrixStackIn) {
        if (beRenders.size() > 0) {
            // [修复] 调用新的 drawLasers 方法 (原 drawLasersLast2)
            RenderUtils.drawLasers(beRenders, matrixStackIn);
        }
    }

    public static void renderConnections(PoseStack matrixStackIn) {
        if (beConnectingRenders.isEmpty()) return;
        // [修复] 调用新的 drawConnectingLasers 方法 (原 drawConnectingLasersLast4)
        RenderUtils.drawConnectingLasers(beConnectingRenders, matrixStackIn);
        beConnectingRenders.clear();
    }

    public static void add(BaseLaserBE be) {
        beRenders.add(be);
    }

    public static void addConnecting(LaserNodeBE be) {
        beConnectingRenders.add(be);
    }
}