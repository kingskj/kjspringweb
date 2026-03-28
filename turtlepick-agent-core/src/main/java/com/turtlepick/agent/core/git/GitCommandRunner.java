package com.turtlepick.agent.core.git;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class GitCommandRunner {

    public CommandResult run(File workingDir, List<String> command) {
        Process process = null;
        StreamCollector stdout = null;
        StreamCollector stderr = null;

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workingDir);
            process = builder.start();

            stdout = new StreamCollector(process.getInputStream());
            stderr = new StreamCollector(process.getErrorStream());
            stdout.start();
            stderr.start();

            int exitCode = process.waitFor();

            stdout.join();
            stderr.join();

            return new CommandResult(exitCode, stdout.getContent(), stderr.getContent());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("command interrupted: " + command, e);
        } catch (IOException e) {
            throw new IllegalStateException("failed to run command: " + command, e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static final class StreamCollector extends Thread {

        private final InputStream inputStream;
        private final StringBuilder content = new StringBuilder();

        private StreamCollector(InputStream inputStream) {
            this.inputStream = inputStream;
            setDaemon(true);
        }

        @Override
        public void run() {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String line;
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    if (!first) {
                        content.append('\n');
                    }
                    content.append(line);
                    first = false;
                }
            } catch (IOException ignore) {
            } finally {
                closeQuietly(reader);
            }
        }

        private String getContent() {
            return content.toString();
        }

        private void closeQuietly(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
