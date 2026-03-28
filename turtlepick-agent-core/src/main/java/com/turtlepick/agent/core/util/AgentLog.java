package com.turtlepick.agent.core.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class AgentLog {

    private static final String PREFIX = "[TP-AGENT-BOOT]";
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    private AgentLog() {
    }

    public static void info(String message) {
        print("INFO", message, null);
    }

    public static void warn(String message) {
        print("WARN", message, null);
    }

    public static void error(String message) {
        print("ERROR", message, null);
    }

    public static void error(String message, Throwable throwable) {
        print("ERROR", message, throwable);
    }

    private static void print(String level, String message, Throwable throwable) {
        String timestamp = FORMAT.format(Instant.now());
        System.err.println(timestamp + " " + PREFIX + "[" + level + "] " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }
}
