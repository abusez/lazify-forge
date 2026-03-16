package com.lazify;

import com.lazify.command.CommandOv;
import com.lazify.config.LazifyConfig;
import com.lazify.overlay.OverlayManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.lang.reflect.Field;

@Mod(modid = LazifyMod.MODID, name = LazifyMod.NAME, version = LazifyMod.VERSION, clientSideOnly = true)
public class LazifyMod {

    public static final String MODID   = "lazify";
    public static final String NAME    = "Lazify";
    public static final String VERSION = "3.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static KeyBinding OVERLAY_KEY;

    @Mod.Instance(MODID)
    public static LazifyMod instance;

    private File configDir;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = event.getModConfigurationDirectory();
        OVERLAY_KEY = new KeyBinding("key.lazify.toggle", Keyboard.KEY_GRAVE, "Lazify");
        ClientRegistry.registerKeyBinding(OVERLAY_KEY);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new com.lazify.EventHandler());
        ClientCommandHandler.instance.registerCommand(new CommandOv());
        OverlayManager.INSTANCE.init(configDir);
        setKeyCode(OVERLAY_KEY, LazifyConfig.INSTANCE.getKeybind());
    }

    public static void setKeyCode(KeyBinding kb, int code) {
        try {
            Field f = KeyBinding.class.getDeclaredField("keyCode");
            f.setAccessible(true);
            f.set(kb, code);
        } catch (Exception e) {
            LOGGER.warn("Could not set keybind code: {}", e.getMessage());
        }
    }
}
