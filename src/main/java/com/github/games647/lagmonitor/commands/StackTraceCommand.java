package com.github.games647.lagmonitor.commands;

import com.github.games647.lagmonitor.LagMonitor;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StackTraceCommand implements CommandExecutor {

    private static final ChatColor PRIMARY_COLOR = ChatColor.DARK_AQUA;
    private static final ChatColor SECONDARY_COLOR = ChatColor.GRAY;

    private final LagMonitor plugin;

    public StackTraceCommand(LagMonitor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String threadName = args[0];
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                if (thread.getName().equalsIgnoreCase(threadName)) {
                    StackTraceElement[] stackTrace = entry.getValue();
                    printStackTrace(sender, stackTrace);
                    return true;
                }
            }

            sender.sendMessage(ChatColor.DARK_RED + "No thread with that name found");
        } else {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            printStackTrace(sender, stackTrace);
        }

        return true;
    }

    private void printStackTrace(CommandSender sender, StackTraceElement[] stackTrace) {
        //begin from the top
        for (int i = stackTrace.length - 1; i >= 0; i--) {
            StackTraceElement traceElement = stackTrace[i];

            String className = traceElement.getClassName();
            String methodName = ChatColor.DARK_GREEN + traceElement.getMethodName();

            boolean nativeMethod = traceElement.isNativeMethod();
            int lineNumber = traceElement.getLineNumber();

            String line = Integer.toString(lineNumber);
            if (nativeMethod) {
                line = "Native";
            }

            sender.sendMessage(PRIMARY_COLOR + className + '.' + methodName + ':' + ChatColor.DARK_PURPLE + line);
        }
    }
}
