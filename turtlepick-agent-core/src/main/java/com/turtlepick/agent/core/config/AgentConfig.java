package com.turtlepick.agent.core.config;

public final class AgentConfig {

    private final String engineBaseUrl;
    private final int engineMetaTimeoutMs;
    private final String agentServerId;
    private final String agentAppName;
    private final String agentGitRepoRoot;
    private final String loggingDir;
    private final int rollingIntervalMinutes;
    private final boolean instrumentationHttp;
    private final boolean instrumentationService;
    private final boolean instrumentationSqlDatasourceProxy;
    private final boolean instrumentationSqlMybatisInterceptor;

    private AgentConfig(Builder builder) {
        this.engineBaseUrl = builder.engineBaseUrl;
        this.engineMetaTimeoutMs = builder.engineMetaTimeoutMs;
        this.agentServerId = builder.agentServerId;
        this.agentAppName = builder.agentAppName;
        this.agentGitRepoRoot = builder.agentGitRepoRoot;
        this.loggingDir = builder.loggingDir;
        this.rollingIntervalMinutes = builder.rollingIntervalMinutes;
        this.instrumentationHttp = builder.instrumentationHttp;
        this.instrumentationService = builder.instrumentationService;
        this.instrumentationSqlDatasourceProxy = builder.instrumentationSqlDatasourceProxy;
        this.instrumentationSqlMybatisInterceptor = builder.instrumentationSqlMybatisInterceptor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEngineBaseUrl() {
        return engineBaseUrl;
    }

    public int getEngineMetaTimeoutMs() {
        return engineMetaTimeoutMs;
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
        private String agentServerId;
        private String agentAppName;
        private String agentGitRepoRoot;
        private String loggingDir = "./turtlepick-logs";
        private int rollingIntervalMinutes = 5;
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
}
