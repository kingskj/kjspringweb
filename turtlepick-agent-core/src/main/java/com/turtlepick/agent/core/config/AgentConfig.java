package com.turtlepick.agent.core.config;

public final class AgentConfig {

    private static final String[] DEFAULT_ERROR_ARGS_EXCLUDE_CLASSES = {
            "java.io.InputStream",
            "java.io.Reader",
            "java.io.File",
            "java.nio.ByteBuffer",
            "org.springframework.web.multipart.MultipartFile",
            "[B",
            "[C"
    };

    private final String engineBaseUrl;
    private final int engineMetaTimeoutMs;
    private final int engineLogReadyTimeoutMs;
    private final String agentServerId;
    private final String agentAppName;
    private final String agentGitRepoRoot;
    private final String loggingDir;
    private final int rollingIntervalMinutes;
    private final boolean verboseFieldNames;
    private final String[] userFramePackages;
    private final boolean errorArgsEnabled;
    private final int errorArgsMaxLength;
    private final String[] errorArgsExcludeClasses;
    private final BusinessErrorConfig businessErrorConfig;
    private final boolean instrumentationHttp;
    private final boolean instrumentationService;
    private final boolean instrumentationSqlDatasourceProxy;
    private final boolean instrumentationSqlMybatisInterceptor;

    private AgentConfig(Builder builder) {
        this.engineBaseUrl = builder.engineBaseUrl;
        this.engineMetaTimeoutMs = builder.engineMetaTimeoutMs;
        this.engineLogReadyTimeoutMs = builder.engineLogReadyTimeoutMs;
        this.agentServerId = builder.agentServerId;
        this.agentAppName = builder.agentAppName;
        this.agentGitRepoRoot = builder.agentGitRepoRoot;
        this.loggingDir = builder.loggingDir;
        this.rollingIntervalMinutes = builder.rollingIntervalMinutes;
        this.verboseFieldNames = builder.verboseFieldNames;
        this.userFramePackages = copyOf(builder.userFramePackages);
        this.errorArgsEnabled = builder.errorArgsEnabled;
        this.errorArgsMaxLength = builder.errorArgsMaxLength;
        this.errorArgsExcludeClasses = copyOf(builder.errorArgsExcludeClasses);
        this.businessErrorConfig = builder.businessErrorConfig == null
                ? BusinessErrorConfig.disabled()
                : builder.businessErrorConfig;
        this.instrumentationHttp = builder.instrumentationHttp;
        this.instrumentationService = builder.instrumentationService;
        this.instrumentationSqlDatasourceProxy = builder.instrumentationSqlDatasourceProxy;
        this.instrumentationSqlMybatisInterceptor = builder.instrumentationSqlMybatisInterceptor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static String[] defaultErrorArgsExcludeClasses() {
        return copyOf(DEFAULT_ERROR_ARGS_EXCLUDE_CLASSES);
    }

    public String getEngineBaseUrl() {
        return engineBaseUrl;
    }

    public int getEngineMetaTimeoutMs() {
        return engineMetaTimeoutMs;
    }

    public int getEngineLogReadyTimeoutMs() {
        return engineLogReadyTimeoutMs;
    }

    public String getAgentServerId() {
        return agentServerId;
    }

    public String getAgentAppName() {
        return agentAppName;
    }

    public String getAgentGitRepoRoot() {
        return agentGitRepoRoot;
    }

    public String getLoggingDir() {
        return loggingDir;
    }

    public int getRollingIntervalMinutes() {
        return rollingIntervalMinutes;
    }

    public boolean isVerboseFieldNames() {
        return verboseFieldNames;
    }

    public String[] getUserFramePackages() {
        return copyOf(userFramePackages);
    }

    public boolean isErrorArgsEnabled() {
        return errorArgsEnabled;
    }

    public int getErrorArgsMaxLength() {
        return errorArgsMaxLength;
    }

    public String[] getErrorArgsExcludeClasses() {
        return copyOf(errorArgsExcludeClasses);
    }

    public BusinessErrorConfig getBusinessErrorConfig() {
        return businessErrorConfig;
    }

    public boolean isInstrumentationHttp() {
        return instrumentationHttp;
    }

    public boolean isInstrumentationService() {
        return instrumentationService;
    }

    public boolean isInstrumentationSqlDatasourceProxy() {
        return instrumentationSqlDatasourceProxy;
    }

    public boolean isInstrumentationSqlMybatisInterceptor() {
        return instrumentationSqlMybatisInterceptor;
    }

    public static final class Builder {

        private String engineBaseUrl;
        private int engineMetaTimeoutMs = 3000;
        private int engineLogReadyTimeoutMs = 3000;
        private String agentServerId;
        private String agentAppName;
        private String agentGitRepoRoot;
        private String loggingDir = "./turtlepick-logs";
        private int rollingIntervalMinutes = 5;
        private boolean verboseFieldNames = false;
        private String[] userFramePackages = new String[0];
        private boolean errorArgsEnabled = true;
        private int errorArgsMaxLength = 10000;
        private String[] errorArgsExcludeClasses = AgentConfig.defaultErrorArgsExcludeClasses();
        private BusinessErrorConfig businessErrorConfig = BusinessErrorConfig.disabled();
        private boolean instrumentationHttp = true;
        private boolean instrumentationService = true;
        private boolean instrumentationSqlDatasourceProxy = true;
        private boolean instrumentationSqlMybatisInterceptor = false;

        private Builder() {
        }

        public Builder engineBaseUrl(String value) {
            this.engineBaseUrl = value;
            return this;
        }

        public Builder engineMetaTimeoutMs(int value) {
            this.engineMetaTimeoutMs = value;
            return this;
        }

        public Builder engineLogReadyTimeoutMs(int value) {
            this.engineLogReadyTimeoutMs = value;
            return this;
        }

        public Builder agentServerId(String value) {
            this.agentServerId = value;
            return this;
        }

        public Builder agentAppName(String value) {
            this.agentAppName = value;
            return this;
        }

        public Builder agentGitRepoRoot(String value) {
            this.agentGitRepoRoot = value;
            return this;
        }

        public Builder loggingDir(String value) {
            this.loggingDir = value;
            return this;
        }

        public Builder rollingIntervalMinutes(int value) {
            this.rollingIntervalMinutes = value;
            return this;
        }

        public Builder verboseFieldNames(boolean value) {
            this.verboseFieldNames = value;
            return this;
        }

        public Builder userFramePackages(String[] value) {
            this.userFramePackages = copyOf(value);
            return this;
        }

        public Builder errorArgsEnabled(boolean value) {
            this.errorArgsEnabled = value;
            return this;
        }

        public Builder errorArgsMaxLength(int value) {
            this.errorArgsMaxLength = value;
            return this;
        }

        public Builder errorArgsExcludeClasses(String[] value) {
            this.errorArgsExcludeClasses = copyOf(value);
            return this;
        }

        public Builder businessErrorConfig(BusinessErrorConfig value) {
            this.businessErrorConfig = value == null ? BusinessErrorConfig.disabled() : value;
            return this;
        }

        public Builder instrumentationHttp(boolean value) {
            this.instrumentationHttp = value;
            return this;
        }

        public Builder instrumentationService(boolean value) {
            this.instrumentationService = value;
            return this;
        }

        public Builder instrumentationSqlDatasourceProxy(boolean value) {
            this.instrumentationSqlDatasourceProxy = value;
            return this;
        }

        public Builder instrumentationSqlMybatisInterceptor(boolean value) {
            this.instrumentationSqlMybatisInterceptor = value;
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(this);
        }
    }

    private static String[] copyOf(String[] value) {
        if (value == null || value.length == 0) {
            return new String[0];
        }
        String[] copy = new String[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return copy;
    }
}

