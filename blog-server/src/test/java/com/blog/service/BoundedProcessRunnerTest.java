package com.blog.service;

import com.blog.common.BizException;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BoundedProcessRunnerTest {
    @Test void timeoutWorksEvenWhenProcessNeverClosesStdout() {
        long started = System.nanoTime();
        assertThrows(BizException.class, () -> BoundedProcessRunner.run(process("hang"), Duration.ofMillis(250),1024));
        assertTrue(Duration.ofNanos(System.nanoTime()-started).toSeconds()<5);
    }
    @Test void rejectsUnboundedOutput() {
        assertThrows(BizException.class, () -> BoundedProcessRunner.run(process("output"),Duration.ofSeconds(5),1024));
    }
    @Test void containerHasNoNetworkAndOnlyTemporaryWritableMount() {
        var runner = new PandocRunner("unused");
        List<String> command = runner.containerCommand(Path.of(System.getProperty("java.io.tmpdir"),"review-test"),"review-test");
        for(String flag: List.of("--network=none","--read-only","--cap-drop=ALL","--memory=512m","--pids-limit=64","--pull=never"))
            assertTrue(command.contains(flag));
    }
    private ProcessBuilder process(String argument) {
        String executable = Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        return new ProcessBuilder(executable,"-cp",Path.of("target/test-classes").toAbsolutePath().toString(),
                Fixture.class.getName(), argument);
    }
    public static class Fixture {
        public static void main(String[] args) throws Exception {
            if(args[0].equals("hang")) Thread.sleep(30000);
            else for(int i=0; i<100000; i++) System.out.print("a long output line\n");
        }
    }
}
