package com.system.service.common.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(name = "yolo.enabled", havingValue = "true")
public class YoloServerRunner implements CommandLineRunner {

    @Value("${yolo.script-path:./yolo_server.py}")
    private String scriptPath;

    @Value("${yolo.port:8007}")
    private int port;

    @Value("${yolo.host:0.0.0.0}")
    private String host;

    private Process pythonProcess;

    @Override
    public void run(String... args) {
        try {
            File scriptFile = findScriptFile();
            if (scriptFile == null || !scriptFile.exists()) {
                log.warn("YOLO 脚本不存在，跳过启动");
                log.warn("尝试的路径:");
                log.warn("  1. 配置路径: {}", scriptPath);
                log.warn("  2. 工作目录: {}", System.getProperty("user.dir"));
                log.warn("  3. 项目根目录: {}", new File("../yolo_server.py").getAbsolutePath());
                return;
            }

            log.info("正在启动 YOLO 视频分析服务...");
            log.info("   脚本路径: {}", scriptFile.getAbsolutePath());
            log.info("   监听端口: {}", port);

            String pythonCommand = isWindows() ? "python" : "python3";

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonCommand,
                    scriptFile.getName(),
                    "--port",
                    String.valueOf(port),
                    "--host",
                    host
            );

            processBuilder.directory(scriptFile.getParentFile());
            
            processBuilder.redirectErrorStream(true);

            pythonProcess = processBuilder.start();

            Thread logThread = new Thread(() -> {
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[YOLO] {}", line);
                    }
                } catch (IOException e) {
                    if (pythonProcess.isAlive()) {
                        log.error("读取 YOLO 日志失败: {}", e.getMessage());
                    }
                }
            });
            logThread.setDaemon(true);
            logThread.start();

            TimeUnit.SECONDS.sleep(2);

            if (!pythonProcess.isAlive()) {
                int exitCode = pythonProcess.exitValue();
                log.error("YOLO 服务启动失败，退出码: {}", exitCode);
            } else {
                log.info("YOLO 视频分析服务已启动 - http://{}:{}", host, port);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(this::stopPythonProcess));

        } catch (Exception e) {
            log.error("启动 YOLO 服务异常: {}", e.getMessage(), e);
        }
    }

    private File findScriptFile() {
        File file = new File(scriptPath);
        if (file.exists()) {
            return file;
        }

        file = new File(System.getProperty("user.dir"), scriptPath);
        if (file.exists()) {
            return file;
        }

        file = new File(System.getProperty("user.dir"), "yolo_server.py");
        if (file.exists()) {
            return file;
        }

        file = new File("../yolo_server.py");
        if (file.exists()) {
            return file.getAbsoluteFile();
        }

        String projectRoot = System.getProperty("user.dir");
        if (projectRoot.endsWith("service")) {
            file = new File(projectRoot, "yolo_server.py");
            if (file.exists()) {
                return file;
            }
            file = new File(projectRoot).getParentFile();
            if (file != null) {
                file = new File(file, "yolo_server.py");
                if (file.exists()) {
                    return file;
                }
            }
        }

        return null;
    }

    @PreDestroy
    public void destroy() {
        stopPythonProcess();
    }

    private void stopPythonProcess() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            log.info("正在停止 YOLO 视频分析服务...");
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(5, TimeUnit.SECONDS)) {
                    log.warn("YOLO 进程未正常终止，强制结束");
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pythonProcess.destroyForcibly();
            }
            log.info("YOLO 视频分析服务已停止");
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
