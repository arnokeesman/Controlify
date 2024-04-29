package dev.isxander.controlify.mixins.feature.virtualmouse.snapping;

import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.BeaconMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends AbstractContainerScreenMixin<BeaconMenu> {
    @Shadow
    @Final
    private List<BeaconScreen.BeaconButton> beaconButtons;

    protected BeaconScreenMixin(Component title) {
        super(title);
    }

    @Override
    public Set<SnapPoint> getSnapPoints() {
        Set<SnapPoint> points = new HashSet<>(super.getSnapPoints());
        beaconButtons.forEach(button -> {
            if (button instanceof AbstractWidget widget) {
                if (!widget.visible) return;

                points.add(new SnapPoint(
                        widget.getX() + widget.getWidth() / 2,
                        widget.getY() + widget.getHeight() / 2,
                        16));
            }
        });
        return points;
    }
}
