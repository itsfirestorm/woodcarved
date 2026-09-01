package com.itsfirestorm.woodcarved.item;

import com.itsfirestorm.woodcarved.crafting.CarvingRecipe;
import com.itsfirestorm.woodcarved.registries.ModRecipeTypes;
import com.itsfirestorm.woodcarved.screen.carving_blade.CarvingBladeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CarvingBlade extends Item {

    public CarvingBlade(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        ItemStack blockAsItem = new ItemStack(state.getBlock().asItem());
        if (blockAsItem.isEmpty()) {
            return InteractionResult.FAIL;
        }

        List<RecipeHolder<CarvingRecipe>> recipes = level.getRecipeManager()
                .getRecipesFor(ModRecipeTypes.CARVING.get(), new SingleRecipeInput(blockAsItem), level);

        if (recipes.isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CarvingBladeMenu(id, inv, level, pos, context.getHand()),
                    Component.translatable("menu.woodcarved.carving_blade")
            ), pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
