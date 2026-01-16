package com.direwolf20.laserio.setup;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
    public static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    public static final String CATEGORY_CARD = "card";
    public static final String SUBCATEGORY_FLUID = "fluid_card";
    public static final String SUBCATEGORY_ENERGY = "energy_card";
    public static final String SUBCATEGORY_CHEMICAL = "chemical_card";

    // [配置] 这里定义能量卡的默认层级值，与流体类似逻辑
    public static final List<Integer> DEFAULT_TIER_VALUES = List.of(4000, 16000, 128000, Integer.MAX_VALUE);

    public static final ModConfigSpec COMMON_CONFIG;
    public static final ModConfigSpec CLIENT_CONFIG;
    public static final ModConfigSpec SERVER_CONFIG;

    public static final ModConfigSpec.IntValue MAX_NODES_DISTANCE;
    public static final ModConfigSpec.IntValue MAX_INTERACTION_RANGE;

    // --- Fluid ---
    public static final ModConfigSpec.IntValue BASE_MILLI_BUCKETS_FLUID;
    public static final ModConfigSpec.IntValue MULTIPLIER_MILLI_BUCKETS_FLUID;
    public static final ModConfigSpec.BooleanValue USE_FLUID_TIERS_MODE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MAX_FLUID_TIERS;

    // --- Energy ---
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MAX_FE_TIERS;
    public static final ModConfigSpec.IntValue MAX_FE_NO_TIERS;
    public static final ModConfigSpec.IntValue MAX_FE_TICK;

    // --- Chemical ---
    public static final ModConfigSpec.IntValue BASE_MILLI_BUCKETS_CHEMICAL;
    public static final ModConfigSpec.IntValue MULTIPLIER_MILLI_BUCKETS_CHEMICAL;
    public static final ModConfigSpec.BooleanValue USE_CHEMICAL_TIERS_MODE; // [新增]
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MAX_CHEMICAL_TIERS; // [新增]

    // --- Ticks ---
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MIN_TICKS_FLUID;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MIN_TICKS_ITEM;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MIN_TICKS_CHEMICAL;
    public static final ModConfigSpec.IntValue MIN_TICKS_ENERGY;

    static {
        COMMON_BUILDER.comment("General settings").push("general");
        MAX_NODES_DISTANCE = COMMON_BUILDER.comment("Maximum distance in blocks between two Laser Nodes (Default: 8)")
                .defineInRange("max_nodes_distance", 8, 1, 64);
        MAX_INTERACTION_RANGE = COMMON_BUILDER.comment("Maximum distance in blocks for Laser Wrench interaction (Default: 10)")
                .defineInRange("max_interaction_range", 10, 1, 64);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Card settings").push(CATEGORY_CARD);

        // Fluid Configs
        COMMON_BUILDER.comment("Fluid Card").push(SUBCATEGORY_FLUID);
        BASE_MILLI_BUCKETS_FLUID = COMMON_BUILDER.comment("Millibuckets for Fluid Cards without Overclockers installed (Linear Mode & Tier Mode Base)")
                .defineInRange("base_milli_buckets_fluid", 5000, 0, Integer.MAX_VALUE);
        MULTIPLIER_MILLI_BUCKETS_FLUID = COMMON_BUILDER.comment("Multiplier for Overclocker Cards (Linear Mode Only) - Formula: Max(Count * Multiplier, Base)")
                .defineInRange("multiplier_milli_buckets_fluid", 10000, 0, Integer.MAX_VALUE);

        // [修改] 默认开启层级模式，以支持你想要的 int 极限
        USE_FLUID_TIERS_MODE = COMMON_BUILDER.comment("If true, use the 'max_fluid_tiers' list instead of the linear multiplier formula. Required for MAX_INT fluid transfer.")
                .define("use_fluid_tiers_mode", true);

        // [修改] 推荐数值：32B -> 128B -> 512B -> 无限
        MAX_FLUID_TIERS = COMMON_BUILDER.comment("Maximum Fluid extraction (mB) per tick based on overclockers count (1-4). Only used if 'use_fluid_tiers_mode' is true.")
                .defineList("max_fluid_tiers",
                        List.of(32000, 128000, 512000, Integer.MAX_VALUE),
                        () -> 0,
                        o -> o instanceof Integer);

        MIN_TICKS_FLUID = COMMON_BUILDER.comment("Minimum ticks between fluid extractions based on overclockers count (0-4)")
                .defineList("min_ticks_fluid",
                        List.of(20, 15, 10, 5, 1),
                        () -> 20,
                        o -> o instanceof Integer);
        COMMON_BUILDER.pop();

        // Energy Configs
        COMMON_BUILDER.comment("Energy Card").push(SUBCATEGORY_ENERGY);

        MAX_FE_NO_TIERS = COMMON_BUILDER.comment("Base FE extract amount per operation without overclockers")
                .defineInRange("max_fe_no_tiers", 1000, 1, Integer.MAX_VALUE);

        // [修改] 能量卡也同步使用包含 MAX_INT 的默认列表
        MAX_FE_TIERS = COMMON_BUILDER.comment("Maximum FE extraction per operation based on overclockers count (1-4)")
                .defineList("max_fe_tiers",
                        DEFAULT_TIER_VALUES,
                        () -> 1000,
                        o -> o instanceof Integer);

        MAX_FE_TICK = COMMON_BUILDER.comment("Maximum FE/T for Energy Cards (Hard Cap)")
                .defineInRange("max_fe_tick", Integer.MAX_VALUE, 0, Integer.MAX_VALUE);

        MIN_TICKS_ENERGY = COMMON_BUILDER.comment("Minimum ticks for energy transfer")
                .defineInRange("min_ticks_energy", 1, 1, 1200);
        COMMON_BUILDER.pop();

        // Chemical Configs
        COMMON_BUILDER.comment("Chemical Card").push(SUBCATEGORY_CHEMICAL);
        BASE_MILLI_BUCKETS_CHEMICAL = COMMON_BUILDER.comment("Millibuckets for Chemical Cards without Overclockers installed (Only is Mekanism is installed)")
                .defineInRange("base_milli_buckets_chemical", 15000, 0, Integer.MAX_VALUE);
        MULTIPLIER_MILLI_BUCKETS_CHEMICAL = COMMON_BUILDER.comment("Multiplier for Overclocker Cards - Number of Overclockers * this value = max millibuckets  (Only is Mekanism is installed)")
                .defineInRange("multiplier_milli_buckets_chemical", 60000, 0, Integer.MAX_VALUE);

        // [新增] 化学卡层级模式配置
        USE_CHEMICAL_TIERS_MODE = COMMON_BUILDER.comment("If true, use the 'max_chemical_tiers' list instead of the linear multiplier formula. Required for MAX_INT chemical transfer.")
                .define("use_chemical_tiers_mode", true);

        // [新增] 化学卡层级数值
        MAX_CHEMICAL_TIERS = COMMON_BUILDER.comment("Maximum Chemical extraction (mB) per tick based on overclockers count (1-4). Only used if 'use_chemical_tiers_mode' is true.")
                .defineList("max_chemical_tiers",
                        List.of(128000, 512000, 2048000, Integer.MAX_VALUE),
                        () -> 0,
                        o -> o instanceof Integer);

        MIN_TICKS_CHEMICAL = COMMON_BUILDER.comment("Minimum ticks between chemical extractions based on overclockers count (0-4)")
                .defineList("min_ticks_chemical",
                        List.of(20, 15, 10, 5, 1),
                        () -> 20,
                        o -> o instanceof Integer);
        COMMON_BUILDER.pop();

        // Item Configs
        COMMON_BUILDER.comment("Item Card").push("item_card");
        MIN_TICKS_ITEM = COMMON_BUILDER.comment("Minimum ticks between item extractions based on overclockers count (0-4)")
                .defineList("min_ticks_item",
                        List.of(20, 15, 10, 5, 1),
                        () -> 20,
                        o -> o instanceof Integer);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
        SERVER_CONFIG = SERVER_BUILDER.build();
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG);
        container.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }
}