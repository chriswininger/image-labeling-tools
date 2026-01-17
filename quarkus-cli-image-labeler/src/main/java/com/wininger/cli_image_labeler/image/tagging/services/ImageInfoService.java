package com.wininger.cli_image_labeler.image.tagging.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import com.wininger.cli_image_labeler.image.tagging.dto.*;
import com.wininger.cli_image_labeler.image.tagging.dto.model_responses.ImageInfoFromDescriptionModelResponse;
import com.wininger.cli_image_labeler.image.tagging.exceptions.ExceededRetryLimitForModelRequest;
import com.wininger.cli_image_labeler.image.tagging.exceptions.ImageReadException;
import com.wininger.cli_image_labeler.image.tagging.exceptions.ImageWriteException;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.output.JsonSchemas;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static com.wininger.cli_image_labeler.image.tagging.utils.FileMetaDataUtils.getCreatedOn;
import static com.wininger.cli_image_labeler.image.tagging.utils.FileMetaDataUtils.getFileCreatedAt;
import static com.wininger.cli_image_labeler.image.tagging.utils.FileMetaDataUtils.getFileLastModified;
import static com.wininger.cli_image_labeler.image.tagging.utils.FileMetaDataUtils.getGeoLocation;
import static com.wininger.cli_image_labeler.image.tagging.utils.ImageUtils.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@ApplicationScoped
public class ImageInfoService
{
  private static final String MULTI_MODAL_MODAL = "gemma3:4b";
  private static final String OCR_MODEL = "deepseek-ocr:3b";

  private static final String TEXT_TAG = "text";
  private static final String PERSON_TAG = "person";

  private static final Integer NUM_MODEL_RETRIES = 5;

  // Maximum dimension (width or height) for resized images to avoid timeouts
  private static final int MAX_IMAGE_DIMENSION = 1024;

  private static final int IMAGE_DIMENSION_FOR_THUMBNAIL = 500;

  private final OllamaChatModel unstructuredModel;

  private final OllamaChatModel imageInfoFromDescriptionModel;

  private final SimilarityService similarityService;

  private final boolean logRequests;

  private final boolean logResponses;

  @Inject
  public ImageInfoService(
      @ConfigProperty(name = "ollama.log-requests", defaultValue = "false") boolean logRequests,
      @ConfigProperty(name = "ollama.log-responses", defaultValue = "false") boolean logResponses,
      final SimilarityService similarityService
  ) {
    this.logRequests = logRequests;
    this.logResponses = logResponses;

    unstructuredModel = getUnstructuredMultiModalModel();
    imageInfoFromDescriptionModel = getMultiModalModel(ImageInfoFromDescriptionModelResponse.class);

    this.similarityService = similarityService;
  }

  public ImageInfo generateImageInfoAndMetadata(final String imagePath, final boolean keepThumbnails) {
    // Load and resize the image
    final BufferedImage originalImage;

    try {
      originalImage = ImageIO.read(Paths.get(imagePath).toFile());
    } catch (IOException ex) {
      throw new ImageReadException(imagePath, ex);
    }

    if (originalImage == null) {
      throw new ImageReadException(imagePath, new NullPointerException("Null value returned from ImageIO.read"));
    }

    final ImageContent imageContent = getImageContentAndResizeIt(originalImage, imagePath);

    // Step 1: Get unstructured detailed description from the vision model
    System.out.println("Getting unstructured description from vision model...");
    final String detailedDescription = getUnstructuredDescription(imageContent);
    System.out.println("Detailed description received: " + detailedDescription.substring(0, Math.min(100, detailedDescription.length())) + "...");


    // Step 2: Extract structured fields from the description
    System.out.println("Extracting structured info from description...");
    final ImageInfoFromDescriptionModelResponse extractedInfo = extractImageInfoFromDescription(detailedDescription, imagePath);

    final boolean isText = isText(extractedInfo.doesContainText());
    final List<String> normalizedTags = normalizeTags(extractedInfo, isText);

    // Step 3: Extract file metadata (GPS location and date taken)
    Double gpsLatitude = null;
    Double gpsLongitude = null;
    Date imageTakenAt = null;

    try {
      final Map<String, Double> geoLocation = getGeoLocation(imagePath);
      gpsLatitude = geoLocation.get("latitude");
      gpsLongitude = geoLocation.get("longitude");
      if (gpsLatitude != null && gpsLongitude != null) {
        System.out.printf("GPS location extracted: %.6f, %.6f%n", gpsLatitude, gpsLongitude);
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not extract GPS location from " + imagePath + ": " + e.getMessage());
    }

    try {
      imageTakenAt = getCreatedOn(imagePath);
      if (imageTakenAt != null) {
        System.out.println("Image taken at: " + imageTakenAt);
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not extract date taken from " + imagePath + ": " + e.getMessage());
    }

    // Step 4: Extract filesystem timestamps
    Date fileCreatedAt = null;
    Date fileLastModified = null;

    try {
      fileCreatedAt = getFileCreatedAt(imagePath);
      fileLastModified = getFileLastModified(imagePath);
      System.out.println("File created at: " + fileCreatedAt);
      System.out.println("File last modified: " + fileLastModified);
    } catch (Exception e) {
      System.err.println("Warning: Could not extract file timestamps from " + imagePath + ": " + e.getMessage());
    }

    if (keepThumbnails) {
      // Save thumbnail to archive
      final byte[] imageBytesForThumbnail;
      try {
        imageBytesForThumbnail = imageToJpegBytes(
            resizeImage(originalImage, IMAGE_DIMENSION_FOR_THUMBNAIL), 0.85f);
      }
      catch (IOException e) {
        throw new ImageWriteException("Error Saving thumbnail: ", e);
      }

      saveThumbnail(imagePath, imageBytesForThumbnail);
    }

    // Generate a thumbnail filename (TODO: Eventually let's actually return ImageContent and then defer
    // all handling of thumbnails to the command)
    final String thumbnailName = keepThumbnails ? generateThumbnailFilename(imagePath) : null;
    return new ImageInfo(
        normalizedTags,
        extractedInfo.fullDescription(),
        extractedInfo.shortTitle(),
        isText,
        null, // textContents - not populated in experimental method for now
        thumbnailName,
        gpsLatitude,
        gpsLongitude,
        imageTakenAt,
        fileCreatedAt,
        fileLastModified
    );
  }

  // At this point we've already read the image, the only reason we are taking imagePath is for logging fileSize
  // and including imagePath in the exception message
  private ImageContent getImageContentAndResizeIt(BufferedImage originalImage, final String imagePath) {
    try {
      final int originalWidth = originalImage.getWidth();
      final int originalHeight = originalImage.getHeight();

      final BufferedImage resizedImage = resizeImage(originalImage, MAX_IMAGE_DIMENSION);

      final int resizedWidth = resizedImage.getWidth();
      final int resizedHeight = resizedImage.getHeight();

      // Convert a resized image to JPEG with compression for smaller file size
      // Always use JPEG to ensure good compression regardless of source format
      final byte[] imageBytes = imageToJpegBytes(resizedImage, 0.85f); // 85% quality
      final long resizedFileSize = imageBytes.length;

      // Log the resize information
      final long originalFileSize = Files.size(Paths.get(imagePath));
      System.out.printf("Image resize: %dx%d (%.1f KB) -> %dx%d (%.1f KB)%n",
          originalWidth, originalHeight, originalFileSize / 1024.0,
          resizedWidth, resizedHeight, resizedFileSize / 1024.0);

      final String base64Img = Base64.getEncoder().encodeToString(imageBytes);
      final long base64Size = base64Img.length();
      System.out.printf("Base64 size: %.1f KB%n", base64Size / 1024.0);

      return ImageContent.from(base64Img, "image/jpeg");
    }
    catch (IOException ex) {
      throw new RuntimeException("Could not parse image: " + imagePath, ex);
    }
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

  private ImageInfoFromDescriptionModelResponse extractImageInfoFromDescription(final String detailedDescription, final String imagePathForLogging) {
    int numbTimesTried = 0;

    while (numbTimesTried < NUM_MODEL_RETRIES) {
      if (numbTimesTried > 0) {
        System.out.println("Failed to get a valid result from the model for image: " + imagePathForLogging);
        System.out.printf("Trying again %s/%s%n", numbTimesTried + 1, NUM_MODEL_RETRIES);
      }
      final ImageInfoFromDescriptionService service = AiServices.builder(ImageInfoFromDescriptionService.class)
        .chatModel(imageInfoFromDescriptionModel)
        .build();

      final var result = service.extractImageInfoFromDetailedImageDescription(detailedDescription);

      if (nonNull(result.tags()) &&
        nonNull(result.fullDescription()) &&
        nonNull(result.shortTitle()) &&
        nonNull(result.doesContainText())) {
        return result;
      }

      // we did not return a result, increment and decide if we should try again
      numbTimesTried++;
    }

    // if we got this far without returning a result it means we exhausted our retries
    throw new ExceededRetryLimitForModelRequest(
      "Could not extract image info after %s tries".formatted(NUM_MODEL_RETRIES));
  }

  // worked well on cli: `llama run deepseek-ocr '"/Users/chriswininger/Pictures/test-images/25-12-17 08-50-55 3819.png"\nExtract the text in the image.'`
  private String doOCR(final ImageContent imageContent) {
    final ChatModel modelOcr = OllamaChatModel.builder()
        .modelName(OCR_MODEL)
        .baseUrl("http://localhost:11434/")
        .build();

    final TextContent command = TextContent.from("\nExtract the text in the image.");
    final UserMessage userMessage = UserMessage.from(imageContent, command);
    final ChatResponse chatResponse = modelOcr.chat(userMessage);

    // Parse the JSON response into ImageInfoModelResponse
    return chatResponse.aiMessage().text();
  }

  private OllamaChatModel getMultiModalModel(Class schemaClass) {
    final var responseFormat = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchemas.jsonSchemaFrom(schemaClass).get())
        .build();

    return OllamaChatModel.builder()
        .modelName(MULTI_MODAL_MODAL)
        //.numCtx(8192) // rather than default 4096, can go up to 128k for gemma3:4b
        .baseUrl("http://localhost:11434/")
        .responseFormat(responseFormat)
        .logRequests(logRequests)
        .logResponses(logResponses)
        //.temperature(0.9D)
        .build();
  }

  private OllamaChatModel getUnstructuredMultiModalModel() {
    return OllamaChatModel.builder()
        .modelName(MULTI_MODAL_MODAL)
        .baseUrl("http://localhost:11434/")
        .logRequests(logRequests)
        .logResponses(logResponses)
        .build();
  }

  /**
   * Gets a complete and thorough unstructured text description of an image from the model.
   * Unlike other methods, this does not attempt to parse the response as JSON.
   *
   * @param imageContent the image to describe
   * @return the model's free-form text description of the image
   */
  public String getUnstructuredDescription(final ImageContent imageContent) {
    final TextContent prompt = TextContent.from(
        "Please provide a complete and thorough description of this image. " +
        "Include all relevant details about the subjects, setting, colors, composition, " +
        "and any text or notable elements visible in the image."
    );
    final UserMessage userMessage = UserMessage.from(imageContent, prompt);
    final ChatResponse chatResponse = unstructuredModel.chat(userMessage);

    return chatResponse.aiMessage().text();
  }

  private List<String> normalizeTags(final ImageInfoFromDescriptionModelResponse modelResponse, final boolean isText) {
    // should have already checked this by now, but just in case :-)
    if (isNull(modelResponse.tags())) {
      return null;
    }

    final List<String> rawTags = new ArrayList<>(modelResponse.tags());

    // ensure text is a label (don't worry if it's already there, next step is de-duplication)
    if (isText) {
      rawTags.add(TEXT_TAG);
    }

    // ensure person is a label (don't worry if it's already there, next step is de-duplication)
    if (rawTags.contains("man") || rawTags.contains("woman") || rawTags.contains("people")) {
      rawTags.add(PERSON_TAG);
    }

    // Deduplicate tags;
    // Convert to lowercase;
    // Sort alphabetically
    return new HashSet<>(
      // convert to lowercase before de-duplicating
      rawTags.stream().map(String::toLowerCase).toList()
    ).stream()
      .filter(str  -> !str.isBlank())
      .sorted()
      .toList();
  }

  public boolean isText(final String textReasoning) {
    final double noTextSimilarity = similarityService.calculateSimilarity(textReasoning, "No visible text");
    final double isTextSimilarity = similarityService.calculateSimilarity(textReasoning, "The image contains text content");

    // TODO: Make these debug level logging
    System.out.println("text reasoning: " + textReasoning);
    System.out.println("noTextSimilarity: " + noTextSimilarity);
    System.out.println("isTestSimilarity: " + isTextSimilarity);

    return isTextSimilarity > noTextSimilarity;
  }
}
