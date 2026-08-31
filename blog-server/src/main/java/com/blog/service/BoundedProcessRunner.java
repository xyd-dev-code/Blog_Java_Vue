package com.blog.service;

import com.blog.common.BizException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class BoundedProcessRunner {
    private BoundedProcessRunner() { }
    static byte[] run(ProcessBuilder builder, Duration timeout, int maxOutput) throws IOException, InterruptedException {
        Process process = builder.redirectErrorStream(true).start();
        ExecutorService drain = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "converter-output");
            thread.setDaemon(true);
            return thread;
        });
        AtomicBoolean overflow = new AtomicBoolean();
        Future<byte[]> output = drain.submit(() -> {
            try (var input = process.getInputStream(); var buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[4096];
                int count;
                while ((count = input.read(chunk)) != -1) {
                    if (buffer.size() + count > maxOutput) { overflow.set(true); return buffer.toByteArray(); }
                    buffer.write(chunk, 0, count);
                }
                return buffer.toByteArray();
            }
        });
        try {
            process.getOutputStream().close();
            long deadline = System.nanoTime() + timeout.toNanos();
            while (!process.waitFor(50, TimeUnit.MILLISECONDS)) {
                if (overflow.get()) throw new BizException("转换进程输出超过限制");
                if (System.nanoTime() >= deadline) throw new BizException("文档转换超时");
            }
            byte[] bytes = output.get(1, TimeUnit.SECONDS);
            if (overflow.get()) throw new BizException("转换进程输出超过限制");
            if (process.exitValue() != 0) throw new BizException("文档转换失败，请检查转换容器及文档格式");
            return bytes;
        } catch (ExecutionException | TimeoutException e) {
            throw new BizException("无法读取转换进程输出");
        } finally {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            if (process.isAlive()) process.destroyForcibly();
            process.getInputStream().close();
            output.cancel(true);
            drain.shutdownNow();
        }
    }
}
