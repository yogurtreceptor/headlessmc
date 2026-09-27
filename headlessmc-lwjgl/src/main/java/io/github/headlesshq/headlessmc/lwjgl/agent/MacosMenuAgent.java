package io.github.headlesshq.headlessmc.lwjgl.agent;

import io.github.headlesshq.headlessmc.lwjgl.transformer.MacosMenuTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/** Skips native window-menu setup for macOS no-render launches. */
public class MacosMenuAgent implements ClassFileTransformer {
    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new MacosMenuAgent());
    }

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> type,
                            ProtectionDomain domain, byte[] bytes) {
        return MacosMenuTransformer.transform(name, bytes);
    }
}
