package com.lazify.config;

import com.lazify.LazifyMod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

public class LazifyGuiConfig extends GuiConfig {

    private static final int DRAG_BUTTON_ID = 9999;

    public LazifyGuiConfig(GuiScreen parent) {
        super(parent, getConfigElements(), LazifyMod.MODID, false, false, "Lazify Settings");
    }

    @Override
    public void initGui() {
        super.initGui();
        // Add a "Drag Position" button at the top-right area
        this.buttonList.add(new GuiButton(DRAG_BUTTON_ID, this.width - 130, 2, 125, 16, "Drag Position..."));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == DRAG_BUTTON_ID) {
            mc.displayGuiScreen(new GuiOverlayPosition(this));
            return;
        }
        super.actionPerformed(button);
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<>();
        Configuration config = LazifyConfig.INSTANCE.getConfiguration();

        elements.add(new ConfigElement(config.getCategory("general")));
        elements.add(new ConfigElement(config.getCategory("columns")));
        elements.add(new ConfigElement(config.getCategory("position")));
        elements.add(new ConfigElement(config.getCategory("colors")));
        elements.add(new ConfigElement(config.getCategory("api")));

        return elements;
    }
}
