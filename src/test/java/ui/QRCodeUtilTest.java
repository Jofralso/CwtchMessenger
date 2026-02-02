package ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.google.zxing.WriterException;
import java.awt.image.BufferedImage;

public class QRCodeUtilTest {
    @Test
    public void testGenerateQRCodeImage() throws Exception {
        String text = "test-qr-content";
        BufferedImage img = QRCodeUtil.generateQRCodeImage(text, 200, 200);
        assertNotNull(img, "QR code image should not be null");
        assertEquals(200, img.getWidth(), "Width should match");
        assertEquals(200, img.getHeight(), "Height should match");
        // Check that the image is not blank (has some black pixels)
        boolean hasBlack = false;
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (img.getRGB(x, y) == -16777216) { // black pixel
                    hasBlack = true;
                    break;
                }
            }
            if (hasBlack) break;
        }
        assertTrue(hasBlack, "QR code image should contain black pixels");
    }

    @Test
    public void testGenerateQRCodeImageThrowsOnInvalidSize() {
        assertThrows(WriterException.class, () -> {
            QRCodeUtil.generateQRCodeImage("text", 0, 0);
        });
    }
}
