package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.config.AgentConfig;
import com.turtlepick.agent.core.http.CompletedTraceFile;
import com.turtlepick.agent.core.http.EngineLogReadyClient;
import com.turtlepick.agent.core.http.LogReadyResponse;
import com.turtlepick.agent.core.util.AgentLog;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class LogReadyNotifier {

    private static final int QUEUE_CAPACITY = 1024;
    private static final long RETRY_DELAY_MS = 1000L;

    private final BlockingQueue<CompletedTraceFile> queue = new LinkedBlockingQueue<CompletedTraceFile>(QUEUE_CAPACITY);
    private final EngineLogReadyClient client;
    private final AgentConfig config;
    private final String commitHash;
    private volatile Thread worker;

    public LogReadyNotifier(EngineLogReadyClient client, AgentConfig config, String commitHash) {
        this.client = client;
        this.config = config;
        this.commitHash = commitHash;
    }

    public synchronized void start() {
        if (worker != null) {
            return;
        }

        Thread thread = new Thread(this::runLoop, "turtlepick-log-ready-notifier");
        thread.setDaemon(true);
        worker = thread;
        thread.start();
    }

    public void shutdown() {
        Thread thread = worker;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void onClosed(String fileName) {
        String trimmedFileName = trimToNull(fileName);
        if (trimmedFileName == null) {
            return;
        }

        CompletedTraceFile file = new CompletedTraceFile(
                config.getAgentServerId(),
                commitHash,
                trimmedFileName
        );
        if (!queue.offer(file)) {
            AgentLog.warn("log_ready queue full fileName=" + trimmedFileName);
        }
    }

    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                CompletedTraceFile file = queue.take();
                sendWithRetry(file);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                AgentLog.warn("log_ready worker skipped cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            }
        }
    }

    private void sendWithRetry(CompletedTraceFile file) {
        LogReadyResponse response = client.send(file, config);
        if (response != null && response.isAccepted()) {
            AgentLog.info("log_ready ok fileName=" + file.getFileName() + " " + response.toLogDetail());
            return;
        }

        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AgentLog.warn("log_ready retry skipped fileName=" + file.getFileName() + " cause=INTERRUPTED");
            return;
        }

        response = client.send(file, config);
        if (response != null && response.isAccepted()) {
            AgentLog.info("log_ready retry ok fileName=" + file.getFileName() + " " + response.toLogDetail());
        } else {
            AgentLog.warn("log_ready failed fileName=" + file.getFileName()
                    + " " + (response == null ? "reason=NO_RESPONSE" : response.toLogDetail()));
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "" : throwable.getMessage();
    }
}
