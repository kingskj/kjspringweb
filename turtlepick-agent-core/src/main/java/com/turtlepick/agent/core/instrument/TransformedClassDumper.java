package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.util.AgentLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class TransformedClassDumper {

    private static final String ENABLED_PROPERTY = "turtlepick.agent.debug.dump-classes";
    private static final String DIR_PROPERTY = "turtlepick.agent.debug.dump-dir";
    private static final String FILTER_PROPERTY = "turtlepick.agent.debug.dump-filter";

    private TransformedClassDumper() {
    }

    static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"));
    }

    static void dump(String phase, String internalClassName, byte[] bytes) {
        if (!isEnabled() || bytes == null || bytes.length == 0 || !matchesFilter(internalClassName)) {
            return;
        }

        File dir = new File(System.getProperty(DIR_PROPERTY, "turtlepick-class-dump"));
        if (!dir.isDirectory() && !dir.mkdirs()) {
            AgentLog.warn("class dump skipped cause=MKDIR_FAILED dir=" + dir.getAbsolutePath());
            return;
        }

        File output = new File(dir, safeName(phase) + "-" + safeName(internalClassName) + ".class");
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(output);
            stream.write(bytes);
        } catch (IOException e) {
            AgentLog.warn("class dump skipped cause=" + e.getClass().getSimpleName()
                    + " path=" + output.getAbsolutePath());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static boolean matchesFilter(String internalClassName) {
        String filter = trimToNull(System.getProperty(FILTER_PROPERTY));
        if (filter == null) {
            return true;
        }

        String normalized = internalClassName == null ? "" : internalClassName.replace('/', '.');
        String[] parts = filter.split(",");
        for (int i = 0; i < parts.length; i++) {
            String item = trimToNull(parts[i]);
            if (item == null) {
                continue;
            }
            if ("*".equals(item) || item.equals(internalClassName) || item.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String safeName(String value) {
        String raw = value == null ? "unknown" : value;
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '.'
                    || ch == '-'
                    || ch == '_') {
                builder.append(ch);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
