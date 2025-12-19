package com.example.addon.Api.mixin;

import com.example.addon.Api.util.ColorUtils;
import com.example.addon.BuildInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {

    private static boolean animationFinished = false;
    private long startTime = 0;

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (animationFinished) {
            context.drawTextWithShadow(textRenderer, Formatting.DARK_PURPLE + "pubhook (sour2k) edition " + " " + BuildInfo.BUILD_TIME, 0, 20, ColorUtils.getRGBA());
            return;
        }

        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }

        long time = System.currentTimeMillis() - startTime;
        float duration = 4000;
        float fadeInTime = 1000;
        float scaleTime = 2000;
        float fadeOutTime = 1000;

        context.fill(0, 0, this.width, this.height, 0xFF000000);

        float alpha = 0;
        float scale = 1.0f;
        String text = "Welcome to pubhook";

        if (time < fadeInTime) {
            alpha = MathHelper.clamp(time / fadeInTime, 0, 1);
        } else if (time < fadeInTime + scaleTime) {
            alpha = 1;
            float scaleProgress = (time - fadeInTime) / scaleTime;
            scale = 1.0f + (float) (Math.sin(scaleProgress * Math.PI) * 0.5f);
        } else if (time < duration) {
            alpha = 1.0f - MathHelper.clamp((time - (fadeInTime + scaleTime)) / fadeOutTime, 0, 1);
        } else {
            animationFinished = true;
            ci.cancel();
            return;
        }

        int textWidth = textRenderer.getWidth(text);
        int x = this.width / 2;
        int y = this.height / 2;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(textRenderer, text, -textWidth / 2, -textRenderer.fontHeight / 2, ((int) (alpha * 255) << 24) | 0xFFFFFF);
        context.getMatrices().popMatrix();

        ci.cancel();
    }
}
