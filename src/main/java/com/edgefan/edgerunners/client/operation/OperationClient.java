package com.edgefan.edgerunners.client.operation;

import com.edgefan.edgerunners.block.ChairSeatEntity;
import com.edgefan.edgerunners.block.RipperdocChairBlockEntity;
import com.edgefan.edgerunners.client.fx.FxManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** The patient's side of an operation: cinematic camera, heartbeat and the end flash. */
public final class OperationClient {
    private static CameraType savedCamera;
    private static boolean active;
    private static int lastBeat;
    public static int beatFlash;

    private OperationClient() {}

    public static RipperdocChairBlockEntity current(Minecraft mc) {
        if (mc.player == null || mc.level == null || !(mc.player.getVehicle() instanceof ChairSeatEntity seat)) return null;
        return mc.level.getBlockEntity(seat.blockPosition()) instanceof RipperdocChairBlockEntity be && be.busy() ? be : null;
    }

    public static boolean active() { return active; }

    public static void tick(Minecraft mc) {
        RipperdocChairBlockEntity be = current(mc);
        if (be != null && !active) {
            active = true;
            savedCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            lastBeat = 0;
        } else if (be == null && active) {
            active = false;
            if (savedCamera != null) mc.options.setCameraType(savedCamera);
            savedCamera = null;
            FxManager.flash(14);
            FxManager.shake(6, 1.5f);
        }
        if (beatFlash > 0) beatFlash--;
        if (!active || mc.player == null) return;
        float p = be.progress(0);
        int interval = Math.max(8, (int) (22 - 12 * p));
        if (mc.player.tickCount - lastBeat >= interval) {
            lastBeat = mc.player.tickCount;
            beatFlash = 5;
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 1.0f, 0.9f + 0.4f * p, false);
        }
        boolean surgery = p > RipperdocChairBlockEntity.DROP_END && p < RipperdocChairBlockEntity.SURGERY_END;
        if (surgery && mc.player.tickCount % 12 == 0) FxManager.shake(4, 0.6f);
    }
}
