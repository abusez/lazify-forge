package com.lazify;

import com.lazify.config.LazifyConfig;
import com.lazify.overlay.OverlayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import java.lang.reflect.Field;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class EventHandler {

    private int tickCounter = 0;
    private boolean keyWasDown = false;
    private static Field theSlotField;

    static {
        try {
            // theSlot field in GuiContainer (may be obfuscated as field_147006_u)
            for (Field f : GuiContainer.class.getDeclaredFields()) {
                if (f.getType() == Slot.class) {
                    f.setAccessible(true);
                    theSlotField = f;
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) {
            keyWasDown = false;
        } else {
            int keyCode = LazifyConfig.INSTANCE.getKeybind();
            boolean keyDown = keyCode >= 0 && Keyboard.isKeyDown(keyCode);

            if (LazifyConfig.INSTANCE.isKeybindHold()) {
                OverlayManager.INSTANCE.setVisible(keyDown);
            } else {
                if (keyDown && !keyWasDown) {
                    OverlayManager.INSTANCE.toggleVisible();
                }
            }
            keyWasDown = keyDown;
        }

        tickCounter++;
        if (tickCounter >= 5) {
            tickCounter = 0;
            OverlayManager.INSTANCE.onTick();
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;
        OverlayManager.INSTANCE.onRender();
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        String plain = event.message.getUnformattedText();
        boolean allow = OverlayManager.INSTANCE.onChat(plain);
        if (!allow) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && event.entity == mc.thePlayer) {
            OverlayManager.INSTANCE.onWorldChange();
        }
    }

    @SubscribeEvent
    public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!LazifyConfig.INSTANCE.isMiddleClickShop()) return;
        if (!Mouse.getEventButtonState()) return; // only on press, not release
        int button = Mouse.getEventButton();
        if (button != 0 && button != 1) return; // only intercept left/right click

        // Let shift-clicks pass through normally (needed for sorting Quick Buy)
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) return;

        if (!OverlayManager.INSTANCE.isInBedwars()) return;
        if (!(event.gui instanceof GuiChest)) return;

        GuiChest chest = (GuiChest) event.gui;
        // Check if the chest title looks like a BW shop
        String title = ((ContainerChest) chest.inventorySlots).getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!isBedwarsShop(title)) return;

        // Get the slot under the mouse
        Slot slot = null;
        if (theSlotField != null) {
            try { slot = (Slot) theSlotField.get(chest); } catch (Exception ignored) {}
        }
        if (slot == null || !slot.getHasStack()) return;

        // Cancel the original click and send a middle-click instead
        event.setCanceled(true);
        Minecraft mc = Minecraft.getMinecraft();
        mc.playerController.windowClick(
            chest.inventorySlots.windowId,
            slot.slotNumber,
            2, // middle click button
            3, // creative/pick mode
            mc.thePlayer
        );
    }

    private static boolean isBedwarsShop(String title) {
        if (title == null) return false;
        // Hypixel BW shop GUI titles
        return title.contains("Quick Buy")
            || title.contains("Blocks")
            || title.contains("Melee")
            || title.contains("Armor")
            || title.contains("Tools")
            || title.contains("Ranged")
            || title.contains("Potions")
            || title.contains("Utility")
            || title.contains("Shop");
    }
}
