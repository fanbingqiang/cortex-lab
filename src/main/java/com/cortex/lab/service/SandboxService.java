package com.cortex.lab.service;

import com.cortex.lab.dto.ExecuteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SandboxService {

    private final Path workDir;

    public SandboxService() {
        try {
            this.workDir = Files.createTempDirectory("cortex-sandbox-");
            Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create sandbox directory", e);
        }
    }

    public ExecuteResponse execute(String code) {
        ExecuteResponse response = new ExecuteResponse();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        Path tempDir = null;

        try {
            tempDir = Files.createDirectory(workDir.resolve(uuid));
            String className = extractClassName(code);
            if (className == null) {
                className = "Main" + uuid;
                code = code.replaceFirst("(public\\s+)?class\\s+\\w+", "public class " + className);
            }

            Path javaFile = tempDir.resolve(className + ".java");
            Files.writeString(javaFile, code, StandardCharsets.UTF_8);

            ProcessBuilder compilePb = new ProcessBuilder("javac", "-encoding", "UTF-8", javaFile.toString());
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();

            Charset outputCharset = isWindows() ? Charset.forName("GBK") : StandardCharsets.UTF_8;
            BufferedReader compileReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream(), outputCharset));
            StringBuilder compileOutput = new StringBuilder();
            String line;
            while ((line = compileReader.readLine()) != null) {
                compileOutput.append(line).append("\n");
            }
            compileProcess.waitFor(15, TimeUnit.SECONDS);

            if (compileProcess.exitValue() != 0) {
                response.setSuccess(false);
                response.setExitCode(compileProcess.exitValue());
                response.setStdout("");
                response.setStderr(compileOutput.toString());
                response.setError("Compilation failed");
                return response;
            }

            ProcessBuilder runPb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", tempDir.toString(), className);
            runPb.redirectErrorStream(true);
            Process runProcess = runPb.start();

            if (!runProcess.waitFor(10, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                response.setSuccess(false);
                response.setExitCode(-1);
                response.setError("Execution timed out (10s)");
                return response;
            }

            BufferedReader runReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream(), outputCharset));
            StringBuilder runOutput = new StringBuilder();
            while ((line = runReader.readLine()) != null) {
                runOutput.append(line).append("\n");
            }

            response.setSuccess(runProcess.exitValue() == 0);
            response.setExitCode(runProcess.exitValue());
            response.setStdout(runOutput.toString());
            response.setStderr("");
            response.setError(runProcess.exitValue() != 0 ? "Runtime error" : null);

        } catch (Exception e) {
            log.error("Sandbox execution error", e);
            response.setSuccess(false);
            response.setExitCode(-1);
            response.setError(e.getMessage());
        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                } catch (Exception ignored) {}
            }
        }

        return response;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String extractClassName(String code) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("class\\s+(\\w+)");
        java.util.regex.Matcher m = p.matcher(code);
        if (m.find()) return m.group(1);
        return null;
    }

    private void cleanup() {
        try {
            Files.walk(workDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (Exception ignored) {}
    }
}
