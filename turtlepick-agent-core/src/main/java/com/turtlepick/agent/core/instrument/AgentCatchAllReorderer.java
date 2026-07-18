package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.List;

final class AgentCatchAllReorderer {

    private AgentCatchAllReorderer() {
    }

    static void moveAgentCatchAllToEnd(
            MethodNode methodNode,
            Label agentHandlerLabel,
            String className,
            String methodName,
            String descriptor) {

        if (methodNode == null || agentHandlerLabel == null) {
            throw failure(className, methodName, descriptor, "missing_method_or_handler");
        }

        List<TryCatchBlockNode> blocks = methodNode.tryCatchBlocks;
        if (blocks == null || blocks.isEmpty()) {
            throw failure(className, methodName, descriptor, "try_catch_empty");
        }

        TryCatchBlockNode agentBlock = null;
        int agentIndex = -1;
        for (int i = 0; i < blocks.size(); i++) {
            TryCatchBlockNode block = blocks.get(i);
            if (isAgentCatchAll(block, agentHandlerLabel)) {
                if (agentBlock != null) {
                    throw failure(className, methodName, descriptor, "multiple_agent_catch_all");
                }
                agentBlock = block;
                agentIndex = i;
            }
        }

        if (agentBlock == null) {
            throw failure(className, methodName, descriptor, "agent_catch_all_missing");
        }

        if (agentIndex != blocks.size() - 1) {
            blocks.remove(agentIndex);
            blocks.add(agentBlock);
        }
        updateTryCatchIndexes(blocks);
    }

    private static boolean isAgentCatchAll(TryCatchBlockNode block, Label agentHandlerLabel) {
        Object labelInfo = agentHandlerLabel.info;
        return block != null
                && block.type == null
                && block.handler != null
                && (block.handler.getLabel() == agentHandlerLabel
                || (labelInfo instanceof LabelNode && block.handler == labelInfo));
    }

    private static void updateTryCatchIndexes(List<TryCatchBlockNode> blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).updateIndex(i);
        }
    }

    private static AgentCatchAllReorderException failure(
            String className,
            String methodName,
            String descriptor,
            String reason) {

        return new AgentCatchAllReorderException("agent catch-all reorder failed"
                + " className=" + className
                + " method=" + methodName
                + " descriptor=" + descriptor
                + " reason=" + reason);
    }
}
