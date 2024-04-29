package dev.isxander.controlify.mixins.feature.virtualmouse.snapping;

import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.StonecutterMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.Set;

@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenMixin extends AbstractContainerScreenMixin<StonecutterMenu> {
    @Shadow private int startIndex;
    @Shadow @Final private static int RECIPES_COLUMNS;
    @Shadow @Final private static int RECIPES_ROWS;
    @Shadow @Final private static int RECIPES_X;
    @Shadow @Final private static int RECIPES_Y;
    @Shadow @Final private static int RECIPES_IMAGE_SIZE_HEIGHT;
    @Shadow @Final private static int RECIPES_IMAGE_SIZE_WIDTH;

    protected StonecutterScreenMixin(Component title) {
        super(title);
    }

    @Override
    public Set<SnapPoint> getSnapPoints() {
        Set<SnapPoint> points = new HashSet<>(super.getSnapPoints());
        int recipeAreaStartX = this.leftPos + RECIPES_X;
        int recipeAreaStartY = this.topPos + RECIPES_Y;
        int recipeCountLimit = this.startIndex + RECIPES_ROWS * RECIPES_COLUMNS;

        for (int i = this.startIndex; i < recipeCountLimit && i < this.menu.getNumRecipes(); ++i) {
            int locationNumber = i - this.startIndex;
            int buttonXPos = recipeAreaStartX + locationNumber % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
            int row = locationNumber / RECIPES_COLUMNS;
            int buttonYPos = recipeAreaStartY + row * RECIPES_IMAGE_SIZE_HEIGHT;
            points.add(new SnapPoint(buttonXPos + RECIPES_IMAGE_SIZE_WIDTH / 2, buttonYPos + RECIPES_IMAGE_SIZE_HEIGHT / 2 + 1, 16));
        }

        return points;
    }
}
