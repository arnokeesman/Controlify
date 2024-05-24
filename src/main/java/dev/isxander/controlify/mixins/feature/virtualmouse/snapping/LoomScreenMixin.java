package dev.isxander.controlify.mixins.feature.virtualmouse.snapping;

import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import dev.isxander.controlify.virtualmouse.SnapUtils;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.LoomMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.Set;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreenMixin<LoomMenu> {
    @Shadow private int startRow;
    @Shadow @Final private static int PATTERNS_X;
    @Shadow @Final private static int PATTERNS_Y;
    @Shadow @Final private static int PATTERN_ROWS;
    @Shadow @Final private static int PATTERN_COLUMNS;
    @Shadow @Final private static int PATTERN_IMAGE_SIZE;

    protected LoomScreenMixin(Component title) {
        super(title);
    }

    @Override
    public Set<SnapPoint> getSnapPoints() {
        Set<SnapPoint> points = new HashSet<>(super.getSnapPoints());

        int patternAreaStartX = this.leftPos + PATTERNS_X;
        int patternAreaStartY = this.topPos + PATTERNS_Y;
        int startIndex = startRow * PATTERN_COLUMNS;
        int itemCount = this.menu.getSelectablePatterns().size();

        SnapUtils.addSnapPointsFromRowsAndColumns(
                patternAreaStartX,
                patternAreaStartY,
                startIndex,
                PATTERN_ROWS,
                PATTERN_COLUMNS,
                PATTERN_IMAGE_SIZE,
                PATTERN_IMAGE_SIZE,
                itemCount,
                points
        );

        return points;
    }
}
