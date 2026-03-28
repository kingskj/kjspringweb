package com.turtlepick.agent.core.state;

import java.util.concurrent.atomic.AtomicReference;

public final class AgentStateHolder {

    private final AtomicReference<AgentState> state =
            new AtomicReference<AgentState>(AgentState.LOG_OFF);

    public AgentState get() {
        return state.get();
    }

    public boolean isLogOn() {
        return state.get() == AgentState.LOG_ON;
    }

    public boolean isLogOff() {
        return state.get() == AgentState.LOG_OFF;
    }

    public void markLogOn() {
        state.set(AgentState.LOG_ON);
    }

    public void markLogOff() {
        state.set(AgentState.LOG_OFF);
    }
}
