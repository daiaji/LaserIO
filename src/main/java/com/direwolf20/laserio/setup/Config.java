package com.direwolf20.laserio.setup;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
    public static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    //public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_CARD = "card";
    public static final String SUBCATEGORY_FLUID = "fluid_card";
    public static final String SUBCATEGORY_ENERGY = "energy_card";
    public static final String SUBCATEGORY_CHEMICAL = "chemical_card";

    // Node connecting distance and interaction range
    public static ModConfigSpec.IntValue MAX_NODES_DISTANCE;
    public static ModConfigSpec.IntValue MAX_INTERACTION_RANGE;

    public static ModConfigSpec.IntValue BASE_MILLI_BUCKETS_FLUID;
    public static ModConfigSpec.IntValue MULTIPLIER_MILLI_BUCKETS_FLUID;
    public static ModConfigSpec.IntValue BASE_MILLI_BUCKETS_CHEMICAL;
    public static ModConfigSpec.IntValue MULTIPLIER_MILLI_BUCKETS_CHEMICAL;
    
    // Energy and Tick related configs
    public static ModConfigSpec.ConfigValue<List<? extends Integer>> MAX_FE_TIERS;
    public static ModConfigSpec.IntValue MAX_FE_TICK;
    public static ModConfigSpec.IntValue MAX_FE_NO_TIERS;
    
    public static ModConfigSpec.ConfigValue<List<? extends Integer>> MIN_TICKS_FLUID;
    public static ModConfigSpec.ConfigValue<List<? extends Integer>> MIN_TICKS_ITEM;
    public static ModConfigSpec.ConfigValue<List<? extends Integer>> MIN_TICKS_CHEMICAL;
    public static ModConfigSpec.IntValue MIN_TICKS_ENERGY;

    public static void register(ModContainer container) {
        //registerServerConfigs(container);
        registerCommonConfigs(container);
        //registerClientConfigs(container);
    }

    private static void registerClientConfigs(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_BUILDER.build());
    }

    private static void registerCommonConfigs(ModContainer container) {
        COMMON_BUILDER.comment("General settings").push("general");
        generalConfig();
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Card settings").push(CATEGORY_CARD);
        cardConfig();
        COMMON_BUILDER.pop();

        container.registerConfig(ModConfig.Type.COMMON, COMMON_BUILDER.build());
    }

    private static void registerServerConfigs(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SERVER_BUILDER.build());
    }

    private static void generalConfig() {
        MAX_NODES_DISTANCE = COMMON_BUILDER.comment("Maximum distance in blocks between two Laser Nodes (Default: 8)")
                .defineInRange("max_nodes_distance", 8, 1, 64);

        MAX_INTERACTION_RANGE = COMMON_BUILDER.comment("Maximum distance in blocks for Laser Wrench interaction (Default: 10)")
                .defineInRange("max_interaction_range", 10, 1, 64);
    }

    @SuppressWarnings("deprecation")
    private static void cardConfig() {
        COMMON_BUILDER.comment("Fluid Card").push(SUBCATEGORY_FLUID);
        BASE_MILLI_BUCKETS_FLUID = COMMON_BUILDER.comment("Millibuckets for Fluid Cards without Overclockers installed")
                .defineInRange("base_milli_buckets_fluid", 5000, 0, Integer.MAX_VALUE);
        MULTIPLIER_MILLI_BUCKETS_FLUID = COMMON_BUILDER.comment("Multiplier for Overclocker Cards - Number of Overclockers * this value = max millibuckets")
                .defineInRange("multiplier_milli_buckets_fluid", 10000, 0, Integer.MAX_VALUE);
        
        MIN_TICKS_FLUID = COMMON_BUILDER.comment("Minimum ticks between fluid extractions based on overclockers count (0-4)")
                .defineList("min_ticks_fluid", List.of(20, 15, 10, 5, 1), o -> o instanceof Integer);
        
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Energy Card").push(SUBCATEGORY_ENERGY);
        
        MAX_FE_TIERS = COMMON_BUILDER.comment("Maximum FE extraction per tick based on overclockers count (1-4)")
                .defineList("max_fe_tiers", List.of(4000, 16000, 32000, 100000), o -> o instanceof Integer);
        
        MAX_FE_TICK = COMMON_BUILDER.comment("Maximum FE/T for Energy Cards (Hard Cap)")
                .defineInRange("max_fe_tick", 1000000, 0, Integer.MAX_VALUE);
        MAX_FE_NO_TIERS = COMMON_BUILDER.comment("Base FE extract amount without overclockers")
                .defineInRange("max_fe_no_tiers", 1000, 1, Integer.MAX_VALUE);
        MIN_TICKS_ENERGY = COMMON_BUILDER.comment("Minimum ticks for energy transfer")
                .defineInRange("min_ticks_energy", 1, 1, 1200);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Chemical Card").push(SUBCATEGORY_CHEMICAL);
        BASE_MILLI_BUCKETS_CHEMICAL = COMMON_BUILDER.comment("Millibuckets for Chemical Cards without Overclockers installed (Only is Mekanism is installed)")
                .defineInRange("base_milli_buckets_chemical", 15000, 0, Integer.MAX_VALUE);
        MULTIPLIER_MILLI_BUCKETS_CHEMICAL = COMMON_BUILDER.comment("Multiplier for Overclocker Cards - Number of Overclockers * this value = max millibuckets  (Only is Mekanism is installed)")
                .defineInRange("multiplier_milli_buckets_chemical", 60000, 0, Integer.MAX_VALUE);
        
        MIN_TICKS_CHEMICAL = COMMON_BUILDER.comment("Minimum ticks between chemical extractions based on overclockers count (0-4)")
                .defineList("min_ticks_chemical", List.of(20, 15, 10, 5, 1), o -> o instanceof Integer);
        
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Item Card").push("item_card");
        
        MIN_TICKS_ITEM = COMMON_BUILDER.comment("Minimum ticks between item extractions based on overclockers count (0-4)")
                .defineList("min_ticks_item", List.of(20, 15, 10, 5, 1), o -> o instanceof Integer);
        
        COMMON_BUILDER.pop();
    }


    private static void clientConfig() {

    }

    private static void serverConfig() {

    }

}