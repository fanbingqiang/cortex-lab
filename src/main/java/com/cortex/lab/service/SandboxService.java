package com.cortex.lab.service;

import com.cortex.lab.dto.ExecuteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.cortex.lab.dto.ProjectFileDTO;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SandboxService {
    // 代码沙箱：编译执行 Java 代码和 Maven 项目

    private final Path workDir;

    public SandboxService() {
        try {
            this.workDir = Files.createTempDirectory("cortex-sandbox-");
            Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create sandbox directory", e);
        }
    }

    // 编译并执行单文件 Java 代码
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

    // 构建 Maven 项目（编译整个项目目录）
    public BuildResult buildMavenProject(List<ProjectFileDTO> files) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        Path projectDir = workDir.resolve(uuid);
        BuildResult result = new BuildResult();

        try {
            // Write all project files
            for (ProjectFileDTO f : files) {
                Path target = projectDir.resolve(f.getPath().replace('/', File.separatorChar));
                Files.createDirectories(target.getParent());
                Files.writeString(target, f.getContent(), StandardCharsets.UTF_8);
            }

            log.info("Maven project written to: {}", projectDir);

            // Run mvn compile
            ProcessBuilder pb = new ProcessBuilder(
                findMvn(), "compile", "-q",
                "-Dmaven.test.skip=true",
                "-Dorg.slf4j.simpleLogger.log.org.apache.maven.plugins=error"
            );
            pb.directory(projectDir.toFile());
            pb.redirectErrorStream(true);

            long start = System.currentTimeMillis();
            Process process = pb.start();

            Charset outputCharset = isWindows() ? Charset.forName("GBK") : StandardCharsets.UTF_8;
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), outputCharset));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.setSuccess(false);
                result.setOutput("编译超时（120s）");
                result.setProjectDir(projectDir.toString());
                return result;
            }

            long elapsed = System.currentTimeMillis() - start;
            result.setSuccess(process.exitValue() == 0);
            result.setExitCode(process.exitValue());
            result.setOutput(output.toString());
            result.setElapsedMs(elapsed);
            result.setProjectDir(projectDir.toString());
            return result;

        } catch (Exception e) {
            log.error("Maven build error", e);
            result.setSuccess(false);
            result.setOutput("构建失败: " + e.getMessage());
            return result;
        }
    }

    // 查找系统可用的 mvn 命令路径
    private String findMvn() {
        // Try common Maven locations
        String[] candidates = {
            "mvn.cmd", "mvn", "mvn.bat",
            "D:/apache-maven-3.9.11/bin/mvn.cmd",
            "D:/apache-maven-3.9.11/bin/mvn",
            "/d/apache-maven-3.9.11/bin/mvn",
            "C:/Program Files/apache-maven-3.9.11/bin/mvn.cmd"
        };
        for (String c : candidates) {
            try {
                Process p = new ProcessBuilder(c, "--version")
                    .redirectErrorStream(true).start();
                boolean exited = p.waitFor(5, TimeUnit.SECONDS);
                if (exited && p.exitValue() == 0) {
                    return c;
                }
            } catch (Exception ignored) {}
        }
        // On Windows, try from PATH with .cmd extension
        if (isWindows()) {
            try {
                Process p = new ProcessBuilder("where", "mvn").start();
                if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return "mvn.cmd";
                }
            } catch (Exception ignored) {}
        }
        return "mvn.cmd"; // fallback
    }

    @lombok.Data
    public static class BuildResult {
        // Maven 构建结果
        private boolean success;
        private int exitCode;
        private String output;
        private long elapsedMs;
        private String projectDir;
    }
}
