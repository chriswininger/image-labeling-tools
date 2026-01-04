package com.wininger.cli_image_labeler;

import com.wininger.cli_image_labeler.image.tagging.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.ImageInfoService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "generate-image-tags", mixinStandardHelpOptions = true)
public class GenerateImageTagsCommand implements Runnable {
    @Parameters(paramLabel = "<image-path>", description = "The path to an image you want to identify")
    String imagePath;

    @Override
    public void run() {
        final ImageInfoService imageInfoService = new ImageInfoService();
        final ImageInfo imageInfo = imageInfoService.generateImageInfo(imagePath);
        
        System.out.println("Image Info: " + imageInfo);
    }
}
