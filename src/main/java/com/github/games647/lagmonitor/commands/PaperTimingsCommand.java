package com.github.games647.lagmonitor.commands;

import co.aikar.timings.TimingHistory;
import co.aikar.timings.TimingsManager;

import com.avaje.ebeaninternal.api.ClassUtil;
import com.github.games647.lagmonitor.LagMonitor;
import com.github.games647.lagmonitor.traffic.Reflection;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * PaperSpigot and Sponge uses a new timings system (v2).
 * Missing data:
 * * TicksRecord
 * -> player ticks
 * -> timedTicks
 * -> entityTicks
 * -> activatedEntityTicks
 * -> tileEntityTicks
 * * MinuteReport
 * -> time
 * -> tps
 * -> avgPing
 * -> fullServerTick
 * -> ticks
 * * World data
 * -> worldName
 * -> tileEntities
 * -> entities
 *
 * => This concludes to the fact that the big benefits from Timings v2 isn't available. For example you cannot
 * scroll through your history
 */
public class PaperTimingsCommand implements CommandExecutor {

    private static final String TIMINGS_PACKAGE = "co.aikar.timings";

    private static final String EXPORT_CLASS = TIMINGS_PACKAGE + '.' + "TimingsExport";
    private static final String HANDLER_CLASS = TIMINGS_PACKAGE + '.' + "TimingHandler";
    private static final String HISTORY_ENTRY_CLASS = TIMINGS_PACKAGE + '.' + "TimingHistoryEntry";
    private static final String DATA_CLASS = TIMINGS_PACKAGE + '.' + "TimingData";

    private static final ChatColor PRIMARY_COLOR = ChatColor.DARK_AQUA;
    private static final ChatColor HEADER_COLOR = ChatColor.YELLOW;
    private static final ChatColor SECONDARY_COLOR = ChatColor.GRAY;

    private final LagMonitor plugin;

    public PaperTimingsCommand(LagMonitor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ClassUtil.isPresent(EXPORT_CLASS)) {
            sender.sendMessage(ChatColor.DARK_RED + "You aren't using PaperSpigot.");
            sender.sendMessage(ChatColor.DARK_RED + "This command is for the new timings (v2) system only");
            sender.sendMessage(ChatColor.DARK_RED + "Please use '/timing' for the old system");
            return true;
        }

        //modify timings settings dynamically?
//        Timings.setHistoryInterval(0);
//        Timings.setHistoryLength(0);
//        Timings.setVerboseTimingsEnabled(true);

        EvictingQueue<TimingHistory> history = Reflection.getField(TimingsManager.class, "HISTORY", EvictingQueue.class)
                .get(null);

        TimingHistory lastHistory = history.peek();
        if (lastHistory == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Not enough data yet");
        }

        //System
        printTimings(sender, lastHistory);
        return true;
    }

    public void printTimings(CommandSender sender, TimingHistory lastHistory) {

        long startTime = Reflection.getField(TimingHistory.class, "startTime", Long.TYPE).get(lastHistory);
        long endTime = Reflection.getField(TimingHistory.class, "endTime", Long.TYPE).get(lastHistory);

        long sampleTime = (endTime - startTime) / 1000;

        // Represents all time spent running the server this history
        long totalTime = Reflection.getField(TimingHistory.class, "totalTime", Long.TYPE).get(lastHistory);
        long totalTicks = Reflection.getField(TimingHistory.class, "totalTicks", Long.TYPE).get(lastHistory);

        long cost = (long) Reflection.getMethod(EXPORT_CLASS, "getCost").invoke(null);
        sender.sendMessage(PRIMARY_COLOR + "Cost: " + SECONDARY_COLOR + cost);
        sender.sendMessage(PRIMARY_COLOR + "Sample (sec): " + SECONDARY_COLOR + sampleTime);

        long playerTicks = TimingHistory.playerTicks;
        long tileEntityTicks = TimingHistory.tileEntityTicks;
        long activatedEntityTicks = TimingHistory.activatedEntityTicks;
        long entityTicks = TimingHistory.entityTicks;

        Collection<?> HANDLERS = Reflection.getField(TimingsManager.class, "HANDLERS", Collection.class)
                .get(null);
        Map<Integer, Object> idHandler = Maps.newHashMap();
        for (Object timingHandler : HANDLERS) {
            int id = Reflection.getField(HANDLER_CLASS, "id", Integer.TYPE).get(timingHandler);
            idHandler.put(id, timingHandler);
        }

        //TimingHistoryEntry
        Object[] entries = Reflection.getField(TimingHistory.class, "entries", Object[].class).get(lastHistory);
        for (Object entry : entries) {
            Object parentData = Reflection.getField(HISTORY_ENTRY_CLASS, "data", Object.class).get(entry);
            int childId = Reflection.getField(DATA_CLASS, "id", Integer.TYPE).get(parentData);

            Object handler = idHandler.get(childId);
            String parentName;
            if (handler == null) {
                parentName = "Unknown-" + childId;
            } else {
                parentName = Reflection.getField(HANDLER_CLASS, "name", String.class).get(handler);
            }

            int parentCount = Reflection.getField(DATA_CLASS, "count", Integer.TYPE).get(parentData);
            long parentTime = Reflection.getField(DATA_CLASS, "totalTime", Long.TYPE).get(parentData);

//            long parentLagCount = Reflection.getField(DATA_CLASS, "lagCount", Integer.TYPE).get(parentData);
//            long parentLagTime = Reflection.getField(DATA_CLASS, "lagTime", Long.TYPE).get(parentData);

            sender.sendMessage(HEADER_COLOR + parentName + " Count: " + parentCount + " Time: " + parentTime);

            Object[] children = Reflection.getField(HISTORY_ENTRY_CLASS, "children", Object[].class).get(entry);
            for (Object childData : children) {
                printChilds(parentData, childData, idHandler, sender);
            }
        }
    }

    private void printChilds(Object parent, Object childData, Map<Integer, Object> idMap, CommandSender sender) {
        int childId = Reflection.getField(DATA_CLASS, "id", Integer.TYPE).get(childData);

        Object handler = idMap.get(childId);
        String childName;
        if (handler == null) {
            childName = "Unknown-" + childId;
        } else {
            childName = Reflection.getField(HANDLER_CLASS, "name", String.class).get(handler);
        }

        int childCount = Reflection.getField(DATA_CLASS, "count", Integer.TYPE).get(childData);
        long childTime = Reflection.getField(DATA_CLASS, "totalTime", Long.TYPE).get(childData);

        sender.sendMessage(PRIMARY_COLOR + "    " + childName + " Count: " + childCount + " Time: " + childTime);
    }
}