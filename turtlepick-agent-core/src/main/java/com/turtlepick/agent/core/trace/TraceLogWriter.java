package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.util.AgentLog;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TraceLogWriter {

    private static final Object LOCK = new Object();
    private static final DateTimeFormatter FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneId.systemDefault());

    private static String loggingDir;
    private static int rollingIntervalMinutes;
    private static long currentSlot = Long.MIN_VALUE;
    private static PrintWriter writer;
    private static String currentFileName;
    private static LogReadyNotifier logReadyNotifier;

    private TraceLogWriter() {
    }

    public static void install(String loggingDirValue, int rollingIntervalMinutesValue) {
        install(loggingDirValue, rollingIntervalMinutesValue, null);
    }

    public static void install(String loggingDirValue, int rollingIntervalMinutesValue, LogReadyNotifier notifier) {
        synchronized (LOCK) {
            String closedFileName = currentFileName;
            closeCurrentWriter();
            notifyClosed(closedFileName);

            loggingDir = trimToNull(loggingDirValue);
            rollingIntervalMinutes = rollingIntervalMinutesValue;
            logReadyNotifier = notifier;
            currentSlot = Long.MIN_VALUE;
            currentFileName = null;

            if (loggingDir == null) {
                AgentLog.warn("trace log writer disabled cause=LOGGING_DIR_BLANK");
                return;
            }
            if (rollingIntervalMinutes <= 0) {
                AgentLog.warn("trace log writer disabled cause=ROLLING_INTERVAL_INVALID value=" + rollingIntervalMinutes);
                return;
            }
        }
    }

    public static void write(String line) {
        if (line == null) {
            return;
        }

        synchronized (LOCK) {
            if (loggingDir == null || rollingIntervalMinutes <= 0) {
                return;
            }

            try {
                long now = System.currentTimeMillis();
                long nextSlot = now / (rollingIntervalMinutes * 60_000L);
                if (writer == null || currentSlot != nextSlot) {
                    rotateWriter(now, nextSlot);
                }
                if (writer == null) {
                    return;
                }

                writer.println(line);
                writer.flush();
            } catch (Throwable t) {
                AgentLog.warn("trace log write skipped cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            }
        }
    }

    private static void rotateWriter(long now, long nextSlot) throws IOException {
        String closedFileName = currentFileName;
        closeCurrentWriter();
        notifyClosed(closedFileName);

        File directory = new File(loggingDir);
        if (!directory.exists() && !directory.mkdirs()) {
            AgentLog.warn("trace log directory create skipped path=" + directory.getAbsolutePath());
            return;
        }
        if (!directory.isDirectory()) {
            AgentLog.warn("trace log path is not directory path=" + directory.getAbsolutePath());
            return;
        }

        String nextFileName = "trace-" + FILE_NAME_FORMAT.format(Instant.ofEpochMilli(now)) + ".log";
        File targetFile = new File(directory, nextFileName);
        writer = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(targetFile, true), StandardCharsets.UTF_8)
        ));
        currentFileName = nextFileName;
        currentSlot = nextSlot;
    }

    private static void closeCurrentWriter() {
        closeQuietly(writer);
        writer = null;
        currentSlot = Long.MIN_VALUE;
        currentFileName = null;
    }

    private static void notifyClosed(String fileName) {
        LogReadyNotifier notifier = logReadyNotifier;
        if (notifier != null && fileName != null) {
            notifier.onClosed(fileName);
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "" : throwable.getMessage();
    }
}
