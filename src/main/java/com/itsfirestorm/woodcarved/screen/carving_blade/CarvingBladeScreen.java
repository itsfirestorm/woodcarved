package com.itsfirestorm.woodcarved.screen.carving_blade;

import com.itsfirestorm.woodcarved.crafting.CarvingRecipe;
import com.itsfirestorm.woodcarved.woodcarved;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CarvingBladeScreen extends AbstractContainerScreen<CarvingBladeMenu> {
    private final static ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(woodcarved.MODID,
                    "textures/gui/carving_blade.png"
            );

    // -- Panel --
    private static final int PANEL_TEX_W = 171;
    private static final int PANEL_TEX_H = 126;

    // -- Grid --
    // -- Result area grid definitions --
    private static final int SLOT_SIZE = 32;
    private static final int RECIPE_AREA_X = 14;
    private static final int RECIPE_AREA_Y = 15;
    private static final int COLUMNS = 4;
    private static final int ROWS = 3;
    private static final int VISIBLE = COLUMNS * ROWS;

    // -- Grid buttons --
    private static final int SLOT_BG_Y = 126;
    private static final int SLOT_BG_NORMAL_X = 0;
    private static final int SLOT_BG_SELECTED_X = 32;
    private static final int SLOT_BG_HOVER_X = 64;

    // Vanilla items render at 16x16, center that
    private static final float ITEM_SCALE = 1.2F;
    private static final int ITEM_RENDER_SIZE = 16;
    private static final int ITEM_INSET = (SLOT_SIZE - (int) (ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;

    // -- Scrollbar --
    private static final int SCROLL_TRACK_X = 145;
    private static final int SCROLL_TRACK_Y = 15;
    private static final int SCROLL_TRACK_H = 84;
    private static final int HANDLE_W = 12;
    private static final int HANDLE_H = 15;
    private static final int HANDLE_ENABLED_X = 171;
    private static final int HANDLE_DISABLED_X = 183;
    private static final int HANDLE_TEX_Y = 0;
    private static final int SCROLL_TRAVEL = SCROLL_TRACK_H - HANDLE_H;

    // Scroll state
    private int startIndex = 0;
    private float scrollOffs = 0.0F;
    private boolean scrolling = false;
    private boolean hasSelectedNewRecipe = false;

    public CarvingBladeScreen(CarvingBladeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_TEX_W;
        this.imageHeight = PANEL_TEX_H;
        this.titleLabelX = 8;
        this.titleLabelY = 5;

        this.inventoryLabelY = -1000; // Offscreen since we don't need the inventory label
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int recipeAreaX = x + RECIPE_AREA_X;
        int recipeAreaY = y + RECIPE_AREA_Y;
        int selectedRecipe = this.menu.getSelectedRecipe();

        this.renderRecipeSlots(guiGraphics, mouseX, mouseY, recipeAreaX, recipeAreaY, selectedRecipe);
        this.renderScrollbar(guiGraphics, x, y);
    }

    private void renderRecipeSlots(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                   int gridX, int gridY, int selectedRecipe) {
        List<RecipeHolder<CarvingRecipe>> recipes = this.menu.getRecipes();

        if (this.startIndex < 0) this.startIndex = 0;
        if (this.startIndex >= recipes.size() && !recipes.isEmpty()) {
            this.startIndex = Math.max(0, ((recipes.size() - 1) / COLUMNS) * COLUMNS);
        }

        for (int i = this.startIndex; i < Math.min(this.startIndex + VISIBLE, recipes.size()); i++) {
            int col = (i - this.startIndex) % COLUMNS;
            int row = (i - this.startIndex) / COLUMNS;
            int slotX = gridX + col * SLOT_SIZE;
            int slotY = gridY + row * SLOT_SIZE;

            boolean isSelected = i == selectedRecipe;
            boolean isHovered = !isSelected && mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;

            int bgX = isSelected ? SLOT_BG_SELECTED_X : (isHovered ? SLOT_BG_HOVER_X : SLOT_BG_NORMAL_X);
            guiGraphics.blit(GUI_TEXTURE, slotX, slotY, bgX, SLOT_BG_Y, SLOT_SIZE, SLOT_SIZE);

            assert this.minecraft != null;
            assert this.minecraft.level != null;
            ItemStack resultItem = recipes.get(i).value().getResultItem(this.minecraft.level.registryAccess());
            this.renderScaledItem(guiGraphics, resultItem, slotX + ITEM_INSET, slotY + ITEM_INSET);
        }
    }

    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(CarvingBladeScreen.ITEM_SCALE, CarvingBladeScreen.ITEM_SCALE, 1.0F);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int x, int y) {
        int trackX = x + SCROLL_TRACK_X;
        int trackY = y + SCROLL_TRACK_Y;

        boolean canScroll = this.canScroll();
        int handleTexX = canScroll ? HANDLE_ENABLED_X : HANDLE_DISABLED_X;
        int handleY = canScroll ? trackY + (int) (SCROLL_TRAVEL * this.scrollOffs) : trackY;

        guiGraphics.blit(GUI_TEXTURE, trackX, handleY, handleTexX, HANDLE_TEX_Y, HANDLE_W, HANDLE_H);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int selectedRecipe = getSelectedRecipe(mouseX, mouseY, x);

        if (selectedRecipe != -1) {
            if (selectedRecipe == this.menu.getSelectedRecipe()) {
                sendButtonClick(-1);
                return true;
            }


            sendButtonClick(selectedRecipe);
            this.menu.setSelectedRecipe(selectedRecipe);
            this.hasSelectedNewRecipe = true;
            return true;
        }

        if (this.canScroll()) {
            int handleY = SCROLL_TRACK_Y + (int) (SCROLL_TRAVEL * this.scrollOffs);
            if (isHovering(SCROLL_TRACK_X, handleY, HANDLE_W, HANDLE_H, mouseX, mouseY)) {
                this.scrolling = true;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling = false;

        if (this.menu.getSelectedRecipe() >= 0 && this.hasSelectedNewRecipe) {
            this.sendButtonClick(CarvingBladeMenu.CARVE_CONFIRMATION_ID);
            this.hasSelectedNewRecipe = false;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }



    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling && this.canScroll()) {
            int y = (this.height - this.imageHeight) / 2;
            int trackY = y + SCROLL_TRACK_Y;

            this.scrollOffs = ((float) mouseY - trackY - HANDLE_H / 2.0F) / SCROLL_TRAVEL;
            this.scrollOffs = Math.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffs * this.getOffScreenRows() + 0.5D) * COLUMNS;
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.canScroll()) {
            int offScreenRows = this.getOffScreenRows();
            float delta = (float) scrollY / offScreenRows;
            this.scrollOffs = Math.clamp(this.scrollOffs - delta, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffs * offScreenRows + 0.5D) * COLUMNS;
        }
        return true;
    }

    private boolean canScroll() {
        return this.menu.getNumRecipes() > VISIBLE;
    }

    private int getOffScreenRows() {
        int numRecipes = this.menu.getNumRecipes();
        if (numRecipes <= VISIBLE) return 0;
        return (numRecipes + COLUMNS - 1) / COLUMNS - ROWS;
    }

    private int getSelectedRecipe(double mouseX, double mouseY, int x) {
        int y = (this.height - this.imageHeight) / 2;

        int recipeAreaX = x + RECIPE_AREA_X;
        int recipeAreaY = y + RECIPE_AREA_Y;
        int selectedRecipe = -1;

        for (int i = startIndex; i < Math.min(startIndex + VISIBLE, this.menu.getNumRecipes()); i++) {
            int col = (i - startIndex) % COLUMNS;
            int row = (i - startIndex) / COLUMNS;
            int slotX = recipeAreaX + col * SLOT_SIZE;
            int slotY = recipeAreaY + row * SLOT_SIZE;

            if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE &&
                    mouseY >= slotY && mouseY <= slotY + SLOT_SIZE) {
                selectedRecipe = i;
            }
        }
        return selectedRecipe;
    }

    private void sendButtonClick(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            if (id >= 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
            if (id < 0 || id == CarvingBladeMenu.CARVE_CONFIRMATION_ID) {
                this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
            }
        }
    }
}
