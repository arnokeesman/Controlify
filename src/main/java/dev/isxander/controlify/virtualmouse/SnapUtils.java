package dev.isxander.controlify.virtualmouse;

import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import dev.isxander.controlify.mixins.feature.virtualmouse.snapping.RecipeBookComponentAccessor;
import dev.isxander.controlify.mixins.feature.virtualmouse.snapping.RecipeBookPageAccessor;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.joml.Vector2i;

import java.util.Collection;

public final class SnapUtils {
    private SnapUtils() {
    }

    public static void addRecipeSnapPoints(RecipeBookComponent recipeBookComponent, Collection<SnapPoint> points) {
        if (recipeBookComponent.isVisible()) {
            RecipeBookComponentAccessor componentAccessor = (RecipeBookComponentAccessor) recipeBookComponent;
            componentAccessor.getTabButtons().forEach(button -> {
                int x = button.getX() + button.getWidth() / 2;
                int y = button.getY() + button.getHeight() / 2;
                points.add(new SnapPoint(new Vector2i(x, y), 20));
            });

            StateSwitchingButton filterButton = componentAccessor.getFilterButton();
            if (filterButton.visible) {
                int x = filterButton.getX() + filterButton.getWidth() / 2;
                int y = filterButton.getY() + filterButton.getHeight() / 2;
                points.add(new SnapPoint(new Vector2i(x, y), 14));
            }

            RecipeBookPageAccessor pageAccessor = (RecipeBookPageAccessor) componentAccessor.getRecipeBookPage();
            pageAccessor.getButtons().stream()
                    .filter(button -> button.visible && button.active)
                    .forEach(button -> {
                        int x = button.getX() + button.getWidth() / 2;
                        int y = button.getY() + button.getHeight() / 2;
                        points.add(new SnapPoint(new Vector2i(x, y), 21));
                    });

            StateSwitchingButton forwardButton = pageAccessor.getForwardButton();
            if (forwardButton.visible) {
                int x = forwardButton.getX() + forwardButton.getWidth() / 2 - 2;
                int y = forwardButton.getY() + forwardButton.getHeight() / 2;
                points.add(new SnapPoint(new Vector2i(x, y), 10));
            }

            StateSwitchingButton backButton = pageAccessor.getBackButton();
            if (backButton.visible) {
                int x = backButton.getX() + backButton.getWidth() / 2 + 2;
                int y = backButton.getY() + backButton.getHeight() / 2;
                points.add(new SnapPoint(new Vector2i(x, y), 10));
            }
        }
    }

    public static void addSnapPointsFromRowsAndColumns(
            int startX,
            int startY,
            int startIndex,
            int rows,
            int columns,
            int tileWidth,
            int tileHeight,
            int cursorOffsetX,
            int cursorOffsetY,
            int itemCount,
            Collection<SnapPoint> points
    ) {
        int availableSlotsOnScreen = startIndex + rows * columns;

        for (int i = startIndex; i < availableSlotsOnScreen && i < itemCount; ++i) {
            int locationNumber = i - startIndex;
            int buttonXPos = startX + locationNumber % columns * tileWidth;
            int row = locationNumber / columns;
            int buttonYPos = startY + row * tileHeight;
            points.add(new SnapPoint(
                    buttonXPos + tileHeight / 2 + cursorOffsetX,
                    buttonYPos + tileHeight / 2 + cursorOffsetY,
                    Math.min(tileWidth, tileHeight)));
        }
    }
}
