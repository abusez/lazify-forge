package com.lazify;

import com.lazify.command.CommandOv;
import com.lazify.overlay.OverlayManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = LazifyMod.MODID, name = LazifyMod.NAME, version = LazifyMod.VERSION, clientSideOnly = true)
public class LazifyMod {

    public static final String MODID   = "lazify";
    public static final String NAME    = "Lazify";
    public static final String VERSION = "3.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.Instance(MODID)
    public static LazifyMod instance;

    private File configDir;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = event.getModConfigurationDirectory();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new com.lazify.EventHandler());
        ClientCommandHandler.instance.registerCommand(new CommandOv());
        OverlayManager.INSTANCE.init(configDir);
    }
}
