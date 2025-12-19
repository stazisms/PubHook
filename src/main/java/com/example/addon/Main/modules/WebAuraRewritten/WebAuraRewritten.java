package com.example.addon.Main.modules.WebAuraRewritten;

import com.example.addon.Hook;
import com.example.addon.Hooked;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebAuraRewritten extends Hooked {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPrediction = settings.createGroup("Prediction");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<WebMode> webMode = sgGeneral.add(new EnumSetting.Builder<WebMode>()
        .name("placement-mode")
        .description("Where to place webs on the target.")
        .defaultValue(WebMode.Both)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The maximum distance to target players.")
        .defaultValue(4.0)
        .min(0).max(7)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("Maximum webs to place per tick.")
        .defaultValue(1)
        .min(1).max(5)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay-ms")
        .description("Delay between web placements in milliseconds.")
        .defaultValue(50)
        .min(0).max(500)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the webs when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiHole = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-hole")
        .description("Prevents placing webs if the target is in a hole.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> movePredict = sgPrediction.add(new BoolSetting.Builder()
        .name("movement-predict")
        .description("Predicts the target's movement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> extrapolationTicks = sgPrediction.add(new IntSetting.Builder()
        .name("extrapolation-ticks")
        .description("How many ticks of movement to predict.")
        .defaultValue(0)
        .min(0).max(100)
        .visible(movePredict::get)
        .build()
    );

    private final Setting<Boolean> elytraPredict = sgPrediction.add(new BoolSetting.Builder()
        .name("elytra-predict")
        .description("Predicts and places webs ahead of elytra jumpers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> advanceDistance = sgPrediction.add(new IntSetting.Builder()
        .name("advance-distance")
        .description("Number of webs to place in front of elytra jumpers.")
        .defaultValue(3)
        .min(1).max(10)
        .visible(elytraPredict::get)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the block where it is placing a web.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
        .name("render-time")
        .description("How long to render the placement.")
        .defaultValue(8)
        .min(1).max(20)
        .visible(render::get)
        .build()
    );

    private PlayerEntity target;
    private long lastPlaceTime;
    private int placedThisTick;
    private final List<RenderBlock> renderBlocks = Collections.synchronizedList(new ArrayList<>());

    public WebAuraRewritten() {
        super(Hook.CATEGORY, "WebAuraRewritten", "Automatically places webs on other players.");
    }

    @Override
    public void onActivate() {
        target = null;
        lastPlaceTime = 0;
        placedThisTick = 0;
        renderBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        placedThisTick = 0;
        renderBlocks.removeIf(RenderBlock::tick);

        if (mc.player == null || mc.world == null) return;

        target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target == null || Friends.get().isFriend(target)) return;

        if (antiHole.get() && isPlayerInHole(target)) return;

        BlockPos targetPos = getPredictedPos(target);

        if (elytraPredict.get() && handleElytraPrediction(targetPos)) return;

        placeWebs(targetPos);
    }

    private void placeWebs(BlockPos pos) {
        switch (webMode.get()) {
            case Head -> placeWeb(pos.up());
            case Feet -> placeWeb(pos);
            case Both -> {
                placeWeb(pos);
                placeWeb(pos.up());
            }
            case Below -> placeWeb(pos.down());
        }
    }

    private boolean placeWeb(BlockPos pos) {
        if (placedThisTick >= blocksPerTick.get()) return false;
        if (System.currentTimeMillis() - lastPlaceTime < placeDelay.get()) return false;

        if (BlockUtils.place(pos, InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 0, false)) {
            lastPlaceTime = System.currentTimeMillis();
            placedThisTick++;
            if (render.get()) {
                renderBlocks.add(new RenderBlock(pos, renderTime.get()));
            }
            return true;
        }
        return false;
    }

    private BlockPos getPredictedPos(PlayerEntity player) {
        if (!movePredict.get() || extrapolationTicks.get() == 0) {
            return player.getBlockPos();
        }
        Vec3d predictedMove = player.getVelocity().multiply(extrapolationTicks.get());
        return BlockPos.ofFloored(player.getPos().add(predictedMove));
    }

    private boolean handleElytraPrediction(BlockPos startPos) {
        Vec3d moveVec = target.getRotationVector().normalize().multiply(advanceDistance.get());
        BlockPos endPos = BlockPos.ofFloored(startPos.toCenterPos().add(moveVec));
        return placeWeb(endPos);
    }

    private boolean isPlayerInHole(PlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        return !mc.world.getBlockState(pos.down()).isAir()
            && !mc.world.getBlockState(pos.north()).isAir()
            && !mc.world.getBlockState(pos.south()).isAir()
            && !mc.world.getBlockState(pos.east()).isAir()
            && !mc.world.getBlockState(pos.west()).isAir();
    }

    @Override
    public String getInfoString() {
        if (target != null) {
            return EntityUtils.getName(target);
        }
        return null;
    }

    public enum WebMode {
        Head, Feet, Both, Below
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock(BlockPos pos, int ticks) {
            this.pos.set(pos);
            this.ticks = ticks;
        }

        public boolean tick() {
            ticks--;
            return ticks <= 0;
        }
    }
}
