package com.example.addon.Main.hud;

import com.example.addon.Api.util.PingUtils;
import com.example.addon.Hook;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Indicators extends HudElement {
    public static final HudElementInfo<Indicators> INFO = new HudElementInfo<>(Hook.HUD_GROUP, "indicators", "Displays various indicators.", Indicators::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPing = settings.createGroup("Ping");
    private final SettingGroup sgBomb = settings.createGroup("Bomb");
    private final SettingGroup sgFakeLag = settings.createGroup("Fake Lag");

    // General Indicators
    private final Setting<Boolean> damage = sgGeneral.add(new BoolSetting.Builder().name("Damage").description("Shows minimum damage override.").defaultValue(true).build());
    private final Setting<Boolean> duck = sgGeneral.add(new BoolSetting.Builder().name("Duck").description("Shows if duck peek assist is enabled.").defaultValue(true).build());
    private final Setting<Boolean> fps = sgGeneral.add(new BoolSetting.Builder().name("FPS").description("Shows your FPS.").defaultValue(true).build());
    private final Setting<Boolean> baim = sgGeneral.add(new BoolSetting.Builder().name("Baim").description("Shows if force body aim is enabled.").defaultValue(true).build());
    private final Setting<Boolean> safePoint = sgGeneral.add(new BoolSetting.Builder().name("Safe Point").description("Shows if force safe point is enabled.").defaultValue(true).build());
    private final Setting<Boolean> lagComp = sgGeneral.add(new BoolSetting.Builder().name("Lag Comp").description("Shows if lag compensation is enabled.").defaultValue(true).build());
    private final Setting<Boolean> doubleTap = sgGeneral.add(new BoolSetting.Builder().name("Double Tap").description("Shows if double tap is enabled.").defaultValue(true).build());
    private final Setting<Boolean> hide = sgGeneral.add(new BoolSetting.Builder().name("Hide").description("Shows if on-shot anti-aim is enabled.").defaultValue(true).build());
    private final Setting<SettingColor> defaultColor = sgGeneral.add(new ColorSetting.Builder().name("Default Color").description("The default color for the indicators.").defaultValue(new SettingColor(123, 194, 21, 255)).build());

    // Ping Indicator
    private final Setting<Boolean> ping = sgPing.add(new BoolSetting.Builder().name("Ping").description("Shows your ping.").defaultValue(true).build());
    private final Setting<Integer> yellowPing = sgPing.add(new IntSetting.Builder().name("Yellow Ping").description("The ping threshold for the yellow color.").defaultValue(10).min(1).sliderMax(100).build());
    private final Setting<Integer> redPing = sgPing.add(new IntSetting.Builder().name("Red Ping").description("The ping threshold for the red color.").defaultValue(100).min(1).sliderMax(200).build());
    private final Setting<SettingColor> greenColor = sgPing.add(new ColorSetting.Builder().name("Green Color").description("The color for stable ping.").defaultValue(new SettingColor(0, 255, 0, 255)).build());
    private final Setting<SettingColor> yellowColor = sgPing.add(new ColorSetting.Builder().name("Yellow Color").description("The color for slightly unstable ping.").defaultValue(new SettingColor(255, 255, 0, 255)).build());
    private final Setting<SettingColor> redColor = sgPing.add(new ColorSetting.Builder().name("Red Color").description("The color for unstable ping.").defaultValue(new SettingColor(255, 0, 0, 255)).build());

    // Bomb Indicator
    private final Setting<Boolean> bombIndicator = sgBomb.add(new BoolSetting.Builder().name("Bomb Indicator").description("Displays a timer for TNT entities.").defaultValue(true).build());
    private final Setting<SettingColor> bombColor = sgBomb.add(new ColorSetting.Builder().name("Bomb Color").description("The color for the bomb timer.").defaultValue(new SettingColor(123, 194, 21, 255)).build());

    // Fake Lag Indicator
    private final Setting<Boolean> fakeLag = sgFakeLag.add(new BoolSetting.Builder().name("Fake Lag").description("Shows a fake lag status.").defaultValue(true).build());
    private final Setting<SettingColor> fakeLagColor = sgFakeLag.add(new ColorSetting.Builder().name("Color").description("The color of the indicator.").defaultValue(new SettingColor(60, 120, 180, 255)).build());
    private final Setting<Integer> fakeLagMax = sgFakeLag.add(new IntSetting.Builder().name("Fake Max Ticks").description("The maximum value for the fake choked ticks.").defaultValue(14).min(1).sliderMax(30).build());

    private final Random random = new Random();
    private int fakeChokedTicks = 0;
    private int tickCounter = 0;
    private double averagePing = 0;
    private int pingSampleCount = 0;

    public Indicators() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        double height = 0;

        // General Indicators
        List<String> indicators = new ArrayList<>();
        if (damage.get()) indicators.add("DMG");
        if (duck.get()) indicators.add("DUCK");
        if (fps.get()) indicators.add("FPS");
        if (baim.get()) indicators.add("BAIM");
        if (safePoint.get()) indicators.add("SP");
        if (lagComp.get()) indicators.add("LC");
        if (doubleTap.get()) indicators.add("DT");
        if (hide.get()) indicators.add("HIDE");

        for (String text : indicators) {
            double textWidth = renderer.textWidth(text, true, 1.0);
            double textHeight = renderer.textHeight(true, 1.0);
            renderer.text(text, x, y + height, defaultColor.get(), true, 1.0);
            height += textHeight + 2;
        }

    if (mc.player != null) {
        PlayerListEntry playerEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
    if (playerEntry != null) {
        int currentPing = PingUtils.getPing();
        if (pingSampleCount < 100) {
            averagePing = (averagePing * pingSampleCount + currentPing) / (pingSampleCount + 1);
            pingSampleCount++;
        } else {
            averagePing = (averagePing * 0.95) + (currentPing * 0.05);
        }

        Color color;
        if (currentPing > averagePing + redPing.get()) {
            color = redColor.get();
        } else if (currentPing > averagePing + yellowPing.get()) {
            color = yellowColor.get();
        } else {
            color = greenColor.get();
        }

        String text = "PING";
        double textWidth = renderer.textWidth(text, true, 1.0);
        double textHeight = renderer.textHeight(true, 1.0);
        renderer.text(text, x, y + height, color, true, 1.0);
        height += textHeight + 2;
    }
}


        // Bomb Indicator
        if (bombIndicator.get() && mc.world != null) {
            TntEntity tnt = null;
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof TntEntity) {
                    tnt = (TntEntity) entity;
                    break;
                }
            }

            if (tnt != null) {
                int fuse = tnt.getFuse();
                String text = String.format("Bomb: %.1fs", fuse / 20.0);
                double textWidth = renderer.textWidth(text, true, 1.0);
                double textHeight = renderer.textHeight(true, 1.0);
                renderer.text(text, x, y + height, bombColor.get(), true, 1.0);
                height += textHeight + 2;
            }
        }

        // Fake Lag Indicator
        if (fakeLag.get()) {
            tickCounter++;
            if (tickCounter > 5) {
                fakeChokedTicks = random.nextInt(fakeLagMax.get() + 1);
                tickCounter = 0;
            }

            double radius = 8;
            double percentage = (double) fakeChokedTicks / fakeLagMax.get();

            String text = String.valueOf(fakeLagMax.get());
            double textWidth = renderer.textWidth(text, true, 1.0);
            double textHeight = renderer.textHeight(true, 1.0);

            renderer.text(text, x, y + height, fakeLagColor.get(), true, 1.0);
            drawCircle(renderer, x + textWidth + 10, y + height + textHeight / 2, radius, new Color(0, 0, 0, 200), 0, 1.0);
            drawCircle(renderer, x + textWidth + 10, y + height + textHeight / 2, radius - 1, fakeLagColor.get(), 0, percentage);
            height += textHeight + 2;
        }

        setSize(renderer.textWidth("DMG", true, 1.0), height);
    }

    private void drawCircle(HudRenderer renderer, double centerX, double centerY, double radius, Color color, double startAngle, double endAngle) {
        for (int i = (int) (startAngle * 360); i < (int) (endAngle * 360); i++) {
            double angle1 = Math.toRadians(i);
            double angle2 = Math.toRadians(i + 1);
            double x1 = centerX + Math.cos(angle1) * radius;
            double y1 = centerY + Math.sin(angle1) * radius;
            double x2 = centerX + Math.cos(angle2) * radius;
            double y2 = centerY + Math.sin(angle2) * radius;
            renderer.line(x1, y1, x2, y2, color);
        }
    }
}
