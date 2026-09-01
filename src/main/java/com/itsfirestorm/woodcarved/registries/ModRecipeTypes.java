package com.itsfirestorm.woodcarved.registries;

import com.itsfirestorm.woodcarved.crafting.CarvingRecipe;
import com.itsfirestorm.woodcarved.woodcarved;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, woodcarved.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CarvingRecipe>> CARVING =
            RECIPE_TYPES.register("carving", () -> new RecipeType<CarvingRecipe>() {
                @Override
                public String toString() {
                    return "carving";
                }
            });
}
