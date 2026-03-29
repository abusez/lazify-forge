package com.lazify;

import com.lazify.config.LazifyConfig;
import com.lazify.overlay.OverlayManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class EventHandler {

    private int tickCounter = 0;
    private boolean keyWasDown = false;

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
}
