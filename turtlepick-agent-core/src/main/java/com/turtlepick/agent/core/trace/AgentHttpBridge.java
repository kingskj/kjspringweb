package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.util.AgentLog;

public final class AgentHttpBridge {

    private AgentHttpBridge() {
    }

    public static void install(AgentRuntimeController runtimeController) {
        AgentInternalRouter.install(runtimeController);
    }

    public static boolean safeIntercept(Object request, Object response) {
        if (!AgentInternalRouter.isInstalled()) {
            return false;
        }

        boolean internal;
        try {
            internal = AgentInternalRouter.isInternalRequest(request);
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            return false;
        }

        if (!internal) {
            return false;
        }

        try {
            AgentInternalRouter.handle(request, response);
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("filter intercept handle failed cause=" + t.getClass().getSimpleName());
            AgentInternalRouter.writeFailure(response);
        }
        return true;
    }

    public static boolean safeEnterOrHandle(Object request, Object response) {
        if (!AgentInternalRouter.isInstalled()) {
            HttpRequestContextBridge.safeEnter(request);
            return false;
        }

        boolean internal;
        try {
            internal = AgentInternalRouter.isInternalRequest(request);
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("agent http bridge check failed cause=" + t.getClass().getSimpleName());
            HttpRequestContextBridge.safeEnter(request);
            return false;
        }

        if (!internal) {
            HttpRequestContextBridge.safeEnter(request);
            return false;
        }

        try {
            AgentInternalRouter.handle(request, response);
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("resume handle failed cause=" + t.getClass().getSimpleName());
            AgentInternalRouter.writeFailure(response);
        }
        return true;
    }
}
