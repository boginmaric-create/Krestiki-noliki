package com.edgefan.edgerunners.client;

import com.edgefan.edgerunners.cyber.CyberSlot;
import com.edgefan.edgerunners.cyber.Cyberware;
import com.edgefan.edgerunners.item.CyberwareItem;
import com.edgefan.edgerunners.item.ModItems;
import com.edgefan.edgerunners.menu.RipperdocMenu;
import com.edgefan.edgerunners.network.ClientCyberData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RipperdocScreen extends AbstractContainerScreen<RipperdocMenu> {
    private static final int BG = 0xF0101018, PANEL = 0xFF1A1C26, SLOT = 0xFF2A2D38, EDGE = 0xFF454A5A;
    private static final int YELLOW = 0xFFFCEE0A, CYAN = 0xFF00F0FF, PINK = 0xFFFF2A6D, GRAY = 0xFF8A8F98, WHITE = 0xFFE8EAF0;
    private static final int ROW_Y = 50, ROW_H = 19;

    private Button installButton;
    private final Button[] removeButtons = new Button[CyberSlot.values().length];

    public RipperdocScreen(RipperdocMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        installButton = addRenderableWidget(Button.builder(Component.translatable("gui.edgerunners.install"),
                b -> click(RipperdocMenu.BUTTON_INSTALL)).bounds(leftPos + 34, topPos + 21, 64, 16).build());
        for (CyberSlot s : CyberSlot.values()) {
            int i = s.ordinal();
            removeButtons[i] = addRenderableWidget(Button.builder(Component.translatable("gui.edgerunners.remove"),
                    b -> click(1 + i)).bounds(leftPos + 126, topPos + ROW_Y + i * ROW_H + 1, 42, 16).build());
        }
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int mask = ClientCyberData.implants;
        ItemStack in = menu.inputStack();
        installButton.active = in.getItem() instanceof CyberwareItem && RipperdocMenu.blockReason(in, mask) == null;
        for (CyberSlot s : CyberSlot.values()) removeButtons[s.ordinal()].visible = s.installed(mask) != null;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.fill(x, y, x + imageWidth, y + 1, YELLOW);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, YELLOW);
        g.fill(x, y, x + 1, y + imageHeight, YELLOW);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, YELLOW);
        for (CyberSlot s : CyberSlot.values()) {
            int ry = y + ROW_Y + s.ordinal() * ROW_H;
            g.fill(x + 6, ry, x + imageWidth - 6, ry + ROW_H - 1, PANEL);
            g.fill(x + 6, ry, x + 7, ry + ROW_H - 1, s.installed(ClientCyberData.implants) != null ? CYAN : EDGE);
        }
        for (Slot s : menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, EDGE);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, SLOT);
        }
        // Psychosis bar with a preview of what the selected implant adds.
        int bx = x + 104, by = y + 30, bw = 64;
        float psy = ClientCyberData.psychosis;
        g.fill(bx, by, bx + bw, by + 5, 0xFF000000);
        g.fill(bx, by, bx + (int) (bw * Math.min(100, psy) / 100f), by + 5, psy >= 75 ? PINK : CYAN);
        if (menu.inputStack().getItem() instanceof CyberwareItem ci && RipperdocMenu.blockReason(menu.inputStack(), ClientCyberData.implants) == null) {
            int from = (int) (bw * Math.min(100, psy) / 100f), to = (int) (bw * Math.min(100, psy + ci.cyberware.load) / 100f);
            g.fill(bx + from, by, bx + to, by + 5, 0xC0FF2A6D);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, YELLOW, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, GRAY, false);
        g.drawString(font, Component.translatable("gui.edgerunners.psychosis", (int) ClientCyberData.psychosis), 104, 20, GRAY, false);

        int mask = ClientCyberData.implants;
        Component reason = RipperdocMenu.blockReason(menu.inputStack(), mask);
        if (reason != null) g.drawString(font, reason, 8, 40, PINK, false);
        else if (menu.inputStack().isEmpty()) g.drawString(font, Component.translatable("gui.edgerunners.hint"), 8, 40, 0xFF5A5F6E, false);

        for (CyberSlot s : CyberSlot.values()) {
            int ry = ROW_Y + s.ordinal() * ROW_H;
            Cyberware c = s.installed(mask);
            g.drawString(font, fit(Component.translatable("gui.edgerunners.slot." + s.id)), 30, ry + 2, GRAY, false);
            if (c != null) {
                g.renderItem(new ItemStack(ModItems.IMPLANTS.get(c).get()), 10, ry + 1);
                g.drawString(font, fit(Component.translatable("item.edgerunners." + c.id)), 30, ry + 10, WHITE, false);
            } else {
                g.drawString(font, Component.translatable("gui.edgerunners.empty"), 30, ry + 10, 0xFF5A5F6E, false);
            }
        }
    }

    /** Trims text so it never runs under the Remove button. */
    private String fit(Component text) {
        String s = text.getString();
        if (font.width(s) <= 94) return s;
        return font.plainSubstrByWidth(s, 94 - font.width("…")) + "…";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
