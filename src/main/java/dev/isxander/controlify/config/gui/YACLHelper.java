package dev.isxander.controlify.config.gui;

import dev.isxander.controlify.config.ControlifyConfig;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.controller.ControllerConfig;
import dev.isxander.yacl.api.ConfigCategory;
import dev.isxander.yacl.api.Option;
import dev.isxander.yacl.api.OptionGroup;
import dev.isxander.yacl.api.YetAnotherConfigLib;
import dev.isxander.yacl.gui.controllers.slider.FloatSliderController;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class YACLHelper {
    public static Screen generateConfigScreen(Screen parent) {
        var yacl = YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Controlify"))
                .save(ControlifyConfig::save);

        for (var controller : Controller.CONTROLLERS.values()) {
            var category = ConfigCategory.createBuilder();

            var customName = controller.config().customName;
            category.name(Component.literal(customName == null ? controller.name() : customName));

            var config = controller.config();
            var def = ControllerConfig.DEFAULT;
            var configGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("controlify.gui.group.config"))
                    .tooltip(Component.translatable("controlify.gui.group.config.tooltip"))
                    .option(Option.createBuilder(float.class)
                            .name(Component.translatable("controlify.gui.look_sensitivity"))
                            .tooltip(Component.translatable("controlify.gui.look_sensitivity.tooltip"))
                            .binding(def.lookSensitivity, () -> config.lookSensitivity, v -> config.lookSensitivity = v)
                            .controller(opt -> new FloatSliderController(opt, 0.1f, 2f, 0.05f, v -> Component.literal(String.format("%.0f%%", v*100))))
                            .build())
                    .option(Option.createBuilder(float.class)
                            .name(Component.translatable("controlify.gui.left_stick_deadzone"))
                            .tooltip(Component.translatable("controlify.gui.left_stick_deadzone.tooltip"))
                            .tooltip(Component.translatable("controlify.gui.stickdrift_warning").withStyle(ChatFormatting.RED))
                            .binding(def.leftStickDeadzone, () -> config.leftStickDeadzone, v -> config.leftStickDeadzone = v)
                            .controller(opt -> new FloatSliderController(opt, 0, 1, 0.02f, v -> Component.literal(String.format("%.0f%%", v*100))))
                            .build())
                    .option(Option.createBuilder(float.class)
                            .name(Component.translatable("controlify.gui.right_stick_deadzone"))
                            .tooltip(Component.translatable("controlify.gui.right_stick_deadzone.tooltip"))
                            .tooltip(Component.translatable("controlify.gui.stickdrift_warning").withStyle(ChatFormatting.RED))
                            .binding(def.rightStickDeadzone, () -> config.rightStickDeadzone, v -> config.rightStickDeadzone = v)
                            .controller(opt -> new FloatSliderController(opt, 0, 1, 0.02f, v -> Component.literal(String.format("%.0f%%", v*100))))
                            .build())
                    .option(Option.createBuilder(float.class)
                            .name(Component.translatable("controlify.gui.left_trigger_threshold"))
                            .tooltip(Component.translatable("controlify.gui.left_trigger_threshold.tooltip"))
                            .binding(def.leftTriggerActivationThreshold, () -> config.leftTriggerActivationThreshold, v -> config.leftTriggerActivationThreshold = v)
                            .controller(opt -> new FloatSliderController(opt, 0, 1, 0.05f, v -> Component.literal(String.format("%.0f%%", v*100))))
                            .build())
                    .option(Option.createBuilder(float.class)
                            .name(Component.translatable("controlify.gui.right_trigger_threshold"))
                            .tooltip(Component.translatable("controlify.gui.right_trigger_threshold.tooltip"))
                            .binding(def.rightTriggerActivationThreshold, () -> config.rightTriggerActivationThreshold, v -> config.rightTriggerActivationThreshold = v)
                            .controller(opt -> new FloatSliderController(opt, 0, 1, 0.05f, v -> Component.literal(String.format("%.0f%%", v*100))))
                            .build());
            category.group(configGroup.build());

            yacl.category(category.build());
        }

        return yacl.build().generateScreen(parent);
    }
}
