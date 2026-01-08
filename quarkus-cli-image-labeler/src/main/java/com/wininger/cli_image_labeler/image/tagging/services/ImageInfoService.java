package com.wininger.cli_image_labeler.image.tagging.services;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfoModelResponse;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.output.JsonSchemas;
import jakarta.enterprise.context.ApplicationScoped;

import static java.util.Objects.nonNull;

@ApplicationScoped
public class ImageInfoService
{
  private static final String PROMPT = """
      Generate an image description and tags based on this image.

      * You are a bot that tags images. You can create your own tags based on what you see but,
      * be sure to use the following tags if any apply: person, building, flower, flowers, tree, trees, animal, animals, chicken, bird.
      * If you can't tell what's in an image you can respond with something like this:
       {"tags": ["unknown"] , "description": "A blurry image possibly containing text." }

      Return a JSON object with the REQUIRED fields 'tags' (array of strings) and 'fullDescription' (string)
    """;

  private static final Integer NUM_MODEL_RETRIES = 5;

  // Maximum dimension (width or height) for resized images to avoid timeouts
  private static final int MAX_IMAGE_DIMENSION = 600;

  private final OllamaChatModel model;

  public ImageInfoService() {
    final var responseFormat = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchemas.jsonSchemaFrom(ImageInfoModelResponse.class).get())
        .build();

    model = OllamaChatModel.builder()
        .modelName("gemma3:4b")
        //.numCtx(8192) // rather than default 4096, can go up to 128k for gemma3:4b
        .baseUrl("http://localhost:11434/")
        .responseFormat(responseFormat)
        .build();
  }

  public ImageInfo generateImageInfoAndMetadata(final String imagePath, final boolean keepThumbnails) {
    final ImageContent imageContent = getImageContent(imagePath, keepThumbnails);
    final ImageInfo imageInfo = generateImageInfo(imageContent, imagePath);

    // Generate a thumbnail filename and add it to ImageInfo (the thumbnail its self has already been saved at this point
    // assuming keepThumbnails was set to true, this just determines if we store it in the db
    final String thumbnailName = keepThumbnails ? generateThumbnailFilename(imagePath) : null;

    // Deduplicate tags, convert to lowercase, sort alphabetically
    final List<String> deduplicatedTags = imageInfo.tags() != null
        ? new HashSet<>(
            // conver to lowercase before de-duplicating
            imageInfo.tags().stream().map(String::toLowerCase).toList()
        ).stream()
          .filter(str  -> !str.isBlank())
          .sorted()
          .toList()
        : null;

    return new ImageInfo(
        deduplicatedTags,
        imageInfo.fullDescription(),
        imageInfo.shortTitle(),
        imageInfo.isText(),
        thumbnailName
    );
  }

  private ImageInfo generateImageInfo(final ImageContent imageContent, final String imagePath) {
    int numbTimesTried = 0;

    while (numbTimesTried < NUM_MODEL_RETRIES) {
      if (numbTimesTried > 0) {
        System.out.println("Failed to get a valid result from the model for image: " + imagePath);
        System.out.println("Trying again %s/%s".formatted(numbTimesTried, NUM_MODEL_RETRIES));
      }

      final TextContent question = TextContent.from(PROMPT);
      final UserMessage userMessage = UserMessage.from(question, imageContent);
      final ChatResponse chatResponse = model.chat(userMessage);

      // Parse the JSON response into ImageInfoModelResponse
      final String jsonResponse = chatResponse.aiMessage().text();
      final ObjectMapper mapper = new ObjectMapper();

      final ImageInfoModelResponse modelResponse;
      try {
        modelResponse = mapper.readValue(jsonResponse, ImageInfoModelResponse.class);

        // Validate all required fields are present
        if (nonNull(modelResponse.tags()) &&
            nonNull(modelResponse.fullDescription())) {
          System.out.println("token usage: " + chatResponse.tokenUsage());

          // Convert to ImageInfo (without thumbnailName, which will be added later)
          return new ImageInfo(
              modelResponse.tags(),
              modelResponse.fullDescription());
        } else {
          // Log which fields are missing
          System.out.println("Missing required fields in model response for image: " + chatResponse.aiMessage().text());
          System.out.println("token usage: " + chatResponse.tokenUsage());
        }
      }
      catch (JsonProcessingException e) {
        // JSON parsing failed - log the error and raw response for debugging
        System.out.println("Error parsing JSON response from model for image: " + imagePath);
        System.out.println("Error: " + e.getMessage());
        System.out.println("Raw response: " + jsonResponse);
      }

      // we did not return a result, increment and decide if we should try again
      numbTimesTried++;
    }

    // if we got this far without returning a result it means we exhausted our retries
    throw new RuntimeException("Could not generate Image Info after %s tries".formatted(NUM_MODEL_RETRIES));
  }

  private ImageContent getImageContent(final String imagePath, final boolean keepThumbnails) {
    try {
      // Get original file size
      final long originalFileSize = Files.size(Paths.get(imagePath));

      // Load and resize the image
      final BufferedImage originalImage = ImageIO.read(Paths.get(imagePath).toFile());
      if (originalImage == null) {
        throw new RuntimeException("Could not read image: " + imagePath);
      }

      final int originalWidth = originalImage.getWidth();
      final int originalHeight = originalImage.getHeight();

      final BufferedImage resizedImage = resizeImage(originalImage, MAX_IMAGE_DIMENSION);

      final int resizedWidth = resizedImage.getWidth();
      final int resizedHeight = resizedImage.getHeight();

      // Convert resized image to JPEG with compression for smaller file size
      // Always use JPEG to ensure good compression regardless of source format
      final byte[] imageBytes = imageToJpegBytes(resizedImage, 0.85f); // 85% quality
      final long resizedFileSize = imageBytes.length;

      // Log the resize information
      System.out.printf("Image resize: %dx%d (%.1f KB) -> %dx%d (%.1f KB)%n",
          originalWidth, originalHeight, originalFileSize / 1024.0,
          resizedWidth, resizedHeight, resizedFileSize / 1024.0);

      final String base64Img = Base64.getEncoder().encodeToString(imageBytes);
      final long base64Size = base64Img.length();
      System.out.printf("Base64 size: %.1f KB%n", base64Size / 1024.0);

      if (keepThumbnails) {
        // Save thumbnail to archive
        saveThumbnail(imagePath, imageBytes);
      }

      return ImageContent.from(base64Img, "image/jpeg");
    }
    catch (IOException ex) {
      throw new RuntimeException("Could not parse image: " + imagePath, ex);
    }
  }

  /**
   * Resizes an image to fit within the maximum dimension while maintaining aspect ratio. If the image is already
   * smaller, returns the original.
   */
  private BufferedImage resizeImage(final BufferedImage originalImage, final int maxDimension) {
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
   * Converts a BufferedImage to a JPEG byte array with specified quality. This ensures good compression regardless of
   * source format.
   */
  private byte[] imageToJpegBytes(final BufferedImage image, final float quality) throws IOException {
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
   * Saves the thumbnail image to the data/thumbnails directory. Uses a hash of the original image path to generate a
   * unique filename.
   */
  private void saveThumbnail(final String imagePath, final byte[] thumbnailBytes) {
    try {
      final String thumbnailFilename = generateThumbnailFilename(imagePath);
      final Path thumbnailPath = Paths.get("data", "thumbnails", thumbnailFilename);

      Files.write(thumbnailPath, thumbnailBytes);
      System.out.println("Thumbnail saved: " + thumbnailPath);
    }
    catch (IOException e) {
      // Log error but don't fail the entire operation if thumbnail saving fails
      System.err.println("Warning: Failed to save thumbnail for " + imagePath + ": " + e.getMessage());
    }
  }

  /**
   * Generates a unique filename for a thumbnail based on the original image path. Uses SHA-256 hash of the absolute
   * path to ensure uniqueness. Returns just the filename (not the full path).
   */
  private String generateThumbnailFilename(final String imagePath) {
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

      return hexString.toString() + ".jpg";
    }
    catch (NoSuchAlgorithmException e) {
      // Fallback to a simpler approach if SHA-256 is not available (should never happen)
      final String sanitized = imagePath.replaceAll("[^a-zA-Z0-9]", "_");
      return Math.abs(sanitized.hashCode()) + ".jpg";
    }
  }
}
