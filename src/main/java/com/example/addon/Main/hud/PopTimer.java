package com.example.addon.Main.hud;

import com.example.addon.Api.Elite.TotemPopEvent;
import com.example.addon.Hook;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.EventHandler;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PopTimer extends HudElement {
    public static final HudElementInfo<PopTimer> INFO = new HudElementInfo<>(Hook.HUD_GROUP, "pop-timer", "Displays a timer when you pop a totem.", PopTimer::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> duration = sgGeneral.add(new IntSetting.Builder()
        .name("duration")
        .description("How long the timer should last in seconds.")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the timer circle.")
        .defaultValue(new SettingColor(255, 165, 0, 255))
        .build()
    );

    private long popTime = 0;

    public PopTimer() {
        super(INFO);
    }

    @EventHandler
    private void onTotemPop(TotemPopEvent event) {
        if (event.getEntity() == mc.player) {
            popTime = System.currentTimeMillis();
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        if (isInEditor()) {
            setSize(32, 32);
            drawCircle(renderer, x + 16, y + 16, 16, color.get(), 0, 1.0);
            return;
        }

        long elapsed = System.currentTimeMillis() - popTime;
        long durationMillis = duration.get() * 1000L;

        if (popTime == 0 || elapsed >= durationMillis) {
            setSize(0, 0);
            return;
        }

        double radius = 16;
        setSize(radius * 2, radius * 2);

        double percentage = 1.0 - ((double) elapsed / durationMillis);

        drawCircle(renderer, x + radius, y + radius, radius, new Color(0, 0, 0, 100), 0, 1.0);
        drawCircle(renderer, x + radius, y + radius, radius, color.get(), 0, percentage);
    }

    private void drawCircle(HudRenderer renderer, double centerX, double centerY, double radius, Color color, double startAngle, double endAngle) {
        for (int i = (int) (startAngle * 360); i < (int) (endAngle * 360); i++) {
            double angle1 = Math.toRadians(i - 90);
            double angle2 = Math.toRadians(i + 1 - 90);
            double x1 = centerX + Math.cos(angle1) * radius;
            double y1 = centerY + Math.sin(angle1) * radius;
            double x2 = centerX + Math.cos(angle2) * radius;
            double y2 = centerY + Math.sin(angle2) * radius;
            renderer.line(x1, y1, x2, y2, color);
        }
    }
}
