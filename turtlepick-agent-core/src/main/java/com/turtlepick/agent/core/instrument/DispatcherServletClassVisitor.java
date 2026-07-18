package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class DispatcherServletClassVisitor extends ClassVisitor {

    private static final String JAVAX_DISPATCH_DESC =
            "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V";
    private static final String JAKARTA_DISPATCH_DESC =
            "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V";
    private static final String DISPATCHER_SERVLET_CLASS =
            "org.springframework.web.servlet.DispatcherServlet";

    private final boolean reorderAgentCatchAll;

    public DispatcherServletClassVisitor(ClassVisitor classVisitor) {
        this(classVisitor, false);
    }

    public DispatcherServletClassVisitor(ClassVisitor classVisitor, boolean reorderAgentCatchAll) {
        super(Opcodes.ASM9, classVisitor);
        this.reorderAgentCatchAll = reorderAgentCatchAll;
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {

        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ("doDispatch".equals(name)
                && (JAVAX_DISPATCH_DESC.equals(descriptor) || JAKARTA_DISPATCH_DESC.equals(descriptor))) {
            Label handlerLabel = new Label();
            MethodVisitor targetMethodVisitor = reorderAgentCatchAll
                    ? new AgentCatchAllReorderingMethodNode(
                            Opcodes.ASM9,
                            access,
                            name,
                            descriptor,
                            signature,
                            exceptions,
                            methodVisitor,
                            DISPATCHER_SERVLET_CLASS,
                            handlerLabel)
                    : methodVisitor;
            return new DispatcherServletDoDispatchAdapter(targetMethodVisitor, access, name, descriptor, handlerLabel);
        }
        return methodVisitor;
    }
}
