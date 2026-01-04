package com.wininger.cli_image_labeler.image.tagging;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.output.JsonSchemas;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ImageInfoService {
     // Maximum dimension (width or height) for resized images to avoid timeouts
     private static final int MAX_IMAGE_DIMENSION = 600;

    private final OllamaChatModel model;

    public ImageInfoService() {
        final var responseFormat = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchemas.jsonSchemaFrom(ImageInfo.class).get())
            .build();

        model = OllamaChatModel.builder()
            .modelName("gemma3:4b")
            .baseUrl("http://localhost:11434/")
            .responseFormat(responseFormat)
            .build();
    }

    public ImageInfo generateImageInfo(final String imagePath) {
        final ImageContent imageContent = getImageContent(imagePath);

        final TextContent question = TextContent.from(
            "Generate an image description and tags based on this image. " +
            "You are a bot that tags images. You can create your own tags based on what you see but, " +
            "be sure to use the following tags if any apply: person, building, flower, flowers, tree, trees, animal, animals, chicken, bird. " +
            "Return a JSON object with 'tags' (array of strings) and 'fullDescription' (string)."
        );
        final UserMessage userMessage = UserMessage.from(question, imageContent);
        final ChatResponse chatResponse = model.chat(userMessage);

        // Parse the JSON response into ImageInfo
        final String jsonResponse = chatResponse.aiMessage().text();
        try {
            final ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(jsonResponse, ImageInfo.class);
        } catch (Exception e) {
            System.err.println("Failed to parse JSON response: " + e.getMessage());
            System.err.println("Raw response: " + jsonResponse);
            throw new RuntimeException("Could not parse image info from response", e);
        }
    }

    private ImageContent getImageContent(final String imagePath) {
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
            
            return ImageContent.from(base64Img, "image/jpeg");
        } catch(IOException ex) {
            throw new RuntimeException("Could not parse image: " + imagePath, ex);
        }
    }

    /**
     * Resizes an image to fit within the maximum dimension while maintaining aspect ratio.
     * If the image is already smaller, returns the original.
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
        } finally {
            g2d.dispose();
        }
        
        return resizedImage;
    }

    /**
     * Converts a BufferedImage to a JPEG byte array with specified quality.
     * This ensures good compression regardless of source format.
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
        } finally {
            writer.dispose();
        }
        
        return baos.toByteArray();
    }
}
