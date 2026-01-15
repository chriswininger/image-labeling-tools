package com.wininger.cli_image_labeler.commands;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Command(name = "read-file-metadata", mixinStandardHelpOptions = true,
         description = "Reads and displays all metadata from an image file")
public class ReadFileMetadataCommand implements Runnable {

    @Parameters(paramLabel = "<image-path>", description = "The path to an image file to read metadata from")
    String imagePath;

    @Override
    public void run() {
        try {
            final Map<String, String> metadata = extractAllMetadata(imagePath);

            System.out.println("Metadata for: " + imagePath);
            System.out.println("========================================");

            metadata.forEach((key, value) ->
                System.out.printf("%s: %s%n", key, value)
            );

            System.out.println("========================================");
            System.out.println("Total entries: " + metadata.size());

        } catch (ImageProcessingException e) {
            System.err.println("Error processing image: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Extracts all metadata from an image file.
     *
     * @param imagePath the path to the image file
     * @return a map of all metadata key-value pairs, with keys prefixed by directory name
     */
    public static Map<String, String> extractAllMetadata(final String imagePath)
            throws ImageProcessingException, IOException {

        final File imageFile = new File(imagePath);
        final Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

        return StreamSupport.stream(metadata.getDirectories().spliterator(), false)
            .flatMap(directory ->
                directory.getTags().stream()
                    .map(tag -> Map.entry(
                        directory.getName() + "." + tag.getTagName(),
                        tag.getDescription() != null ? tag.getDescription() : ""
                    ))
            )
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> right  // in case of duplicate keys
            ));
    }
}
