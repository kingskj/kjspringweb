package com.turtlepick.agent.core.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class SpringWebRequestTransformer implements ClassFileTransformer {

    private static final String DISPATCHER_SERVLET = "org/springframework/web/servlet/DispatcherServlet";

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (loader == null || className == null || !DISPATCHER_SERVLET.equals(className)) {
            return null;
        }

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new SafeClassWriter(reader);
        ClassVisitor visitor = new DispatcherServletClassVisitor(writer);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static final class SafeClassWriter extends ClassWriter {

        private SafeClassWriter(ClassReader classReader) {
            super(classReader, ClassWriter.COMPUTE_FRAMES);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            if (type1 == null || type2 == null) {
                return "java/lang/Object";
            }
            if (type1.equals(type2)) {
                return type1;
            }
            return "java/lang/Object";
        }
    }
}
