package com.blog.controller;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteControllerFaviconTest {

    @Test
    void convertsSquareImageToTransparentCircularPng() throws Exception {
        BufferedImage source = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();

        ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
        ImageIO.write(source, "png", inputBytes);

        byte[] result = SiteController.toCircularPng(inputBytes.toByteArray());
        assertNotNull(result);
        BufferedImage favicon = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(64, favicon.getWidth());
        assertEquals(64, favicon.getHeight());
        assertEquals(0, favicon.getRGB(0, 0) >>> 24);
        assertTrue((favicon.getRGB(32, 32) >>> 24) > 0);
    }
}
