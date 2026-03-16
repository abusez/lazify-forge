package com.lazify.overlay;

public class ColumnDef {

    public String display;
    public String header;
    public String key;
    public int width;
    public int maxwidth;
    public int position;
    public boolean enabled;

    public ColumnDef(String display, String header, String key, int width, int maxwidth, int position, boolean enabled) {
        this.display = display;
        this.header = header;
        this.key = key;
        this.width = width;
        this.maxwidth = maxwidth;
        this.position = position;
        this.enabled = enabled;
    }

    public String getDisplay() { return display; }
    public String getHeader() { return header; }
    public String getKey() { return key; }
    public int getWidth() { return width; }
    public int getMaxwidth() { return maxwidth; }
    public int getPosition() { return position; }
    public boolean isEnabled() { return enabled; }

    public void setDisplay(String display) { this.display = display; }
    public void setHeader(String header) { this.header = header; }
    public void setKey(String key) { this.key = key; }
    public void setWidth(int width) { this.width = width; }
    public void setMaxwidth(int maxwidth) { this.maxwidth = maxwidth; }
    public void setPosition(int position) { this.position = position; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
