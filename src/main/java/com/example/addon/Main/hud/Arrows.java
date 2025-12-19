package com.example.addon.Main.hud;

import com.example.addon.Hook;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Arrows extends HudElement {
    public static final HudElementInfo<Arrows> INFO = new HudElementInfo<>(Hook.HUD_GROUP, "arrows", "Displays arrows that change color when players are nearby.", Arrows::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The maximum distance to detect players.")
        .defaultValue(50.0)
        .min(0.0)
        .sliderMax(100.0)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the arrows.")
        .defaultValue(10.0)
        .min(1.0)
        .sliderMax(30.0)
        .build()
    );

    private final Setting<Double> spacing = sgGeneral.add(new DoubleSetting.Builder()
        .name("spacing")
        .description("The distance of the arrows from the center of the screen.")
        .defaultValue(20.0)
        .min(0.0)
        .sliderMax(50.0)
        .build()
    );

    private final Setting<SettingColor> defaultColor = sgGeneral.add(new ColorSetting.Builder()
        .name("default-color")
        .description("The color of the arrows when no player is detected.")
        .defaultValue(new SettingColor(150, 150, 150, 200))
        .build()
    );

    private final Setting<SettingColor> detectColor = sgGeneral.add(new ColorSetting.Builder()
        .name("detect-color")
        .description("The color of the arrows when a player is detected.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    public Arrows() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mc.world == null || mc.player == null) return;

        setSize(10, 10);

        boolean playerOnLeft = false;
        boolean playerOnRight = false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isDead() || mc.player.distanceTo(player) > distance.get()) {
                continue;
            }

            if (isPlayerInFov(player)) {
                Vec3d toEntity = player.getPos().subtract(mc.player.getPos()).normalize();
                double angle = Math.toDegrees(Math.atan2(toEntity.z, toEntity.x)) - 90;
                double playerYaw = MathHelper.wrapDegrees(mc.player.getYaw());
                double angleDiff = MathHelper.wrapDegrees(angle - playerYaw);

                if (angleDiff > 0 && angleDiff < 180) { // Check right side
                    playerOnRight = true;
                } else { // Check left side
                    playerOnLeft = true;
                }
            }
        }
        double arrowSize = scale.get();

        // Draw left arrow
        drawArrow(renderer, 5 - spacing.get(), 5, arrowSize, playerOnLeft ? detectColor.get() : defaultColor.get(), false);

        // Draw right arrow
        drawArrow(renderer, 5 + spacing.get(), 5, arrowSize, playerOnRight ? detectColor.get() : defaultColor.get(), true);
    }

    private boolean isPlayerInFov(PlayerEntity player) {
        Vec3d rotationVec = mc.player.getRotationVector();
        Vec3d toEntityVec = player.getPos().subtract(mc.player.getEyePos()).normalize();
        double dotProduct = rotationVec.dotProduct(toEntityVec);
        // A dot product > 0 means the angle is < 90 degrees, which is within a 180-degree FOV.
        return dotProduct > 0;
    }

    private void drawArrow(HudRenderer renderer, double x, double y, double size, SettingColor color, boolean right) {
        double dir = right ? 1 : -1;
        renderer.line(x, y - size / 2, x + dir * size / 2, y, color);
        renderer.line(x + dir * size / 2, y, x, y + size / 2, color);
    }
}
