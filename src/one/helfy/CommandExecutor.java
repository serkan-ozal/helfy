package one.helfy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * @author serkan
 */
public class CommandExecutor {

    public static void main(String[] args) throws IOException {
        Process proc = Runtime.getRuntime().exec(args);
        BufferedReader is = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String line;
        while ((line = is.readLine()) != null) {
            System.out.println(line);
        }
        while ((line = err.readLine()) != null) {
            System.err.println(line);
        }
        proc.destroy();
    }

    public static class CommandResult {

        private final String stdout;
        private final String stderr;

        private CommandResult(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

    }

    public static String getProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return jvmName.split("@")[0];
    }

    private static CommandResult getCommandResult(Process proc) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();
        String line;
        while ((line = is.readLine()) != null) {
            stdoutBuilder.append(line).append("\n");
        }
        while ((line = err.readLine()) != null) {
            stderrBuilder.append(line).append("\n");
        }
        proc.destroy();
        return new CommandResult(stdoutBuilder.toString(), stderrBuilder.toString());
    }

    public static CommandResult runCommandFromCurrentProcess(String cmd) throws IOException {
        List<String> cmdList = new ArrayList<String>();
        cmdList.add("/bin/sh");
        cmdList.add("-c");
        cmdList.add(cmd);

        Process proc = Runtime.getRuntime().exec(cmdList.toArray(new String[cmdList.size()]));
        return getCommandResult(proc);
    }

    public static CommandResult runCommandFromDifferentProcess(String cmd) throws IOException {
        String classpath = System.getProperty("java.class.path");
        List<String> cmdList = new ArrayList<String>();
        cmdList.add("java");
        cmdList.add("-classpath");
        cmdList.add(classpath);
        cmdList.add(CommandExecutor.class.getName());
        cmdList.add("/bin/sh");
        cmdList.add("-c");
        cmdList.add(cmd);

        Process proc = Runtime.getRuntime().exec(cmdList.toArray(new String[cmdList.size()]));
        return getCommandResult(proc);
    }

}
