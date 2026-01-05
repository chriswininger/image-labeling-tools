package com.wininger.cli_image_labeler;

import com.wininger.cli_image_labeler.image.tagging.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.ImageInfoService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "generate-image-tags", mixinStandardHelpOptions = true)
public class GenerateImageTagsCommand implements Runnable {
    @Parameters(paramLabel = "<image-path>", description = "The path to an image you want to identify")
    String imagePath;

    private final ImageInfoService imageInfoService;

    @Inject
    public GenerateImageTagsCommand(final ImageInfoService imageInfoService) {
        this.imageInfoService = imageInfoService;
    }

    @Override
    public void run() {
        final ImageInfo imageInfo = imageInfoService.generateImageInfo(imagePath, false);
        
        System.out.println("Image Info: " + imageInfo);
    }
}
