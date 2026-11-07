package com.forkmyfolio.service.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.openpdf.text.Image;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * QR-code bitmap generation for the PDF templates.
 *
 * <p>
 * Phase 0.12 (OpenPDF migration): OpenPDF 3.x ships no QR-code support
 * (iText 7's {@code BarcodeQRCode} has no equivalent), so QR codes are
 * generated with ZXing (Apache-2.0) and embedded as a high-resolution PNG
 * raster. The raster is rendered at 600 px regardless of the final point
 * size, so print quality (≥ 300 dpi at the sizes used by the templates)
 * is visually equivalent to the previous vector rendering.
 *
 * <p>
 * Adheres to NAG-01 (DBR): pure utility, no entity mutation.
 */
public final class QrCodeImages {

    /** Rendered QR raster resolution in pixels (side length). */
    private static final int QR_PIXELS = 600;

    /** Private utility class — never instantiated. */
    private QrCodeImages() {
    }

    /**
     * Renders {@code content} as a QR image scaled to {@code sizePt × sizePt}
     * points. The quiet zone is zeroed to match the tight crop the previous
     * iText renderer produced; the caller wraps the image with its own margin.
     *
     * @param content   the text to encode (never blank).
     * @param darkColor the module colour (previously the iText render colour).
     * @param sizePt    final rendered size in points.
     * @return the scaled image, ready to be added to a cell or document.
     * @throws IOException if the QR matrix cannot be encoded or rasterised.
     */
    public static Image createQrImage(String content, Color darkColor, float sizePt) throws IOException {
        if (content == null || content.isBlank()) {
            throw new IOException("QR content must not be blank");
        }
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    QR_PIXELS,
                    QR_PIXELS,
                    Map.of(EncodeHintType.MARGIN, 0));
            BufferedImage raster = new BufferedImage(matrix.getWidth(), matrix.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Color light = Color.WHITE;
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    raster.setRGB(x, y, matrix.get(x, y) ? darkColor.getRGB() : light.getRGB());
                }
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(raster, "png", png);
            Image image = Image.getInstance(png.toByteArray());
            image.scaleAbsolute(sizePt, sizePt);
            return image;
        } catch (com.google.zxing.WriterException e) {
            throw new IOException("Failed to encode QR code", e);
        }
    }
}
