package com.edgefan.edgerunners.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyBinds {
    public static final String CATEGORY = "key.categories.edgerunners";
    public static final KeyMapping SANDEVISTAN = key("sandevistan", GLFW.GLFW_KEY_V);
    public static final KeyMapping MONOWIRE = key("monowire", GLFW.GLFW_KEY_R);
    public static final KeyMapping SCAN = key("scan", GLFW.GLFW_KEY_B);
    public static final KeyMapping HORN = key("horn", GLFW.GLFW_KEY_H);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.edgerunners." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    private KeyBinds() {}
}
