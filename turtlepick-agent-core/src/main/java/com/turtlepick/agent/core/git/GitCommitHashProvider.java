package com.turtlepick.agent.core.git;

import com.turtlepick.agent.core.config.AgentConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class GitCommitHashProvider {

    private static final Pattern FULL_HASH_PATTERN = Pattern.compile("^[0-9a-f]{40}$");

    private final GitCommandRunner gitCommandRunner;

    public GitCommitHashProvider(GitCommandRunner gitCommandRunner) {
        this.gitCommandRunner = gitCommandRunner;
    }

    public String resolveFullCommitHash(AgentConfig config) {
        File repoRoot = resolveRepoRoot(config);

        List<String> command = new ArrayList<String>();
        command.add("git");
        command.add("-C");
        command.add(repoRoot.getAbsolutePath());
        command.add("rev-parse");
        command.add("HEAD");

        CommandResult result = gitCommandRunner.run(repoRoot, command);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("git rev-parse HEAD failed: " + result.getStderr());
        }

        return normalizeAndValidate(result.getStdout());
    }

    File resolveRepoRoot(AgentConfig config) {
        String configuredPath = config.getAgentGitRepoRoot();
        File repoRoot;
        if (configuredPath == null || configuredPath.trim().length() == 0) {
            repoRoot = new File(System.getProperty("user.dir"));
        } else {
            repoRoot = new File(configuredPath);
            if (!repoRoot.isAbsolute()) {
                repoRoot = new File(System.getProperty("user.dir"), configuredPath);
            }
        }

        try {
            return repoRoot.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException("failed to resolve repo root: " + repoRoot.getPath(), e);
        }
    }

    String normalizeAndValidate(String rawHash) {
        if (rawHash == null) {
            throw new IllegalArgumentException("git hash is null");
        }
        String normalized = rawHash.trim().toLowerCase(Locale.ROOT);
        if (!FULL_HASH_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid full commit hash: " + rawHash);
        }
        return normalized;
    }
}
