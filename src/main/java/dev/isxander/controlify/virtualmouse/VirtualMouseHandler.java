package dev.isxander.controlify.virtualmouse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.vmousesnapping.ISnapBehaviour;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.debug.DebugProperties;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.mixins.feature.virtualmouse.KeyboardHandlerAccessor;
import dev.isxander.controlify.mixins.feature.virtualmouse.MouseHandlerAccessor;
import dev.isxander.controlify.utils.HoldRepeatHelper;
import dev.isxander.controlify.utils.ToastUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.RoundingMode;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Set;

public class VirtualMouseHandler {
    private static final ResourceLocation CURSOR_TEXTURE = new ResourceLocation("controlify", "textures/gui/virtual_mouse.png");

    private double targetX, targetY;
    private double currentX, currentY;

    private double scrollX, scrollY;

    private final Minecraft minecraft;
    private boolean virtualMouseEnabled;

    private Set<SnapPoint> snapPoints;
    private SnapPoint lastSnappedPoint;

    private final HoldRepeatHelper holdRepeatHelper = new HoldRepeatHelper(10, 6);

    public VirtualMouseHandler() {
        this.minecraft = Minecraft.getInstance();

        if (minecraft.screen != null && minecraft.screen instanceof ISnapBehaviour snapBehaviour)
            snapPoints = snapBehaviour.getSnapPoints();
        else
            snapPoints = Set.of();

        ControlifyEvents.INPUT_MODE_CHANGED.register(this::onInputModeChanged);
    }

    public void handleControllerInput(Controller<?, ?> controller) {
        if (controller.bindings().VMOUSE_TOGGLE.justPressed()) {
            toggleVirtualMouse();
        }

        if (!virtualMouseEnabled) {
            return;
        }

        var impulseY = controller.bindings().VMOUSE_MOVE_DOWN.state() - controller.bindings().VMOUSE_MOVE_UP.state();
        var impulseX = controller.bindings().VMOUSE_MOVE_RIGHT.state() - controller.bindings().VMOUSE_MOVE_LEFT.state();
        var prevImpulseY = controller.bindings().VMOUSE_MOVE_DOWN.prevState() - controller.bindings().VMOUSE_MOVE_UP.prevState();
        var prevImpulseX = controller.bindings().VMOUSE_MOVE_RIGHT.prevState() - controller.bindings().VMOUSE_MOVE_LEFT.prevState();

        if (minecraft.screen != null && minecraft.screen instanceof ISnapBehaviour snapBehaviour) {
            snapPoints = snapBehaviour.getSnapPoints();
        } else {
            snapPoints = Set.of();
        }

        // if just released stick, snap to nearest snap point
        if (impulseX == 0 && impulseY == 0) {
            if ((prevImpulseX != 0 || prevImpulseY != 0))
                snapToClosestPoint();
        }

        var sensitivity = controller.config().virtualMouseSensitivity;
        var windowSizeModifier = Math.max(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight()) / 800f;

        // quadratic function to make small movements smaller
        // abs to keep sign
        targetX += impulseX * Mth.abs(impulseX) * 20f * sensitivity * windowSizeModifier;
        targetY += impulseY * Mth.abs(impulseY) * 20f * sensitivity * windowSizeModifier;

        targetX = Mth.clamp(targetX, 0, minecraft.getWindow().getWidth());
        targetY = Mth.clamp(targetY, 0, minecraft.getWindow().getHeight());

        scrollY += controller.bindings().VMOUSE_SCROLL_UP.state() - controller.bindings().VMOUSE_SCROLL_DOWN.state();

        if (holdRepeatHelper.shouldAction(controller.bindings().VMOUSE_SNAP_UP)) {
            snapInDirection(ScreenDirection.UP);
            holdRepeatHelper.onNavigate();
        } else if (holdRepeatHelper.shouldAction(controller.bindings().VMOUSE_SNAP_DOWN)) {
            snapInDirection(ScreenDirection.DOWN);
            holdRepeatHelper.onNavigate();
        } else if (holdRepeatHelper.shouldAction(controller.bindings().VMOUSE_SNAP_LEFT)) {
            snapInDirection(ScreenDirection.LEFT);
            holdRepeatHelper.onNavigate();
        } else if (holdRepeatHelper.shouldAction(controller.bindings().VMOUSE_SNAP_RIGHT)) {
            snapInDirection(ScreenDirection.RIGHT);
            holdRepeatHelper.onNavigate();
        }

        if (ScreenProcessorProvider.provide(minecraft.screen).virtualMouseBehaviour().isDefaultOr(VirtualMouseBehaviour.ENABLED)) {
            handleCompatibilityBinds(controller);
        }

        if (controller.bindings().GUI_BACK.justPressed() && minecraft.screen != null) {
            ScreenProcessor.playClackSound();
            minecraft.screen.onClose();
        }
    }

    public void handleCompatibilityBinds(Controller<?, ?> controller) {
        var mouseHandler = (MouseHandlerAccessor) minecraft.mouseHandler;
        var keyboardHandler = (KeyboardHandlerAccessor) minecraft.keyboardHandler;

        if (controller.bindings().VMOUSE_LCLICK.justPressed()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
        } else if (controller.bindings().VMOUSE_LCLICK.justReleased()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
        }

        if (controller.bindings().VMOUSE_RCLICK.justPressed()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS, 0);
        } else if (controller.bindings().VMOUSE_RCLICK.justReleased()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_RELEASE, 0);
        }

        if (controller.bindings().VMOUSE_SHIFT_CLICK.justPressed()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
        } else if (controller.bindings().VMOUSE_SHIFT_CLICK.justReleased()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
        }
    }

    public void updateMouse() {
        if (!virtualMouseEnabled) return;

        if (Math.round(targetX * 100) / 100.0 != Math.round(currentX * 100) / 100.0 || Math.round(targetY * 100) / 100.0 != Math.round(currentY * 100) / 100.0) {
            currentX = Mth.lerp(minecraft.getFrameTime(), currentX, targetX);
            currentY = Mth.lerp(minecraft.getFrameTime(), currentY, targetY);

            ((MouseHandlerAccessor) minecraft.mouseHandler).invokeOnMove(minecraft.getWindow().getWindow(), currentX, currentY);
        } else {
            currentX = targetX;
            currentY = targetY;
        }

        if (Math.abs(scrollX) >= 0.01 || Math.abs(scrollY) >= 0.01) {
            var currentScrollY = scrollY * Minecraft.getInstance().getDeltaFrameTime();
            scrollY -= currentScrollY;
            var currentScrollX = scrollX * Minecraft.getInstance().getDeltaFrameTime();
            scrollX -= currentScrollX;

            ((MouseHandlerAccessor) minecraft.mouseHandler).invokeOnScroll(minecraft.getWindow().getWindow(), currentScrollX, currentScrollY);
        } else {
            scrollX = scrollY = 0;
        }
    }

    private void snapToClosestPoint() {
        var window = minecraft.getWindow();
        var scaleFactor = new Vector2d((double)window.getGuiScaledWidth() / (double)window.getScreenWidth(), (double)window.getGuiScaledHeight() / (double)window.getScreenHeight());
        var target = new Vector2d(targetX, targetY).mul(scaleFactor);

        if (lastSnappedPoint != null) {
            if (lastSnappedPoint.position().distanceSquared(new Vector2i(target, RoundingMode.FLOOR)) > (long) lastSnappedPoint.range() * lastSnappedPoint.range()) {
                lastSnappedPoint = null;
            }
        }

        var closestSnapPoint = snapPoints.stream()
                .filter(snapPoint -> !snapPoint.equals(lastSnappedPoint)) // don't snap to the point currently over snapped point
                .map(snapPoint -> new Pair<>(snapPoint, snapPoint.position().distanceSquared(new Vector2i(target, RoundingMode.FLOOR)))) // map with distance to current pos
                .filter(point -> point.getSecond() <= (long) point.getFirst().range() * point.getFirst().range()) // filter out of range options
                .min(Comparator.comparingLong(Pair::getSecond)) // find the closest point
                .orElse(new Pair<>(null, Long.MAX_VALUE)).getFirst(); // retrieve point

        if (closestSnapPoint != null) {
            snapToPoint(closestSnapPoint, scaleFactor);
        }
    }

    private void snapInDirection(ScreenDirection direction) {
        var window = minecraft.getWindow();
        var scaleFactor = new Vector2d((double)window.getGuiScaledWidth() / (double)window.getScreenWidth(), (double)window.getGuiScaledHeight() / (double)window.getScreenHeight());
        var target = new Vector2d(targetX, targetY).mul(scaleFactor);

        var closestSnapPoint = snapPoints.stream()
                .filter(snapPoint -> !snapPoint.equals(lastSnappedPoint)) // don't snap to the point currently over snapped point
                .map(snapPoint -> new Pair<>(snapPoint, new Vector2d(snapPoint.position().x() - target.x(), snapPoint.position().y() - target.y()))) // map with distance to current pos
                // filter points that are not in the correct direction
                .filter(pair -> {
                    Vector2d dist = pair.getSecond();

                    double axis = direction.getAxis() == ScreenAxis.HORIZONTAL ? dist.x : dist.y;
                    double positive = direction.isPositive() ? 1 : -1;

                    return axis * positive > 0;
                })
                .filter(pair -> {
                    SnapPoint snapPoint = pair.getFirst();
                    Vector2d dist = pair.getSecond();

                    // distance in the correct orthogonal direction
                    double distance = Math.abs(direction.getAxis() == ScreenAxis.HORIZONTAL ? dist.x : dist.y);
                    // distance in the incorrect orthogonal direction
                    double deviation = Math.abs(direction.getAxis() == ScreenAxis.HORIZONTAL ? dist.y : dist.x);

                    // punish deviation significantly
                    pair.getSecond().set(distance, deviation * 4);

                    // reject if the deviation is double the correct direction away
                    return distance >= snapPoint.range() && (deviation < distance * 2);
                })
                // pick the closest point
                .min(Comparator.comparingDouble(pair -> {
                    Vector2d distDev = pair.getSecond();
                    // x = dist, y = deviation
                    return distDev.x + distDev.y;
                }))
                .map(Pair::getFirst);

        closestSnapPoint.ifPresent(snapPoint -> {
            snapToPoint(snapPoint, scaleFactor);
        });
    }

    public void snapToPoint(SnapPoint snapPoint, Vector2dc scaleFactor) {
        lastSnappedPoint = snapPoint;

        targetX = currentX = snapPoint.position().x() / scaleFactor.x();
        targetY = currentY = snapPoint.position().y() / scaleFactor.y();
        ((MouseHandlerAccessor) minecraft.mouseHandler).invokeOnMove(minecraft.getWindow().getWindow(), currentX, currentY);
    }

    public void onScreenChanged() {
        if (minecraft.screen != null) {
            if (requiresVirtualMouse()) {
                enableVirtualMouse();
            } else {
                disableVirtualMouse();
            }
            if (Controlify.instance().currentInputMode().isController())
                GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        } else if (virtualMouseEnabled) {
            disableVirtualMouse();

            minecraft.mouseHandler.grabMouse(); // re-grab mouse after vmouse disable
        }
    }

    public void onInputModeChanged(InputMode mode) {
        if (mode.isController()) {
            if (requiresVirtualMouse()) {
                enableVirtualMouse();
            }
        } else if (virtualMouseEnabled) {
            disableVirtualMouse();
        }
    }

    public void renderVirtualMouse(GuiGraphics graphics) {
        if (!virtualMouseEnabled) return;

        if (DebugProperties.DEBUG_SNAPPING) {
            for (var snapPoint : snapPoints) {
                graphics.fill(snapPoint.position().x() - snapPoint.range(), snapPoint.position().y() - snapPoint.range(), snapPoint.position().x() + snapPoint.range(), snapPoint.position().y() + snapPoint.range(), 0x33FFFFFF);
                graphics.fill( snapPoint.position().x() - 1, snapPoint.position().y() - 1, snapPoint.position().x() + 1, snapPoint.position().y() + 1, snapPoint.equals(lastSnappedPoint) ? 0xFFFFFF00 : 0xFFFF0000);
            }
        }

        var scaledX = currentX * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
        var scaledY = currentY * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();

        graphics.pose().pushPose();
        graphics.pose().translate(scaledX, scaledY, 1000f);
        graphics.pose().scale(0.5f, 0.5f, 0.5f);

        RenderSystem.enableBlend();
        graphics.blit(CURSOR_TEXTURE, -16, -16, 0, 0, 32, 32, 32, 32);
        RenderSystem.disableBlend();

        graphics.pose().popPose();
    }

    public void enableVirtualMouse() {
        if (virtualMouseEnabled) return;

        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        virtualMouseEnabled = true;

        if (minecraft.mouseHandler.xpos() == -50 && minecraft.mouseHandler.ypos() == -50) {
            targetX = currentX = minecraft.getWindow().getScreenWidth() / 2f;
            targetY = currentY = minecraft.getWindow().getScreenHeight() / 2f;
        } else {
            targetX = currentX = minecraft.mouseHandler.xpos();
            targetY = currentY = minecraft.mouseHandler.ypos();
        }
        setMousePosition();

        ControlifyEvents.VIRTUAL_MOUSE_TOGGLED.invoker().onVirtualMouseToggled(true);
    }

    public void disableVirtualMouse() {
        if (!virtualMouseEnabled) return;

        // make sure minecraft doesn't think the mouse is grabbed when it isn't
        ((MouseHandlerAccessor) minecraft.mouseHandler).setMouseGrabbed(false);

        Controlify.instance().hideMouse(true, true);
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        setMousePosition();
        virtualMouseEnabled = false;
        targetX = currentX = minecraft.mouseHandler.xpos();
        targetY = currentY = minecraft.mouseHandler.ypos();

        ControlifyEvents.VIRTUAL_MOUSE_TOGGLED.invoker().onVirtualMouseToggled(false);
    }

    private void setMousePosition() {
        GLFW.glfwSetCursorPos(
                minecraft.getWindow().getWindow(),
                targetX,
                targetY
        );
    }

    public boolean requiresVirtualMouse() {
        var isController = Controlify.instance().currentInputMode().isController();
        var hasScreen = minecraft.screen != null;

        if (isController && hasScreen) {
            return switch (ScreenProcessorProvider.provide(minecraft.screen).virtualMouseBehaviour()) {
                case DEFAULT -> Controlify.instance().config().globalSettings().virtualMouseScreens.stream().anyMatch(s -> s.isAssignableFrom(minecraft.screen.getClass()));
                case ENABLED, CURSOR_ONLY -> true;
                case DISABLED -> false;
            };
        }

        return false;
    }

    public void toggleVirtualMouse() {
        if (minecraft.screen == null) return;

        if (ScreenProcessorProvider.provide(minecraft.screen).virtualMouseBehaviour() != VirtualMouseBehaviour.DEFAULT) {
            ToastUtils.sendToast(
                    Component.translatable("controlify.toast.vmouse_unavailable.title"),
                    Component.translatable("controlify.toast.vmouse_unavailable.description"),
                    false
            );
            return;
        }

        var screens = Controlify.instance().config().globalSettings().virtualMouseScreens;
        var screenClass = minecraft.screen.getClass();
        if (screens.contains(screenClass)) {
            screens.remove(screenClass);
            disableVirtualMouse();
            Controlify.instance().hideMouse(true, false);

            ToastUtils.sendToast(
                    Component.translatable("controlify.toast.vmouse_disabled.title"),
                    Component.translatable("controlify.toast.vmouse_disabled.description"),
                    false
            );
        } else {
            screens.add(screenClass);
            enableVirtualMouse();

            ToastUtils.sendToast(
                    Component.translatable("controlify.toast.vmouse_enabled.title"),
                    Component.translatable("controlify.toast.vmouse_enabled.description"),
                    false
            );
        }

        Controlify.instance().config().save();
    }

    public boolean isVirtualMouseEnabled() {
        return virtualMouseEnabled;
    }

    public int getCurrentX(float deltaTime) {
        return (int) Mth.lerp(deltaTime, currentX, targetX);
    }

    public int getCurrentY(float deltaTime) {
        return (int) Mth.lerp(deltaTime, currentY, targetY);
    }
}
