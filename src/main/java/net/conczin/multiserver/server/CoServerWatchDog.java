package net.conczin.multiserver.server;

import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class CoServerWatchDog implements Runnable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MAX_SHUTDOWN_TIME = 10000L;
    private static final int SHUTDOWN_STATUS = 1;
    private final CoServerInstance server;
    private final long maxTickTime;

    public CoServerWatchDog(CoServerInstance dedicatedServer) {
        this.server = dedicatedServer;
        this.maxTickTime = dedicatedServer.getMaxTickLength();
    }

    @Override
    public void run() {
        while (this.server.isRunning()) {
            long l = this.server.getNextTickTime();
            long m = Util.getMillis();
            long n = m - l;
            if (n > this.maxTickTime) {
                LOGGER.error(LogUtils.FATAL_MARKER, "A single server tick took {} seconds (should be max {})", String.format(Locale.ROOT, "%.2f", Float.valueOf((float) n / 1000.0f)), String.format(Locale.ROOT, "%.2f", Float.valueOf(0.05f)));
                LOGGER.error(LogUtils.FATAL_MARKER, "Considering it to be crashed, server will forcibly shutdown.");
                ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
                StringBuilder stringBuilder = new StringBuilder();
                Error error = new Error("Watchdog");
                for (ThreadInfo threadInfo : threadInfos) {
                    if (threadInfo.getThreadId() == this.server.getRunningThread().getId()) {
                        error.setStackTrace(threadInfo.getStackTrace());
                    }
                    stringBuilder.append(threadInfo);
                    stringBuilder.append("\n");
                }
                CrashReport crashReport = new CrashReport("Watching Server", error);
                this.server.fillSystemReport(crashReport.getSystemReport());
                CrashReportCategory crashReportCategory = crashReport.addCategory("Thread Dump");
                crashReportCategory.setDetail("Threads", stringBuilder);
                CrashReportCategory crashReportCategory2 = crashReport.addCategory("Performance stats");
                crashReportCategory2.setDetail("Random tick rate", () -> this.server.getWorldData().getGameRules().getRule(GameRules.RULE_RANDOMTICKING).toString());
                crashReportCategory2.setDetail("Level stats", () -> Streams.stream(this.server.getAllLevels()).map(serverLevel -> serverLevel.dimension() + ": " + serverLevel.getWatchdogStats()).collect(Collectors.joining(",\n")));
                Bootstrap.realStdoutPrintln("Crash report:\n" + crashReport.getFriendlyReport());
                File file = new File(new File(this.server.getServerDirectory(), "crash-reports"), "crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
                if (crashReport.saveToFile(file)) {
                    LOGGER.error("This crash report has been saved to: {}", file.getAbsolutePath());
                } else {
                    LOGGER.error("We were unable to save this crash report to disk.");
                }
                this.exit();
            }
            try {
                Thread.sleep(l + this.maxTickTime - m);
            } catch (InterruptedException interruptedException) {
            }
        }
    }

    private void exit() {
        try {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    Runtime.getRuntime().halt(1);
                }
            }, 10000L);
            System.exit(1);
        } catch (Throwable throwable) {
            Runtime.getRuntime().halt(1);
        }
    }
}

