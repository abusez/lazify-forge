package com.lazify;

import com.lazify.config.GuiClickMenu;
import com.lazify.config.LazifyConfig;
import com.lazify.overlay.MellowTabOverlay;
import com.lazify.overlay.OverlayManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import java.lang.reflect.Field;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
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
            syncOverlayKeybindFromControls();
            boolean keyDown = LazifyMod.overlayKeybind.isKeyDown();

            if (LazifyConfig.INSTANCE.isKeybindHold()) {
                OverlayManager.INSTANCE.setVisible(keyDown);
            } else {
                if (keyDown && !keyWasDown) {
                    OverlayManager.INSTANCE.toggleVisible();
                }
            }
            keyWasDown = keyDown;

            // Keybind opens click GUI
            if (LazifyMod.guiKeybind.isPressed()) {
                mc.displayGuiScreen(new GuiClickMenu());
            }
        }

        // /ov|/lazify with no args schedules open after chat closes
        OverlayManager.INSTANCE.tickPendingClickGui();

        // NoHurtCam: suppress damage camera tilt
        if (LazifyConfig.INSTANCE.isNoHurtCam() && mc.thePlayer != null) {
            mc.thePlayer.hurtTime = 0;
        }

        // AntiDebuff: remove visual debuff effects
        if (LazifyConfig.INSTANCE.isAntiDebuff() && mc.thePlayer != null) {
            if (mc.thePlayer.isPotionActive(9))  mc.thePlayer.removePotionEffect(9);  // nausea
            if (mc.thePlayer.isPotionActive(15)) mc.thePlayer.removePotionEffect(15); // blindness
        }

        if (MellowTabOverlay.isActive() && mc.gameSettings != null
                && mc.gameSettings.keyBindPlayerList.isKeyDown()) {
            int wheel = Mouse.getDWheel();
            if (wheel != 0 && mc.getNetHandler() != null) {
                MellowTabOverlay.handleMouseWheel(wheel, mc.getNetHandler().getPlayerInfoMap().size());
            }
        }

        tickCounter++;
        if (tickCounter >= 5) {
            tickCounter = 0;
            OverlayManager.INSTANCE.onTick();
        }
    }

    /** Replace vanilla tab list with Mellow extended table when that theme is active. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderTabPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return;
        if (!MellowTabOverlay.isActive()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings == null || !mc.gameSettings.keyBindPlayerList.isKeyDown()) return;

        event.setCanceled(true);
        MellowTabOverlay.render(OverlayManager.INSTANCE);
    }

    /** Render HUD overlay after tab list. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        OverlayManager.INSTANCE.onRender();
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        String plain = event.message.getUnformattedText();
        String formatted = event.message.getFormattedText();
        boolean allow = OverlayManager.INSTANCE.onChat(plain, formatted);
        if (!allow) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.PlaceEvent event) {
        if (!OverlayManager.INSTANCE.isInBedwars() && !LazifyConfig.INSTANCE.isDebug()) return;
        EntityPlayer player = event.player;
        IBlockState state = event.state;
        if (player == null || state == null) return;
        OverlayManager.INSTANCE.onBlockPlaced(player, state, event.pos);
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

    @SubscribeEvent
    public void onFOVUpdate(FOVUpdateEvent event) {
        if (!LazifyConfig.INSTANCE.isAntiDebuff()) return;
        // Counteract slowness FOV reduction: vanilla applies fov *= 1 - (amplifier+1) * 0.075
        if (event.entity.isPotionActive(Potion.moveSlowdown)) {
            PotionEffect effect = event.entity.getActivePotionEffect(Potion.moveSlowdown);
            if (effect != null) {
                float slowFactor = 1.0F - (effect.getAmplifier() + 1) * 0.075F;
                if (slowFactor > 0) event.newfov /= slowFactor;
            }
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!event.modID.equals(LazifyMod.MODID)) return;
        LazifyConfig.INSTANCE.syncFromFile();
        LazifyConfig.INSTANCE.save();
        OverlayManager.INSTANCE.defaultSettings();
    }

    /** Persist Controls-menu rebinds back to lazify.cfg. */
    private static void syncOverlayKeybindFromControls() {
        if (LazifyMod.overlayKeybind == null) return;
        int code = LazifyMod.overlayKeybind.getKeyCode();
        LazifyConfig cfg = LazifyConfig.INSTANCE;
        if (code != cfg.getKeybind()) {
            cfg.setKeybind(code);
            cfg.save();
        }
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
