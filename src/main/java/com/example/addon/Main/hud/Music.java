package com.example.addon.Main.hud;

import com.example.addon.Hook;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Music extends HudElement {
    public static final HudElementInfo<Music> INFO = new HudElementInfo<>(Hook.HUD_GROUP, "music", "Displays the currently playing song by scanning window titles.", Music::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgNotif = settings.createGroup("Notification");

    // General Settings
    private final Setting<List<String>> artists = sgGeneral.add(new StringListSetting.Builder()
        .name("artists")
        .description("The list of artists to detect from window titles.")
        .defaultValue(Arrays.asList("nettspend", "bleood", "wifiskeleton", "zayguapkid", "ksuuvi", "xaviersobased", "osamason", "bladee", "chief keef", "yuke", "jaydes", "dream caster", "woody", "jack frost", "twotimer", "blueface", "lil tjay", "ken carson", "destroy lonley", "playboy carti", "ratbowl", "boolymon", "phreshboyswag", "maxon", "snakechildpain", "fimiguerrero", "brennan jones", "edward skeletrix", "lucki", "lifelessgarments"))
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

    // Notification Settings
    private final Setting<List<String>> notificationMessages = sgNotif.add(new StringListSetting.Builder()
        .name("notification-messages")
        .description("The messages to display when a new song plays. Use {song} for the song name.")
        .defaultValue(Arrays.asList(
            "W song choice, now playing {song}",
            "Now Playing: {song}"
        ))
        .build()
    );

    private final Setting<Double> notificationDuration = sgNotif.add(new DoubleSetting.Builder()
        .name("notification-duration")
        .description("How long the notification is displayed in seconds.")
        .defaultValue(3.0)
        .min(0.0)
        .build()
    );

    private final Setting<Double> fadeDuration = sgNotif.add(new DoubleSetting.Builder()
        .name("fade-duration")
        .description("The duration of the fade-in and fade-out animation in seconds.")
        .defaultValue(0.5)
        .min(0.0)
        .build()
    );

    private final Random random = new Random();
    private String lastSong = "";
    private String currentSong = "";
    private String notificationText = "";
    private long notificationStartTime = 0;
    private long lastCheckTime = 0;

    public Music() {
        super(INFO);
    }

    // JNA Interface for extended User32 functions
    private interface User32Extra extends User32 {
        User32Extra INSTANCE = Native.load("user32", User32Extra.class);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
    }

    @Override
    public void render(HudRenderer renderer) {
        long now = System.currentTimeMillis();

        // Scan for music only once per second to save performance
        if (now - lastCheckTime > 1000) {
            lastCheckTime = now;
            currentSong = findMusicWindowTitle();
        }

        if (currentSong == null || currentSong.isEmpty()) {
            setSize(0, 0);
            return;
        }

        // Check for song change
        if (!currentSong.equals(lastSong)) {
            lastSong = currentSong;
            List<String> notifs = notificationMessages.get();
            if (!notifs.isEmpty()) {
                notificationText = notifs.get(random.nextInt(notifs.size())).replace("{song}", currentSong);
                notificationStartTime = System.currentTimeMillis();
            }
        }

        // Handle notification display
        long timeSinceNotif = System.currentTimeMillis() - notificationStartTime;
        double notifDurationMs = notificationDuration.get() * 1000;
        double fadeDurationMs = fadeDuration.get() * 1000;

        if (timeSinceNotif < notifDurationMs) {
            double alpha;
            if (timeSinceNotif < fadeDurationMs) {
                alpha = (double) timeSinceNotif / fadeDurationMs;
            } else if (timeSinceNotif > notifDurationMs - fadeDurationMs) {
                alpha = 1.0 - ((timeSinceNotif - (notifDurationMs - fadeDurationMs)) / fadeDurationMs);
            } else {
                alpha = 1.0;
            }

            Color notifColor = new Color(color.get()).a((int) (color.get().a * alpha));
            setSize(renderer.textWidth(notificationText, shadow.get(), scale.get()), renderer.textHeight(shadow.get(), scale.get()));
            renderer.text(notificationText, x, y, notifColor, shadow.get(), scale.get());
        }
        // Display current song info
        else {
            setSize(renderer.textWidth(currentSong, shadow.get(), scale.get()), renderer.textHeight(shadow.get(), scale.get()));
            renderer.text(currentSong, x, y, color.get(), shadow.get(), scale.get());
        }
    }

    private String findMusicWindowTitle() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            return "Window scanning is only supported on Windows.";
        }

        final String[] foundTitle = {null};
        List<String> artistList = artists.get();

        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            if (User32.INSTANCE.IsWindowVisible(hWnd)) {
                byte[] windowTextBuffer = new byte[512];
                User32Extra.INSTANCE.GetWindowTextA(hWnd, windowTextBuffer, 512);
                String windowTitle = Native.toString(windowTextBuffer);

                if (!windowTitle.isEmpty()) {
                    for (String artist : artistList) {
                        if (windowTitle.toLowerCase().contains(artist.toLowerCase())) {
                            foundTitle[0] = windowTitle;
                            return false; // Stop enumerating
                        }
                    }
                }
            }
            return true; // Continue enumerating
        }, null);

        return foundTitle[0];
    }
}
