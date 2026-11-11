package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.util.AgentLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * trace 파일 쓰기와 닫기. 닫힌 파일에는 같은 폴더에 "{파일명}.ready" 표시를 남기고,
 * 엔진은 HTTP 알림을 놓쳐도 그 표시를 근거로 회수한다.
 * 파일명은 기동마다 고유하며 기존 파일을 덮거나 이어 쓰지 않는다.
 */
public final class TraceLogWriter {

    static final String READY_SUFFIX = ".ready";
    static final int READY_VERSION = 1;

    private static final Object LOCK = new Object();
    private static final DateTimeFormatter FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneId.systemDefault());
    private static final String BOOT_ID = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private static final int MAX_CREATE_ATTEMPTS = 100;
    private static final long EXPIRY_CHECK_INTERVAL_MS = 10_000L;

    private static String loggingDir;
    private static int rollingIntervalMinutes;
    private static String commitHash;
    private static String serverId;
    private static String appName;
    private static boolean verboseFieldNames;
    private static long currentSlot = Long.MIN_VALUE;
    private static PrintWriter writer;
    private static String currentFileName;
    private static Path currentFile;
    private static String currentCommitHash;
    private static String currentServerId;
    private static LogReadyNotifier logReadyNotifier;
    private static int fileSequence;
    private static boolean shutdown;
    private static Thread expiryThread;

    private TraceLogWriter() {
    }

    public static void install(
            String loggingDirValue,
            int rollingIntervalMinutesValue,
            String commitHashValue,
            String serverIdValue,
            String appNameValue,
            boolean verboseFieldNamesValue,
            LogReadyNotifier notifier
    ) {
        synchronized (LOCK) {
            closeAndMark(true);

            loggingDir = trimToNull(loggingDirValue);
            rollingIntervalMinutes = rollingIntervalMinutesValue;
            commitHash = trimToNull(commitHashValue);
            serverId = trimToNull(serverIdValue);
            appName = trimToNull(appNameValue);
            verboseFieldNames = verboseFieldNamesValue;
            logReadyNotifier = notifier;

            if (loggingDir == null) {
                AgentLog.warn("trace log writer disabled cause=LOGGING_DIR_BLANK");
                return;
            }
            if (rollingIntervalMinutes <= 0) {
                AgentLog.warn("trace log writer disabled cause=ROLLING_INTERVAL_INVALID value=" + rollingIntervalMinutes);
                return;
            }
            if (commitHash == null) {
                AgentLog.warn("trace log writer disabled cause=COMMIT_HASH_BLANK");
                loggingDir = null;
                return;
            }
            if (serverId == null) {
                AgentLog.warn("trace log header serverId omitted cause=SERVER_ID_BLANK");
            }
            if (appName == null) {
                AgentLog.warn("trace log header appName omitted cause=APP_NAME_BLANK");
            }
            startExpiryThread();
        }
    }

    public static void write(RuntimeTraceContext context) {
        if (context == null) {
            return;
        }

        synchronized (LOCK) {
            if (shutdown || loggingDir == null || rollingIntervalMinutes <= 0 || commitHash == null) {
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

                String line = TraceLogSerializer.serialize(context, verboseFieldNames);
                writer.println(line);
                writer.flush();
            } catch (Throwable t) {
                AgentLog.warn("trace log write skipped cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            }
        }
    }

    /** 요청이 끊겨도 지난 슬롯의 파일을 닫아 표시를 남긴다. 새 파일은 다음 trace가 올 때 연다. */
    static void closeIfExpired() {
        synchronized (LOCK) {
            if (writer == null || rollingIntervalMinutes <= 0) {
                return;
            }
            long slot = System.currentTimeMillis() / (rollingIntervalMinutes * 60_000L);
            if (slot != currentSlot) {
                closeAndMark(true);
            }
        }
    }

    /** JVM 정상 종료 hook. 종료를 늦추지 않도록 HTTP 알림은 보내지 않고 표시까지만 확정한다. */
    public static void closeForShutdown() {
        synchronized (LOCK) {
            shutdown = true;
            closeAndMark(false);
        }
    }

    private static void rotateWriter(long now, long nextSlot) throws IOException {
        closeAndMark(true);

        File directory = new File(loggingDir);
        if (!directory.exists() && !directory.mkdirs()) {
            AgentLog.warn("trace log directory create skipped path=" + directory.getAbsolutePath());
            return;
        }
        if (!directory.isDirectory()) {
            AgentLog.warn("trace log path is not directory path=" + directory.getAbsolutePath());
            return;
        }

        String stamp = FILE_NAME_FORMAT.format(Instant.ofEpochMilli(now));
        for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
            fileSequence++;
            String nextFileName = "trace-" + stamp + "-" + BOOT_ID + "-" + fileSequence + ".log";
            Path targetFile = directory.toPath().resolve(nextFileName);
            OutputStream out;
            try {
                out = Files.newOutputStream(targetFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException e) {
                continue;
            }
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
            currentFileName = nextFileName;
            currentFile = targetFile;
            currentCommitHash = commitHash;
            currentServerId = serverId;
            currentSlot = nextSlot;
            writer.println(TraceLogSerializer.serializeHeader(commitHash, now, verboseFieldNames, serverId, appName));
            writer.flush();
            return;
        }
        AgentLog.warn("trace log file create skipped cause=NAME_EXHAUSTED stamp=" + stamp);
    }

    /**
     * 롤링·재설치·시간 만료·종료가 함께 쓰는 닫기. 쓰기·닫기 오류가 없을 때만 표시를 남기고,
     * 표시가 확정된 뒤에만 알림을 보낸다.
     */
    private static void closeAndMark(boolean notify) {
        PrintWriter closing = writer;
        String fileName = currentFileName;
        Path file = currentFile;
        String fileCommitHash = currentCommitHash;
        String fileServerId = currentServerId;
        writer = null;
        currentSlot = Long.MIN_VALUE;
        currentFileName = null;
        currentFile = null;
        currentCommitHash = null;
        currentServerId = null;
        if (closing == null) {
            return;
        }

        // PrintWriter는 쓰기·닫기 IOException을 던지지 않고 오류 표시만 남긴다.
        closing.close();
        if (closing.checkError()) {
            AgentLog.warn("trace log close failed; ready marker skipped fileName=" + fileName);
            return;
        }
        if (!writeReadyMarker(file, fileName, fileServerId, fileCommitHash)) {
            return;
        }
        if (notify) {
            notifyClosed(fileName);
        }
    }

    private static boolean writeReadyMarker(Path file, String fileName, String fileServerId, String fileCommitHash) {
        if (fileServerId == null) {
            AgentLog.warn("trace log ready marker skipped cause=SERVER_ID_BLANK fileName=" + fileName);
            return false;
        }
        Path part = file.resolveSibling(fileName + READY_SUFFIX + ".part");
        Path ready = file.resolveSibling(fileName + READY_SUFFIX);
        try {
            long size = Files.size(file);
            String content = "version=" + READY_VERSION + "\n"
                    + "serverId=" + fileServerId + "\n"
                    + "fileName=" + fileName + "\n"
                    + "commitHash=" + fileCommitHash + "\n"
                    + "size=" + size + "\n";
            Files.write(part, content.getBytes(StandardCharsets.UTF_8));
            Files.move(part, ready, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (Throwable t) {
            AgentLog.warn("trace log ready marker failed fileName=" + fileName
                    + " cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            try {
                Files.deleteIfExists(part);
            } catch (IOException ignore) {
            }
            return false;
        }
    }

    private static void startExpiryThread() {
        if (expiryThread != null) {
            return;
        }
        Thread thread = new Thread(TraceLogWriter::runExpiryLoop, "turtlepick-trace-expiry");
        thread.setDaemon(true);
        expiryThread = thread;
        thread.start();
    }

    private static void runExpiryLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(EXPIRY_CHECK_INTERVAL_MS);
                closeIfExpired();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                AgentLog.warn("trace log expiry check skipped cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            }
        }
    }

    private static void notifyClosed(String fileName) {
        LogReadyNotifier notifier = logReadyNotifier;
        if (notifier != null && fileName != null) {
            notifier.onClosed(fileName);
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
