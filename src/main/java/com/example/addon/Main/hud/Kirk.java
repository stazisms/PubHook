package com.example.addon.Main.hud;

import com.example.addon.Hook;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.MathHelper;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Kirk extends HudElement {
    public static final HudElementInfo<Kirk> INFO = new HudElementInfo<>(Hook.HUD_GROUP, "kirk", "A memorial for Kirk.", Kirk::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .description("The messages to display. Use {days} for the day count.")
        .defaultValue(Arrays.asList(
            "Rest in peace kirk",
            "{days} days since you've been gone...",
            "kirk u were the meaning of lyfe",
            "{days} days since the last debate 💔",
            "Kirk ur legacy will never be gone",
            "LONG LIVE KIRK",
            "kirks last logged in {days} days ago"
        ))
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay in seconds before fading to the next message.")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> transitionDuration = sgGeneral.add(new DoubleSetting.Builder()
        .name("transition-duration")
        .description("The duration of the fade transition in seconds.")
        .defaultValue(1.0)
        .min(0.0)
        .sliderMax(5.0)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the text.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders a shadow behind the text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the text.")
        .defaultValue(1.0)
        .min(0.1)
        .sliderRange(0.1, 4.0)
        .build()
    );

    private static final LocalDate DATE_OF_DEATH = LocalDate.of(2025, 9, 10);
    private final Random random = new Random();

    private int currentMessageIndex = 0;
    private int nextMessageIndex = 0;
    private long lastSwitchTime;

    public Kirk() {
        super(INFO);
        lastSwitchTime = System.currentTimeMillis();
    }

    @Override
    public void render(HudRenderer renderer) {
        List<String> msgs = messages.get();
        if (msgs.isEmpty()) {
            setSize(0, 0);
            return;
        }

        long daysPassed = ChronoUnit.DAYS.between(DATE_OF_DEATH, LocalDate.now());
        if (daysPassed < 0) daysPassed = 0;

        long fullCycleTime = (long) ((delay.get() + transitionDuration.get()) * 1000L);
        long timeSinceLastSwitch = System.currentTimeMillis() - lastSwitchTime;

        // Time to switch to a new message
        if (timeSinceLastSwitch >= fullCycleTime) {
            lastSwitchTime = System.currentTimeMillis();
            timeSinceLastSwitch = 0;
            currentMessageIndex = nextMessageIndex;

            if (msgs.size() > 1) {
                do {
                    nextMessageIndex = random.nextInt(msgs.size());
                } while (nextMessageIndex == currentMessageIndex);
            }
        }

        String currentText = msgs.get(currentMessageIndex).replace("{days}", String.valueOf(daysPassed));
        String nextText = msgs.get(nextMessageIndex).replace("{days}", String.valueOf(daysPassed));

        double transitionProgress = 0;
        long transitionStartTime = delay.get() * 1000L;

        if (timeSinceLastSwitch > transitionStartTime && transitionDuration.get() > 0) {
            transitionProgress = Math.min(1.0, (timeSinceLastSwitch - transitionStartTime) / (transitionDuration.get() * 1000.0));
        }

        // Calculate size based on the wider of the two texts
        double currentWidth = renderer.textWidth(currentText, shadow.get(), scale.get());
        double nextWidth = renderer.textWidth(nextText, shadow.get(), scale.get());
        double textHeight = renderer.textHeight(shadow.get(), scale.get());
        setSize(Math.max(currentWidth, nextWidth), textHeight);

        // Render
        if (transitionProgress > 0) {
            // Fading out current text
            Color fadeOutColor = new Color(color.get()).a((int) (color.get().a * (1 - transitionProgress)));
            renderer.text(currentText, x, y, fadeOutColor, shadow.get(), scale.get());

            // Fading in next text
            Color fadeInColor = new Color(color.get()).a((int) (color.get().a * transitionProgress));
            renderer.text(nextText, x, y, fadeInColor, shadow.get(), scale.get());
        } else {
            // Render stable current text
            renderer.text(currentText, x, y, color.get(), shadow.get(), scale.get());
        }
    }
}
