package io.github.headlesshq.headlessmc.launcher.instrumentation.lwjgl;

import io.github.headlesshq.headlessmc.launcher.instrumentation.AbstractClassTransformer;
import io.github.headlesshq.headlessmc.launcher.instrumentation.Target;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Skips native menu setup when SDL has not created a macOS window. */
public class MacosMenuTransformer extends AbstractClassTransformer {
    private static final String CLASS = "com/mojang/blaze3d/platform/MacosUtil";

    public MacosMenuTransformer() {
        super(CLASS);
    }

    @Override
    public boolean matches(Target target) {
        // Loaders put Minecraft's classes in different jars. Select the class
        // and method, rather than depending on a loader's filename or version.
        try (JarFile jar = target.toJar()) {
            JarEntry entry = jar.getJarEntry(CLASS + ".class");
            if (entry == null) {
                return false;
            }

            try (InputStream stream = jar.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new ClassReader(stream).accept(node, ClassReader.SKIP_CODE
                    | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return node.methods.stream().anyMatch(this::isMenuSetup);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not inspect " + target.getPath(), e);
        }
    }

    @Override
    protected void transform(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (isMenuSetup(method)) {
                // 26.3 assumes a native windowsMenu exists. No-render mode has
                // no Cocoa window or menu, so there is nothing to disable.
                method.instructions = new InsnList();
                method.instructions.add(new InsnNode(Opcodes.RETURN));
                method.tryCatchBlocks.clear();
                if (method.localVariables != null) {
                    method.localVariables.clear();
                }
                method.visibleLocalVariableAnnotations = null;
                method.invisibleLocalVariableAnnotations = null;
            }
        }
    }

    private boolean isMenuSetup(MethodNode method) {
        return method.name.equals("disableCloseWindowMenuItem")
            && method.desc.equals("()V")
            && (method.access & Opcodes.ACC_STATIC) != 0;
    }
}
