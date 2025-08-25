package life.trally.knowcatfud.runner;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class StartupRunner implements ApplicationRunner {

    // 应用启动后自动执行一次
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 创建目录
        Files.createDirectories(Path.of("files/cache"));
        Files.createDirectories(Path.of("images"));
    }
}