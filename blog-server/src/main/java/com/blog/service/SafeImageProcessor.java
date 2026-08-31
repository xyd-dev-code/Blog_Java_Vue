package com.blog.service;

import com.blog.common.BizException;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;

/** Checks metadata before allocating pixels and limits concurrent image decoding. */
public final class SafeImageProcessor {
    private static final long MAX_PIXELS = 16_000_000L;
    private static final Semaphore DECODERS = new Semaphore(2);
    private SafeImageProcessor() { }
    public record Result(byte[] bytes, String extension) { }

    public static Result process(byte[] input, int maxDimension) throws IOException {
        if (input == null || input.length == 0 || maxDimension < 1) throw new BizException("图片为空");
        if (!DECODERS.tryAcquire()) throw new BizException(429, "图片处理中，请稍后重试");
        try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(input))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new BizException("图片无法解析，仅支持 PNG/JPEG/GIF");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, false, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("png", "jpeg", "jpg", "gif").contains(format)) throw new BizException("仅支持 PNG/JPEG/GIF");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                checkDimensions(width, height);
                if ("gif".equals(format)) {
                    if (input.length < 10) throw new BizException("GIF 文件不完整");
                    int cw = (input[6] & 255) | ((input[7] & 255) << 8);
                    int ch = (input[8] & 255) | ((input[9] & 255) << 8);
                    checkDimensions(cw, ch);
                    int frames = reader.getNumImages(true);
                    if (frames < 1 || frames > 100 || (long) cw * ch * frames > MAX_PIXELS)
                        throw new BizException("GIF 动画帧数或总像素超过限制");
                    long framePixels = 0;
                    for (int i = 0; i < frames; i++) {
                        int frameWidth = reader.getWidth(i), frameHeight = reader.getHeight(i);
                        checkDimensions(frameWidth, frameHeight);
                        framePixels += (long) frameWidth * frameHeight;
                        if (framePixels > MAX_PIXELS) throw new BizException("GIF 动画总像素超过限制");
                    }
                    return new Result(input, ".gif");
                }
                ImageReadParam readParam = reader.getDefaultReadParam();
                int sample = Math.max(1, Math.max(width, height) / maxDimension);
                readParam.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage source = reader.read(0, readParam);
                if (source == null) throw new BizException("无法解析的图片内容");
                double ratio = Math.min(1, (double) maxDimension / Math.max(source.getWidth(), source.getHeight()));
                int w = Math.max(1, (int) (source.getWidth() * ratio));
                int h = Math.max(1, (int) (source.getHeight() * ratio));
                boolean png = "png".equals(format);
                BufferedImage output = new BufferedImage(w, h, png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = output.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(source, 0, 0, w, h, null);
                } finally { graphics.dispose(); source.flush(); }
                ImageWriter writer = ImageIO.getImageWritersByFormatName(png ? "png" : "jpeg").next();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (var destination = new MemoryCacheImageOutputStream(bytes)) {
                    writer.setOutput(destination);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (!png) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(0.85f);
                    }
                    writer.write(null, new IIOImage(output, null, null), param);
                    destination.flush();
                } finally { writer.dispose(); output.flush(); }
                return new Result(bytes.toByteArray(), png ? ".png" : ".jpg");
            } finally { reader.dispose(); }
        } finally { DECODERS.release(); }
    }

    private static void checkDimensions(int width, int height) {
        if (width < 1 || height < 1 || width > 8192 || height > 8192 || (long) width * height > MAX_PIXELS)
            throw new BizException("图片尺寸过大，最大边长 8192、总像素 1600 万");
    }
}
