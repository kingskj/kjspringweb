package com.turtlepick.agent.core.config;

import com.turtlepick.agent.core.util.AgentLog;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class TurtlepickConfigLoader {

    private static final String CONFIG_SYSTEM_PROPERTY = "turtlepick.config";
    private static final String CONFIG_FILE_NAME = "turtlepick.properties";
    private static final String BUSINESS_ERROR_STATUS_KEY =
            "turtlepick.agent.business-error.exclude-http-statuses";
    private static final String BUSINESS_ERROR_RULE_PREFIX =
            "turtlepick.agent.business-error.rules.";
    private static final String SLOW_ENABLED_KEY =
            "turtlepick.agent.slow.enabled";
    private static final String SLOW_THRESHOLD_KEY =
            "turtlepick.agent.slow.threshold-ms";
    private static final String SLOW_EXCLUDE_PREFIX =
            "turtlepick.agent.slow.excludes.";
    private static final String SQL_CAPTURE_ENABLED_KEY =
            "turtlepick.agent.sql.capture.enabled";
    private static final String SQL_CAPTURE_MAX_BIND_VALUE_LENGTH_KEY =
            "turtlepick.agent.sql.capture.max-bind-value-length";

    public AgentConfig load(File agentJarFile) {
        File configFile = resolveConfigFile(agentJarFile);
        Properties properties = loadProperties(configFile);
        AgentConfig config = bind(properties);
        validate(config);
        logBusinessErrorSummary(config.getBusinessErrorConfig());
        logSlowTraceSummary(config.getSlowTraceConfig());
        logSqlCaptureSummary(config.getSqlCaptureConfig());
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
        int metaTimeoutMs = getInt(properties, "turtlepick.engine.meta.timeout-ms", 3000);
        return AgentConfig.builder()
                .engineBaseUrl(getRequiredString(properties, "turtlepick.engine.base-url"))
                .engineMetaTimeoutMs(metaTimeoutMs)
                .engineLogReadyTimeoutMs(getInt(properties, "turtlepick.engine.log-ready.timeout-ms", metaTimeoutMs))
                .agentServerId(getRequiredString(properties, "turtlepick.agent.server-id"))
                .agentAppName(getRequiredString(properties, "turtlepick.agent.app-name"))
                .agentGitRepoRoot(getOptionalString(properties, "turtlepick.agent.git.repo-root", System.getProperty("user.dir")))
                .loggingDir(getOptionalString(properties, "turtlepick.agent.logging.dir", "./turtlepick-logs"))
                .rollingIntervalMinutes(getInt(properties, "turtlepick.agent.logging.rolling.interval-minutes", 5))
                .verboseFieldNames(getBoolean(properties, "turtlepick.agent.logging.verbose-field-names", false))
                .userFramePackages(getStringArray(properties, "turtlepick.agent.error.user-frame-packages"))
                .errorArgsEnabled(getBoolean(properties, "turtlepick.agent.error.args.enabled", true))
                .errorArgsMaxLength(getInt(properties, "turtlepick.agent.error.args.max-length", 10000))
                .errorArgsExcludeClasses(getStringArray(
                        properties,
                        "turtlepick.agent.error.args.exclude-classes",
                        AgentConfig.defaultErrorArgsExcludeClasses()
                ))
                .businessErrorConfig(parseBusinessErrorConfig(properties))
                .slowTraceConfig(parseSlowTraceConfig(properties))
                .sqlCaptureConfig(parseSqlCaptureConfig(properties))
                .instrumentationHttp(getBoolean(properties, "turtlepick.agent.instrumentation.http", true))
                .instrumentationService(getBoolean(properties, "turtlepick.agent.instrumentation.service", true))
                .instrumentationSqlDatasourceProxy(getBoolean(properties, "turtlepick.agent.instrumentation.sql.datasource-proxy", true))
                .instrumentationSqlMybatisInterceptor(getBoolean(properties, "turtlepick.agent.instrumentation.sql.mybatis-interceptor", false))
                .build();
    }

    private BusinessErrorConfig parseBusinessErrorConfig(Properties properties) {
        Set<Integer> statuses = parseExcludeHttpStatuses(properties.getProperty(BUSINESS_ERROR_STATUS_KEY));
        Map<String, RuleDraft> drafts = new LinkedHashMap<String, RuleDraft>();

        for (Object keyObject : properties.keySet()) {
            String key = String.valueOf(keyObject);
            if (!key.startsWith(BUSINESS_ERROR_RULE_PREFIX)) {
                continue;
            }
            String suffix = key.substring(BUSINESS_ERROR_RULE_PREFIX.length());
            int dot = suffix.lastIndexOf('.');
            if (dot <= 0 || dot == suffix.length() - 1) {
                AgentLog.warn("business-error rule ignored key=" + key + " cause=INVALID_RULE_KEY");
                continue;
            }

            String ruleId = trimToNull(suffix.substring(0, dot));
            String field = trimToNull(suffix.substring(dot + 1));
            if (ruleId == null || field == null) {
                AgentLog.warn("business-error rule ignored key=" + key + " cause=INVALID_RULE_KEY");
                continue;
            }
            if (!isBusinessRuleField(field)) {
                AgentLog.warn("business-error rule field ignored"
                        + " ruleId=" + ruleId
                        + " field=" + field
                        + " cause=UNKNOWN_FIELD");
                continue;
            }

            RuleDraft draft = drafts.get(ruleId);
            if (draft == null) {
                draft = new RuleDraft(ruleId);
                drafts.put(ruleId, draft);
            }
            draft.put(field, properties.getProperty(key));
        }

        List<BusinessErrorRule> rules = new ArrayList<BusinessErrorRule>();
        for (RuleDraft draft : drafts.values()) {
            BusinessErrorRule rule = draft.toRule();
            if (rule.isValid()) {
                rules.add(rule);
            } else {
                AgentLog.warn("business-error rule ignored"
                        + " ruleId=" + draft.ruleId
                        + " cause=INCOMPLETE_RULE");
            }
        }
        return new BusinessErrorConfig(statuses, rules);
    }

    private Set<Integer> parseExcludeHttpStatuses(String value) {
        LinkedHashSet<Integer> statuses = new LinkedHashSet<Integer>();
        String normalized = trimToNull(value);
        if (normalized == null) {
            return statuses;
        }

        String[] parts = normalized.split(",");
        for (int i = 0; i < parts.length; i++) {
            String part = trimToNull(parts[i]);
            if (part == null) {
                continue;
            }
            try {
                int status = Integer.parseInt(part);
                if (status >= 500 && status <= 599) {
                    AgentLog.warn("business-error http status ignored status=" + status + " cause=FIVE_XX_GUARD");
                    continue;
                }
                if (status < 100 || status > 599) {
                    AgentLog.warn("business-error http status ignored status=" + status + " cause=INVALID_STATUS");
                    continue;
                }
                statuses.add(Integer.valueOf(status));
            } catch (NumberFormatException e) {
                AgentLog.warn("business-error http status ignored value=" + part + " cause=INVALID_INT");
            }
        }
        return statuses;
    }

    private boolean isBusinessRuleField(String field) {
        return "exception-class".equals(field)
                || "code-accessor".equals(field)
                || "exclude-codes".equals(field);
    }

    private SlowTraceConfig parseSlowTraceConfig(Properties properties) {
        boolean enabled = getBoolean(properties, SLOW_ENABLED_KEY, false);
        if (!enabled) {
            return SlowTraceConfig.disabled();
        }

        Integer thresholdMs = parseSlowThreshold(properties.getProperty(SLOW_THRESHOLD_KEY));
        if (thresholdMs == null) {
            return SlowTraceConfig.disabled();
        }

        Map<String, SlowRuleDraft> drafts = new LinkedHashMap<String, SlowRuleDraft>();
        for (Object keyObject : properties.keySet()) {
            String key = String.valueOf(keyObject);
            if (!key.startsWith(SLOW_EXCLUDE_PREFIX)) {
                continue;
            }
            String suffix = key.substring(SLOW_EXCLUDE_PREFIX.length());
            int dot = suffix.lastIndexOf('.');
            if (dot <= 0 || dot == suffix.length() - 1) {
                AgentLog.warn("slow exclude rule ignored key=" + key + " cause=INVALID_RULE_KEY");
                continue;
            }

            String ruleId = trimToNull(suffix.substring(0, dot));
            String field = trimToNull(suffix.substring(dot + 1));
            if (ruleId == null || field == null) {
                AgentLog.warn("slow exclude rule ignored key=" + key + " cause=INVALID_RULE_KEY");
                continue;
            }
            if (!isRuleIdValid(ruleId)) {
                AgentLog.warn("slow exclude rule ignored"
                        + " ruleId=" + ruleId
                        + " cause=INVALID_RULE_ID");
                continue;
            }
            if (!isSlowExcludeField(field)) {
                AgentLog.warn("slow exclude rule field ignored"
                        + " ruleId=" + ruleId
                        + " field=" + field
                        + " cause=UNKNOWN_FIELD");
                continue;
            }

            SlowRuleDraft draft = drafts.get(ruleId);
            if (draft == null) {
                draft = new SlowRuleDraft(ruleId);
                drafts.put(ruleId, draft);
            }
            draft.put(field, properties.getProperty(key));
        }

        List<SlowExcludeRule> rules = new ArrayList<SlowExcludeRule>();
        for (SlowRuleDraft draft : drafts.values()) {
            SlowExcludeRule rule = draft.toRule();
            if (rule.isValid()) {
                rules.add(rule);
            } else {
                AgentLog.warn("slow exclude rule ignored"
                        + " ruleId=" + draft.ruleId
                        + " cause=INVALID_SELECTOR_COUNT");
            }
        }
        return new SlowTraceConfig(true, thresholdMs.intValue(), rules);
    }

    private SqlCaptureConfig parseSqlCaptureConfig(Properties properties) {
        boolean enabled = getBoolean(properties, SQL_CAPTURE_ENABLED_KEY, false);
        int maxBindValueLength = getInt(properties, SQL_CAPTURE_MAX_BIND_VALUE_LENGTH_KEY, 512);
        if (maxBindValueLength <= 0) {
            AgentLog.warn("sql capture max bind length defaulted"
                    + " value=" + maxBindValueLength
                    + " default=512");
            maxBindValueLength = 512;
        }
        return new SqlCaptureConfig(enabled, maxBindValueLength);
    }

    private Integer parseSlowThreshold(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            AgentLog.warn("slow trace disabled cause=THRESHOLD_MISSING");
            return null;
        }
        try {
            int thresholdMs = Integer.parseInt(normalized);
            if (thresholdMs <= 0) {
                AgentLog.warn("slow trace disabled thresholdMs=" + thresholdMs + " cause=THRESHOLD_INVALID");
                return null;
            }
            return Integer.valueOf(thresholdMs);
        } catch (NumberFormatException e) {
            AgentLog.warn("slow trace disabled value=" + normalized + " cause=THRESHOLD_INVALID");
            return null;
        }
    }

    private boolean isSlowExcludeField(String field) {
        return "endpoint-id".equals(field) || "method".equals(field);
    }

    private boolean isRuleIdValid(String ruleId) {
        if (ruleId == null || ruleId.length() == 0) {
            return false;
        }
        for (int i = 0; i < ruleId.length(); i++) {
            char ch = ruleId.charAt(i);
            boolean valid = (ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '-';
            if (!valid) {
                return false;
            }
        }
        return true;
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
        if (config.getEngineLogReadyTimeoutMs() <= 0) {
            throw new IllegalArgumentException("turtlepick.engine.log-ready.timeout-ms must be > 0");
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

    private String[] getStringArray(Properties properties, String key) {
        return getStringArray(properties, key, new String[0]);
    }

    private String[] getStringArray(Properties properties, String key, String[] defaultValue) {
        String value = trimToNull(properties.getProperty(key));
        if (value == null) {
            return copyOf(defaultValue);
        }
        String[] parts = value.split(",");
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < parts.length; i++) {
            String item = normalizeClassPattern(trimToNull(parts[i]));
            if (item != null) {
                values.add(item);
            }
        }
        return values.toArray(new String[values.size()]);
    }

    private Set<String> getStringSet(String value) {
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        String normalized = trimToNull(value);
        if (normalized == null) {
            return values;
        }
        String[] parts = normalized.split(",");
        for (int i = 0; i < parts.length; i++) {
            String item = trimToNull(parts[i]);
            if (item != null) {
                values.add(item);
            }
        }
        return values;
    }

    private void logBusinessErrorSummary(BusinessErrorConfig config) {
        BusinessErrorConfig value = config == null ? BusinessErrorConfig.disabled() : config;
        AgentLog.info("business-error rules loaded"
                + " statuses=" + value.getExcludeHttpStatuses()
                + " rules=" + value.getRules().size()
                + " ruleIds=" + value.describeRuleIds());
    }

    private void logSlowTraceSummary(SlowTraceConfig config) {
        SlowTraceConfig value = config == null ? SlowTraceConfig.disabled() : config;
        AgentLog.info("slow trace loaded"
                + " enabled=" + value.isEnabled()
                + " thresholdMs=" + value.getThresholdMs()
                + " excludes=" + value.getExcludes().size()
                + " ruleIds=" + value.describeRuleIds());
    }

    private void logSqlCaptureSummary(SqlCaptureConfig config) {
        SqlCaptureConfig value = config == null ? SqlCaptureConfig.disabled() : config;
        AgentLog.info("sql capture loaded"
                + " enabled=" + value.isEnabled()
                + " maxBindValueLength=" + value.getMaxBindValueLength());
    }

    private String normalizeClassPattern(String value) {
        if ("byte[]".equals(value)) {
            return "[B";
        }
        if ("char[]".equals(value)) {
            return "[C";
        }
        return value;
    }

    private String[] copyOf(String[] value) {
        if (value == null || value.length == 0) {
            return new String[0];
        }
        String[] copy = new String[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return copy;
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

    private final class RuleDraft {

        private final String ruleId;
        private String exceptionClassName;
        private String codeAccessor;
        private Set<String> excludeCodes = new LinkedHashSet<String>();

        private RuleDraft(String ruleId) {
            this.ruleId = ruleId;
        }

        private void put(String field, String value) {
            if ("exception-class".equals(field)) {
                exceptionClassName = trimToNull(value);
            } else if ("code-accessor".equals(field)) {
                codeAccessor = trimToNull(value);
            } else if ("exclude-codes".equals(field)) {
                excludeCodes = getStringSet(value);
            }
        }

        private BusinessErrorRule toRule() {
            return new BusinessErrorRule(ruleId, exceptionClassName, codeAccessor, excludeCodes);
        }
    }

    private final class SlowRuleDraft {

        private final String ruleId;
        private String endpointId;
        private String method;

        private SlowRuleDraft(String ruleId) {
            this.ruleId = ruleId;
        }

        private void put(String field, String value) {
            if ("endpoint-id".equals(field)) {
                endpointId = trimToNull(value);
            } else if ("method".equals(field)) {
                method = trimToNull(value);
            }
        }

        private SlowExcludeRule toRule() {
            return new SlowExcludeRule(ruleId, endpointId, method);
        }
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

