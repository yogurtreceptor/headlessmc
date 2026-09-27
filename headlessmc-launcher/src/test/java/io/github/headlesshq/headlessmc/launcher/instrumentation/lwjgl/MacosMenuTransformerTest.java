package io.github.headlesshq.headlessmc.launcher.instrumentation.lwjgl;

import com.mojang.blaze3d.platform.MacosUtil;
import io.github.headlesshq.headlessmc.launcher.instrumentation.Instrumentation;
import io.github.headlesshq.headlessmc.launcher.instrumentation.Target;
import io.github.headlesshq.headlessmc.launcher.util.IOUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class MacosMenuTransformerTest {
    private static final String CLASS = "com/mojang/blaze3d/platform/MacosUtil";
    @TempDir
    Path directory;

    @Test
    public void testMenuSetupWithoutNativeWindow() throws Exception {
        Path jar = directory.resolve("loader-client.jar");
        try (InputStream stream = MacosUtil.class.getResourceAsStream("MacosUtil.class");
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            assertNotNull(stream);
            output.putNextEntry(new JarEntry(CLASS + ".class"));
            IOUtil.copy(stream, output);
        }

        MacosMenuTransformer transformer = new MacosMenuTransformer();
        Target target = new Target(false, jar.toString());
        assertTrue(transformer.matches(target));
        Path transformed = Files.createDirectory(directory.resolve("transformed"));
        List<String> result = new Instrumentation(Collections.singletonList(transformer),
            transformed.toFile()).instrument(Collections.singletonList(target));
        try (URLClassLoader loader = new URLClassLoader(
            new URL[]{new java.io.File(result.get(0)).toURI().toURL()}, null)) {
            Class<?> type = loader.loadClass(MacosUtil.class.getName());
            assertDoesNotThrow(() -> type.getMethod("disableCloseWindowMenuItem").invoke(null));
            assertEquals(42, type.getMethod("unrelatedMethod").invoke(null));
        }

        assertTrue(transformer.hasRun());
    }

    @Test
    public void testOlderMinecraftClassIsNotSelected() throws Exception {
        // 26.2 has MacosUtil but not the new native menu setup method.
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, CLASS, null, "java/lang/Object", null);
        writer.visitEnd();
        Path jar = directory.resolve("older-client.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(CLASS + ".class"));
            output.write(writer.toByteArray());
        }

        assertFalse(new MacosMenuTransformer().matches(new Target(true, jar.toString())));
    }
}
