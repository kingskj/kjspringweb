package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.state.EndpointResolver;
import com.turtlepick.agent.core.state.ResolvedEndpoint;
import com.turtlepick.agent.core.util.AgentLog;

public final class RuntimeMethodBridge {

    private static volatile EndpointResolver endpointResolver;

    private RuntimeMethodBridge() {
    }

    public static void installEndpointResolver(EndpointResolver resolver) {
        endpointResolver = resolver;
    }

    public static void enter(int methodId, String fqcnMethod) {
        RuntimeTraceContext context = TraceContextHolder.getOrCreate();
        boolean root = context.isEmpty();
        context.push(methodId, fqcnMethod);

        if (root) {
            EndpointResolver resolver = endpointResolver;
            if (resolver != null) {
                HttpRequestContext httpRequestContext = HttpRequestContextHolder.get();
                ResolvedEndpoint resolvedEndpoint = resolver.resolve(methodId, httpRequestContext);
                context.attachResolvedEndpoint(resolvedEndpoint, httpRequestContext);
            }
        }
    }

    public static void exit(int methodId, boolean error) {
        RuntimeTraceContext context = TraceContextHolder.get();
        if (context == null) {
            return;
        }

        MethodFrame top = context.peek();
        if (top == null) {
            TraceContextHolder.clear();
            return;
        }

        if (top.getMethodId() != methodId) {
            AgentLog.warn("method stack mismatch expected=" + methodId + " actual=" + top.getMethodId());
            context.clear();
            TraceContextHolder.clear();
            return;
        }

        context.pop();

        if (context.isEmpty()) {
            try {
                long now = System.currentTimeMillis();
                String line = TraceLogSerializer.serialize(context, now);
                TraceLogWriter.write(line);
            } catch (Throwable t) {
                AgentLog.warn("trace flush failed methodId=" + context.getEntryMethodId()
                        + " cause=" + t.getClass().getSimpleName());
            } finally {
                TraceContextHolder.clear();
            }
        }
    }
}
