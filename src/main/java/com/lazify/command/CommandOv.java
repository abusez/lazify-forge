package com.lazify.command;

import com.lazify.config.LazifyConfig;
import com.lazify.overlay.OverlayManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandOv extends CommandBase {

    private static final String[] CONTROL_CMDS = {"sc","hide","clearhidden","reload","clear","key","set","tags"};
    private static final String[] KEY_TYPES     = {"urchin"};
    private static final String[] BOOL_VALS     = {"true","false"};

    @Override public String getCommandName()  { return "ov"; }
    @Override public String getCommandUsage(ICommandSender s) { return "/ov [setting] [value]"; }
    @Override public int    getRequiredPermissionLevel() { return 0; }
    @Override public List<String> getCommandAliases() { return Arrays.asList("overlay", "lazify"); }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        OverlayManager.INSTANCE.handleCommand(args);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        List<String> opts = new ArrayList<>();
        if (args.length == 0) return opts;

        String a0 = args[0].toLowerCase();

        if (args.length == 1) {
            for (String s : CONTROL_CMDS)              addIf(opts, s, a0);
            for (String s : OverlayManager.ALL_SETTINGS) addIf(opts, s, a0);
            if (LazifyConfig.INSTANCE.isDebug()) {
                addIf(opts, "debugsb", a0);
                addIf(opts, "debugtab", a0);
            }
            return opts;
        }

        // Handle 'set' as an alias that just shifts args
        String[] effective = args;
        if (effective[0].equalsIgnoreCase("set") && effective.length >= 2) {
            String[] shifted = new String[effective.length - 1];
            System.arraycopy(effective, 1, shifted, 0, shifted.length);
            effective = shifted;
            a0 = effective[0].toLowerCase();
        }

        if (effective.length == 2) {
            String a1 = effective[1].toLowerCase();
            switch (a0) {
                case "key":  for (String s : KEY_TYPES) addIf(opts, s, a1); break;
                case "col":  for (String s : OverlayManager.ALL_COLUMNS) addIf(opts, s, a1); break;
                case "sortby":   addIf(opts,"0",a1);addIf(opts,"1",a1);addIf(opts,"2",a1);
                                 addIf(opts,"3",a1);addIf(opts,"4",a1);addIf(opts,"5",a1); break;
                case "sortmode": addIf(opts,"0",a1); addIf(opts,"1",a1); break;
                case "winstreak":for (int i=0;i<=5;i++) addIf(opts,String.valueOf(i),a1); break;
                default:
                    for (String s : OverlayManager.ALL_SETTINGS) {
                        if (!s.equals(a0)) continue;
                        for (String b : BOOL_VALS) addIf(opts, b, a1);
                    }
            }
        } else if (effective.length == 3 && a0.equals("col")) {
            String a2 = effective[2].toLowerCase();
            for (String b : BOOL_VALS) addIf(opts, b, a2);
        }

        return opts;
    }

    private static void addIf(List<String> list, String candidate, String partial) {
        if (candidate.startsWith(partial)) list.add(candidate);
    }
}
