package com.direwolf20.laserio.client.renderer;

import com.direwolf20.laserio.common.LaserIO;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.OptionalDouble;

public class MyRenderType extends RenderType {
    // [现代化] 使用 ResourceLocation.parse 适配 1.21+
    private final static ResourceLocation laserBeam = ResourceLocation.parse(LaserIO.MODID + ":textures/misc/laser.png");
    private final static ResourceLocation laserBeam2 = ResourceLocation.parse(LaserIO.MODID + ":textures/misc/laser2.png");
    private final static ResourceLocation laserBeamGlow = ResourceLocation.parse(LaserIO.MODID + ":textures/misc/laser_glow.png");

    public MyRenderType(String name, VertexFormat format, VertexFormat.Mode p_i225992_3_, int p_i225992_4_, boolean p_i225992_5_, boolean p_i225992_6_, Runnable runnablePre, Runnable runnablePost) {
        super(name, format, p_i225992_3_, p_i225992_4_, p_i225992_5_, p_i225992_6_, runnablePre, runnablePost);
    }

    private static final LineStateShard THICK_LINES = new LineStateShard(OptionalDouble.of(3.0D));

    public static void updateRenders() {
        // 动态更新逻辑保留为空，目前不需要
    }

    // 核心渲染类型定义：格式为 POSITION_COLOR_TEX_LIGHTMAP
    // 注意：此格式不包含 OVERLAY 元素，RenderUtils 中必须严禁调用 .setOverlay()
    public static final RenderType LASER_MAIN_BEAM = create("MiningLaserMainBeam",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder().setTextureState(new TextureStateShard(laserBeam2, false, false))
                    .setShaderState(ShaderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setLayeringState(NO_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    public static final RenderType LASER_MAIN_CORE = create("MiningLaserCoreBeam",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder().setTextureState(new TextureStateShard(laserBeam, false, false))
                    .setShaderState(ShaderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    public static final RenderType CONNECTING_LASER = create("ConnectingLaser",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder().setTextureState(new TextureStateShard(laserBeam, false, false))
                    .setShaderState(ShaderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    public static final RenderType BlockOverlay = create("MiningLaserBlockOverlay",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(ShaderStateShard.POSITION_COLOR_SHADER)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(NO_TEXTURE)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));
}