package com.itsfirestorm.woodcarved.crafting;

import com.itsfirestorm.woodcarved.woodcarved;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, woodcarved.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CarvingRecipe>> CARVING =
            RECIPE_SERIALIZERS.register("carving", CarvingRecipe.Serializer::new);
}
