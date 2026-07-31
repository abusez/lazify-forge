package com.lazify.config;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Save / load appearance packs under {@code <config>/lazify/presets/}.
 */
public final class AppearancePreset {

    private AppearancePreset() {}

    public static File presetsDir(File configDir) {
        File dir = new File(configDir, "lazify" + File.separator + "presets");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Sanitize a user-facing name into a safe filename stem. */
    public static String sanitizeName(String name) {
        if (name == null) return "";
        String s = name.trim().replaceAll("[^a-zA-Z0-9._\\- ]", "").replaceAll("\\s+", "_");
        if (s.length() > 48) s = s.substring(0, 48);
        return s;
    }

    public static File fileFor(File configDir, String name) {
        String stem = sanitizeName(name);
        if (stem.isEmpty()) return null;
        return new File(presetsDir(configDir), stem + ".properties");
    }

    public static List<String> listNames(File configDir) {
        File dir = presetsDir(configDir);
        File[] files = dir.listFiles();
        List<String> names = new ArrayList<>();
        if (files == null) return names;
        for (File f : files) {
            String n = f.getName();
            if (f.isFile() && n.endsWith(".properties")) {
                names.add(n.substring(0, n.length() - ".properties".length()));
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static void save(File configDir, String name) throws IOException {
        File out = fileFor(configDir, name);
        if (out == null) throw new IOException("Invalid preset name");
        Properties props = LazifyConfig.INSTANCE.exportAppearance();
        FileOutputStream fos = new FileOutputStream(out);
        try {
            props.store(fos, "Lazify appearance preset: " + sanitizeName(name));
        } finally {
            fos.close();
        }
    }

    public static void load(File configDir, String name) throws IOException {
        File in = fileFor(configDir, name);
        if (in == null || !in.isFile()) throw new IOException("Preset not found: " + name);
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(in);
        try {
            props.load(fis);
        } finally {
            fis.close();
        }
        LazifyConfig.INSTANCE.importAppearance(props);
        LazifyConfig.INSTANCE.save();
    }

    public static boolean delete(File configDir, String name) {
        File f = fileFor(configDir, name);
        return f != null && f.isFile() && f.delete();
    }

    /** Open the presets directory in the OS file manager. */
    public static void openFolder(File configDir) throws IOException {
        File dir = presetsDir(configDir);
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(dir);
                return;
            }
        }
        // Windows fallback
        Runtime.getRuntime().exec(new String[]{"explorer.exe", dir.getAbsolutePath()});
    }
}
