package com.turtlepick.agent.core.config;

import com.turtlepick.agent.core.util.AgentLog;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class TurtlepickConfigLoader {

    private static final String CONFIG_SYSTEM_PROPERTY = "turtlepick.config";
    private static final String CONFIG_FILE_NAME = "turtlepick.properties";

    public AgentConfig load(File agentJarFile) {
        File configFile = resolveConfigFile(agentJarFile);
        Properties properties = loadProperties(configFile);
        AgentConfig config = bind(properties);
        validate(config);
        AgentLog.info("config loaded path=" + configFile.getAbsolutePath());
        return config;
    }

    File resolveConfigFile(File agentJarFile) {
        String explicitPath = trimToNull(System.getProperty(CONFIG_SYSTEM_PROPERTY));
        if (explicitPath != null) {
            File file = new File(explicitPath);
            if (file.isFile()) {
                return file;
            }
            throw new IllegalArgumentException("config file not found: " + file.getAbsolutePath());
        }

        File userDirFile = new File(System.getProperty("user.dir"), CONFIG_FILE_NAME);
        if (userDirFile.isFile()) {
            return userDirFile;
        }

        File agentJarDir = agentJarFile.getParentFile();
        if (agentJarDir != null) {
            File jarDirFile = new File(agentJarDir, CONFIG_FILE_NAME);
            if (jarDirFile.isFile()) {
                return jarDirFile;
            }
        }

        throw new IllegalArgumentException("turtlepick.properties not found");
    }

    Properties loadProperties(File configFile) {
        Properties properties = new Properties();
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8);
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("failed to load config: " + configFile.getAbsolutePath(), e);
        } finally {
            closeQuietly(reader);
        }
    }

    AgentConfig bind(Properties properties) {
        return AgentConfig.builder()
                .engineBaseUrl(getRequiredString(properties, "turtlepick.engine.base-url"))
                .engineMetaTimeoutMs(getInt(properties, "turtlepick.engine.meta.timeout-ms", 3000))
                .agentServerId(getRequiredString(properties, "turtlepick.agent.server-id"))
                .agentAppName(getRequiredString(properties, "turtlepick.agent.app-name"))
                .agentGitRepoRoot(getOptionalString(properties, "turtlepick.agent.git.repo-root", System.getProperty("user.dir")))
                .loggingDir(getOptionalString(properties, "turtlepick.agent.logging.dir", "./turtlepick-logs"))
                .rollingIntervalMinutes(getInt(properties, "turtlepick.agent.logging.rolling.interval-minutes", 5))
                .instrumentationHttp(getBoolean(properties, "turtlepick.agent.instrumentation.http", true))
                .instrumentationService(getBoolean(properties, "turtlepick.agent.instrumentation.service", true))
                .instrumentationSqlDatasourceProxy(getBoolean(properties, "turtlepick.agent.instrumentation.sql.datasource-proxy", true))
                .instrumentationSqlMybatisInterceptor(getBoolean(properties, "turtlepick.agent.instrumentation.sql.mybatis-interceptor", false))
                .build();
    }

    void validate(AgentConfig config) {
        if (isEmpty(config.getEngineBaseUrl())) {
            throw new IllegalArgumentException("turtlepick.engine.base-url is required");
        }
        if (isEmpty(config.getAgentServerId())) {
            throw new IllegalArgumentException("turtlepick.agent.server-id is required");
        }
        if (isEmpty(config.getAgentAppName())) {
            throw new IllegalArgumentException("turtlepick.agent.app-name is required");
        }
        if (config.getEngineMetaTimeoutMs() <= 0) {
            throw new IllegalArgumentException("turtlepick.engine.meta.timeout-ms must be > 0");
        }
    }

    private String getRequiredString(Properties properties, String key) {
        String value = trimToNull(properties.getProperty(key));
        if (value == null) {
            throw new IllegalArgumentException("missing required property: " + key);
        }
        return value;
    }

    private String getOptionalString(Properties properties, String key, String defaultValue) {
        String value = trimToNull(properties.getProperty(key));
        return value != null ? value : defaultValue;
    }

    private int getInt(Properties properties, String key, int defaultValue) {
        String value = trimToNull(properties.getProperty(key));
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid int property: " + key + "=" + value, e);
        }
    }

    private boolean getBoolean(Properties properties, String key, boolean defaultValue) {
        String value = trimToNull(properties.getProperty(key));
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
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
