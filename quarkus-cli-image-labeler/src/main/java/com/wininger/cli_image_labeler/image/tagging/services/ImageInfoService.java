package com.wininger.cli_image_labeler.image.tagging.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;

import com.wininger.cli_image_labeler.image.tagging.dto.*;
import com.wininger.cli_image_labeler.image.tagging.dto.model_responses.ImageInfoFromDescriptionModelResponse;
import com.wininger.cli_image_labeler.image.tagging.dto.model_responses.ImageInfoIsTextModelResponse;
import com.wininger.cli_image_labeler.image.tagging.dto.model_responses.ImageInfoTagsAndDescriptionModelResponse;
import com.wininger.cli_image_labeler.image.tagging.dto.model_responses.ImageInfoTitleModelResponse;
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

import static com.wininger.cli_image_labeler.image.tagging.utils.ImageUtils.*;
import static java.util.Objects.nonNull;

@ApplicationScoped
public class ImageInfoService
{
  private static final String MULTI_MODAL_MODAL = "gemma3:4b";
  private static final String OCR_MODEL = "deepseek-ocr:3b";

  private static final String TEXT_TAG = "text";

  private static final Integer NUM_MODEL_RETRIES = 5;

  // Maximum dimension (width or height) for resized images to avoid timeouts
  private static final int MAX_IMAGE_DIMENSION = 1024;

  private static final int IMAGE_DIMENSION_FOR_THUMBNAIL = 500;

  private final OllamaChatModel isTextModel;

  private final OllamaChatModel titleModel;

  private final OllamaChatModel tagAndDescriptionModel;

  private final OllamaChatModel unstructuredModel;

  private final OllamaChatModel imageInfoFromDescriptionModel;

  private final boolean logRequests;

  private final boolean logResponses;

  @Inject
  public ImageInfoService(
      @ConfigProperty(name = "ollama.log-requests", defaultValue = "false") boolean logRequests,
      @ConfigProperty(name = "ollama.log-responses", defaultValue = "false") boolean logResponses
  ) {
    this.logRequests = logRequests;
    this.logResponses = logResponses;
    isTextModel = getMultiModalModel(ImageInfoIsTextModelResponse.class);
    titleModel = getMultiModalModel(ImageInfoTitleModelResponse.class);
    tagAndDescriptionModel = getMultiModalModel(ImageInfoTagsAndDescriptionModelResponse.class);
    unstructuredModel = getUnstructuredMultiModalModel();
    imageInfoFromDescriptionModel = getMultiModalModel(ImageInfoFromDescriptionModelResponse.class);
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
    final ImageInfo imageInfo = generateImageInfo(imageContent, imagePath);

    // Generate a thumbnail filename and add it to ImageInfo (the thumbnail its self has already been saved at this point
    // assuming keepThumbnails was set to true, this just determines if we store it in the db
    final String thumbnailName = keepThumbnails ? generateThumbnailFilename(imagePath) : null;

    // Deduplicate tags, convert to lowercase, sort alphabetically
    final List<String> deduplicatedTags = imageInfo.tags() != null
        ? new HashSet<>(
            // convert to lowercase before de-duplicating
            imageInfo.tags().stream().map(String::toLowerCase).toList()
        ).stream()
          .filter(str  -> !str.isBlank())
          .sorted()
          .toList()
        : null;

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

    return new ImageInfo(
        deduplicatedTags,
        imageInfo.fullDescription(),
        imageInfo.shortTitle(),
        imageInfo.isText(),
        imageInfo.textContents(),
        thumbnailName
    );
  }

  /**
   * Experimental method that uses a two-pass approach:
   * 1. First gets an unstructured detailed description from the vision model
   * 2. Then extracts structured fields (tags, description, title, isText) from that description
   *
   * This approach may produce better quality results since the vision model focuses purely
   * on describing what it sees, and structured extraction happens separately.
   */
  public ImageInfo generateImageInfoAndMetadataExperimental(final String imagePath, final boolean keepThumbnails) {
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
    final ImageInfoFromDescriptionModelResponse extractedInfo = extractImageInfoFromDescription(detailedDescription);

    // Generate a thumbnail filename
    final String thumbnailName = keepThumbnails ? generateThumbnailFilename(imagePath) : null;

    // Deduplicate tags, convert to lowercase, sort alphabetically
    final List<String> deduplicatedTags = extractedInfo.tags() != null
        ? new HashSet<>(
            extractedInfo.tags().stream().map(String::toLowerCase).toList()
        ).stream()
          .filter(str -> !str.isBlank())
          .sorted()
          .toList()
        : null;

    // Ensure "text" tag is present if isText is true
    final List<String> finalTags;
    if (Boolean.TRUE.equals(extractedInfo.isText()) && deduplicatedTags != null && !deduplicatedTags.contains(TEXT_TAG)) {
      finalTags = new java.util.ArrayList<>(deduplicatedTags);
      finalTags.add(TEXT_TAG);
      finalTags.sort(String::compareTo);
    } else {
      finalTags = deduplicatedTags;
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

    return new ImageInfo(
        finalTags,
        extractedInfo.fullDescription(),
        extractedInfo.shortTitle(),
        extractedInfo.isText(),
        null, // textContents - not populated in experimental method for now
        thumbnailName
    );
  }

  private ImageInfo generateImageInfo(final ImageContent imageContent, final String imagePath) {
    int numbTimesTried = 0;

    while (numbTimesTried < NUM_MODEL_RETRIES) {
      if (numbTimesTried > 0) {
        System.out.println("Failed to get a valid result from the model for image: " + imagePath);
        System.out.printf("Trying again %s/%s%n", numbTimesTried + 1, NUM_MODEL_RETRIES);
      }

      final ImageInfoTagsAndDescriptionModelResponse tagsAndDescription = getTagsAndDescription(imageContent);

      // Validate all required fields are present
      if (nonNull(tagsAndDescription.tags()) &&
          nonNull(tagsAndDescription.fullDescription())) {

        final String shortTitle = getShortTitle(tagsAndDescription.fullDescription());
        final Boolean isText = isText(imageContent);
        final String textContents = null; // isText ? doOCR(imageContent) : null;

        if (isText && !tagsAndDescription.tags().contains(TEXT_TAG)) {
          // ensure it's in the labels
          tagsAndDescription.tags().add(TEXT_TAG);
        }

        // Convert to ImageInfo (without thumbnailName, which will be added later)
        return new ImageInfo(
            tagsAndDescription.tags(),
            tagsAndDescription.fullDescription(),
            shortTitle,
            isText,
            textContents,
            null);
      } else {
        // To see the actual prompts and model responses you can add this to application.properties
        // quarkus.log.category."dev.langchain4j".level=DEBUG
        System.out.println("Missing required fields in model response for image");

        // TODO: Figure out if there's a way to log this with Services
        // System.out.println("token usage: " + chatResponse.tokenUsage());
      }


      // we did not return a result, increment and decide if we should try again
      numbTimesTried++;
    }

    // if we got this far without returning a result it means we exhausted our retries
    throw new ExceededRetryLimitForModelRequest(
        "Could not generate Image Info after %s tries".formatted(NUM_MODEL_RETRIES));
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

  // When I try to extract more than tags and description in a single prompt the quality of the tags and description
  // degrades, so I'm splitting this into multiple passes
  private String getShortTitle(final String fullDescription) {
    final ImageInfoTitleService titleTx = AiServices.builder(ImageInfoTitleService.class)
        .chatModel(titleModel)
        .build();

    final ImageInfoTitleModelResponse titleInfo = titleTx.extractTitle(fullDescription);

    return titleInfo.shortTitle();
  }

  public Boolean isText(final ImageContent imageContent) {
    final ImageInfoIsTextService titleTx = AiServices.builder(ImageInfoIsTextService.class)
        .chatModel(isTextModel)
        .build();

    final ImageInfoIsTextModelResponse textInfo = titleTx.determineIfIsText(imageContent);

    // TODO: this should be a debug log, or threaded into final output
    System.out.println("text reasoning: " + textInfo.reason());

    return textInfo.isText();
  }

  public ImageInfoTagsAndDescriptionModelResponse getTagsAndDescription(final ImageContent imageContent) {
    final InitialImageInfoService titleTx = AiServices.builder(InitialImageInfoService.class)
        .chatModel(tagAndDescriptionModel)
        .build();

    //System.out.println("token usage: " + chatResponse.tokenUsage());

    return titleTx.getImageInfo(imageContent);
  }

  private ImageInfoFromDescriptionModelResponse extractImageInfoFromDescription(final String detailedDescription) {
    final ImageInfoFromDescriptionService service = AiServices.builder(ImageInfoFromDescriptionService.class)
        .chatModel(imageInfoFromDescriptionModel)
        .build();

    return service.extractImageInfoFromDetailedImageDescription(detailedDescription);
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
}
