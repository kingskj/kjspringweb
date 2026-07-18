package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

final class AgentCatchAllReorderingMethodNode extends MethodNode {

    private final MethodVisitor downstream;
    private final String className;
    private final String methodName;
    private final String descriptor;
    private final Label agentHandlerLabel;

    AgentCatchAllReorderingMethodNode(
            int api,
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions,
            MethodVisitor downstream,
            String className,
            Label agentHandlerLabel) {

        super(api, access, name, descriptor, signature, exceptions);
        this.downstream = downstream;
        this.className = className;
        this.methodName = name;
        this.descriptor = descriptor;
        this.agentHandlerLabel = agentHandlerLabel;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        AgentCatchAllReorderer.moveAgentCatchAllToEnd(
                this,
                agentHandlerLabel,
                className,
                methodName,
                descriptor);
        accept(downstream);
    }
}
