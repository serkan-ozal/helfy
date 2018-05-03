package one.helfy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Symbols {

    private static SymbolFinder symbolFinder;

    static {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                String jre = System.getProperty("java.home");
                if (!loadLibrary(jre + "/bin/server/jvm.dll") && !loadLibrary(jre + "/bin/client/jvm.dll")) {
                    throw new JVMException("Cannot find jvm.dll. Unsupported JVM?");
                }
                symbolFinder = new NativeSymbolFinder(Symbols.class.getClassLoader());
            } else if (os.contains("nix")) {
                symbolFinder = new NativeSymbolFinder(null);
            } else if (os.contains("mac")) {
                String jre = System.getProperty("java.home");
                String libPath;
                boolean loaded = loadLibrary(libPath = jre + "/lib/server/libjvm.dylib");
                if (!loaded) {
                    loaded = loadLibrary(libPath = jre + "/lib/client/libjvm.dylib");
                }
                if (!loaded) {
                    throw new JVMException("Cannot find libjvm.dylib. Unsupported JVM?");
                }
                symbolFinder = new MacSymbolFinder(libPath, "libjvm.dylib");
            } else {

            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    private static boolean loadLibrary(String dll) {
        try {
            System.load(dll);
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static long lookup(String name) {
        if (symbolFinder == null) {
            throw new IllegalStateException("No symbol finder available");
        }
        return symbolFinder.lookup(name);
    }

    private interface SymbolFinder {

        long lookup(String name);

    }

    private static class NativeSymbolFinder implements SymbolFinder {

        private final ClassLoader classLoader;
        private final Method findNative;

        private NativeSymbolFinder(ClassLoader classLoader) {
            this.classLoader = classLoader;
            try {
                this.findNative = ClassLoader.class.getDeclaredMethod("findNative", ClassLoader.class, String.class);
                this.findNative.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new JVMException("Method ClassLoader.findNative not found");
            }
        }

        @Override
        public long lookup(String name) {
            try {
                return (Long) findNative.invoke(null, classLoader, name);
            } catch (InvocationTargetException e) {
                throw new JVMException(e.getTargetException());
            } catch (IllegalAccessException e) {
                throw new JVMException(e);
            }
        }

    }

    private static class MacSymbolFinder implements SymbolFinder {

        private final long jvmLibBaseAddress;
        private final Map<String, Long> symbolOffsetMap;

        private MacSymbolFinder(String jvmLibPath, String jvmLibName) {
            try {
                this.jvmLibBaseAddress = findLibBaseAddress(jvmLibName);
                this.symbolOffsetMap = findSymbolOffsets(jvmLibPath);
            } catch (IOException e) {
                throw new JVMException(e);
            }
        }

        @Override
        public long lookup(String name) {
            Long offset = symbolOffsetMap.get("_" + name);
            if (offset == null) {
                throw new IllegalArgumentException("Symbol couldn't be found: " + name);
            }
            return jvmLibBaseAddress + offset;
        }

        private static long findLibBaseAddress(String libName) throws IOException {
            CommandExecutor.CommandResult commandResult =
                    CommandExecutor.runCommandFromCurrentProcess(
                            "vmmap " + CommandExecutor.getProcessId() + " | " + "grep " + libName);
            String stderr = commandResult.getStderr();
            if (stderr != null && stderr.length() > 0) {
                throw new IOException(stderr);
            }
            for (String line : commandResult.getStdout().split("\\n")) {
                if (line.startsWith("__TEXT")) {
                    return Long.parseLong(line.split("\\s+")[1].split("-")[0], 16);
                }
            }
            throw new IOException(libName + " not found");
        }

        private static Map<String, Long> findSymbolOffsets(String libPath) throws IOException {
            CommandExecutor.CommandResult commandResult =
                    CommandExecutor.runCommandFromCurrentProcess("nm " + libPath);
            String stderr = commandResult.getStderr();
            if (stderr != null && stderr.length() > 0) {
                throw new IOException(stderr);
            }
            Map<String, Long> symbols = new HashMap<String, Long>();
            for (String line : commandResult.getStdout().split("\\n")) {
                String[] lineParts = line.trim().split("\\s+");
                if (lineParts.length == 3) {
                    String symbolName = lineParts[2];
                    long symbolOffset = Long.parseLong(lineParts[0], 16);
                    symbols.put(symbolName, symbolOffset);
                }
            }
            return symbols;
        }

        private static long findSymbolOffset(String libPath, String name) throws IOException {
            CommandExecutor.CommandResult commandResult =
                    CommandExecutor.runCommandFromCurrentProcess(
                            "nm " + libPath + " | " + "grep _" + name);
            String stderr = commandResult.getStderr();
            if (stderr != null && stderr.length() > 0) {
                throw new IOException(stderr);
            }
            return Long.parseLong(commandResult.getStdout().split("\\s+")[0], 16);
        }

    }

}
