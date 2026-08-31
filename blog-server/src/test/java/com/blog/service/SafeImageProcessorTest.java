package com.blog.service;
import com.blog.common.BizException;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class SafeImageProcessorTest {
    @Test void rejectsHugeDimensionsWithoutDecodingPixelData() {
        byte[] gif = Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        gif[6]=(byte)255; gif[7]=(byte)127; gif[8]=(byte)255; gif[9]=(byte)127;
        assertThrows(BizException.class, () -> SafeImageProcessor.process(gif,512));
    }
    @Test void acceptsValidGifAndPreservesAnimationFormat() throws Exception {
        byte[] gif = Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        var output=SafeImageProcessor.process(gif,512);
        assertEquals(".gif",output.extension()); assertArrayEquals(gif,output.bytes());
    }
    @Test void narrowImageNeverScalesToZeroAndKeepsTransparency() throws Exception {
        BufferedImage input = new BufferedImage(1,4000,BufferedImage.TYPE_INT_ARGB);
        var bytes=new ByteArrayOutputStream(); ImageIO.write(input,"png",bytes);
        var result=SafeImageProcessor.process(bytes.toByteArray(),512);
        var decoded=ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertEquals(1,decoded.getWidth()); assertTrue(decoded.getHeight()<=512); assertTrue(decoded.getColorModel().hasAlpha());
    }
}
