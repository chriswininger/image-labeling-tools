package com.wininger.cli_image_labeler.image.tagging.utils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageUtils {
  /**
   * Converts a BufferedImage to a JPEG byte array with specified quality. This ensures good compression regardless of
   * source format.
   */
  public static byte[] imageToJpegBytes(final BufferedImage image, final float quality) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // Get JPEG writer
    final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
    final ImageWriteParam param = writer.getDefaultWriteParam();

    // Enable compression
    if (param.canWriteCompressed()) {
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(quality);
    }

    try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
      writer.setOutput(ios);
      writer.write(null, new IIOImage(image, null, null), param);
    }
    finally {
      writer.dispose();
    }

    return baos.toByteArray();
  }

  /**
   * Resizes an image to fit within the maximum dimension while maintaining aspect ratio. If the image is already
   * smaller, returns the original.
   */
  public static BufferedImage resizeImage(final BufferedImage originalImage, final int maxDimension) {
    final int originalWidth = originalImage.getWidth();
    final int originalHeight = originalImage.getHeight();

    // If image is already smaller than max dimension, return original
    if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
      return originalImage;
    }

    // Calculate new dimensions maintaining aspect ratio
    final double scale = Math.min(
        (double) maxDimension / originalWidth,
        (double) maxDimension / originalHeight
    );

    final int newWidth = (int) (originalWidth * scale);
    final int newHeight = (int) (originalHeight * scale);

    // Create resized image with better quality rendering
    final BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

    final Graphics2D g2d = resizedImage.createGraphics();
    try {
      // Use high-quality rendering hints
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
    }
    finally {
      g2d.dispose();
    }

    return resizedImage;
  }

  /**
   * Generates a unique filename for a thumbnail based on the original image path. Uses SHA-256 hash of the absolute
   * path to ensure uniqueness. Returns just the filename (not the full path).
   */
  public static String generateThumbnailFilename(final String imagePath) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest(Paths.get(imagePath).toAbsolutePath().toString().getBytes());

      // Convert hash to hex string
      final StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        final String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      return hexString + ".jpg";
    }
    catch (NoSuchAlgorithmException e) {
      // Fallback to a simpler approach if SHA-256 is not available (should never happen)
      final String sanitized = imagePath.replaceAll("[^a-zA-Z0-9]", "_");
      return Math.abs(sanitized.hashCode()) + ".jpg";
    }
  }
}
