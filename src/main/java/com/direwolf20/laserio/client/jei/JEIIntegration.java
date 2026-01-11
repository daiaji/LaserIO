package com.direwolf20.laserio.client.jei;

import com.direwolf20.laserio.client.screens.*;
import com.direwolf20.laserio.common.LaserIO;
import com.direwolf20.laserio.integration.ModIntegration;
import com.direwolf20.laserio.integration.jei.handlers.GhostIngredientHandler; // [引用新类]
import com.direwolf20.laserio.integration.jei.handlers.GuiContainerHandler; // [引用新类]
import com.direwolf20.laserio.setup.Registration;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEIIntegration implements IModPlugin {

    @Nonnull
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(LaserIO.MODID, "jei_plugin");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        IRecipeManager recipeRegistry = jeiRuntime.getRecipeManager();
        if (Minecraft.getInstance().level == null) return;
        
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<RecipeHolder<CraftingRecipe>> hiddenRecipes = new ArrayList<>();
        
        // 隐藏 NBT 清除配方
        recipeManager.byKey(ResourceLocation.parse(Registration.Card_Item.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        recipeManager.byKey(ResourceLocation.parse(Registration.Card_Fluid.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        recipeManager.byKey(ResourceLocation.parse(Registration.Card_Energy.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        recipeManager.byKey(ResourceLocation.parse(Registration.Card_Redstone.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        recipeManager.byKey(ResourceLocation.parse(Registration.Filter_Basic.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        recipeManager.byKey(ResourceLocation.parse(Registration.Filter_Count.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        recipeManager.byKey(ResourceLocation.parse(Registration.Filter_Tag.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        recipeManager.byKey(ResourceLocation.parse(Registration.Filter_NBT.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        recipeManager.byKey(ResourceLocation.parse(Registration.Filter_Mod.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        
        // 如果加载了 Mekanism，隐藏化学卡配方
        if (ModIntegration.MEKANISM.isLoaded()) {
             recipeManager.byKey(ResourceLocation.parse(Registration.Card_Chemical.getId() + "_nbtclear")).ifPresent(r -> hiddenRecipes.add((RecipeHolder<CraftingRecipe>) r));
        }

        recipeRegistry.hideRecipes(RecipeTypes.CRAFTING, hiddenRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // [新增] 注册 GUI 遮挡处理器 (解决侧边栏被遮挡问题)
        registration.addGuiContainerHandler(LaserNodeScreen.class, new GuiContainerHandler<>());
        registration.addGuiContainerHandler(CardItemScreen.class, new GuiContainerHandler<>());
        registration.addGuiContainerHandler(CardEnergyScreen.class, new GuiContainerHandler<>());
        
        // [修改] 使用新的通用 GhostIngredientHandler 替换旧的 4 个处理器
        registration.addGhostIngredientHandler(CardItemScreen.class, new GhostIngredientHandler<>());
        registration.addGhostIngredientHandler(FilterBasicScreen.class, new GhostIngredientHandler<>());
        registration.addGhostIngredientHandler(FilterCountScreen.class, new GhostIngredientHandler<>());
        registration.addGhostIngredientHandler(FilterTagScreen.class, new GhostIngredientHandler<>());
        registration.addGhostIngredientHandler(FilterNBTScreen.class, new GhostIngredientHandler<>());
        
        if (ModIntegration.MEKANISM.isLoaded()) {
             // 如果有化学卡界面，也可以注册
             // registration.addGhostIngredientHandler(CardChemicalScreen.class, new GhostIngredientHandler<>());
        }
    }
}