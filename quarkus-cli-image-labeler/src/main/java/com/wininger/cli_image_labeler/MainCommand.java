package com.wininger.cli_image_labeler;

import com.wininger.cli_image_labeler.commands.GenerateImageTagsCommand;
import com.wininger.cli_image_labeler.commands.GenerateImageTagsForDirectoryCommand;
import com.wininger.cli_image_labeler.commands.RandomizeGpsCoordinatesCommand;
import com.wininger.cli_image_labeler.commands.ReadFileMetadataCommand;
import com.wininger.cli_image_labeler.commands.RunMigrationsCommand;
import com.wininger.cli_image_labeler.commands.WriteTagsToLocalDbCommand;
import com.wininger.cli_image_labeler.setup.DataDirectoryInitializer;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(name = "app", mixinStandardHelpOptions = true,
         subcommands = {
             GenerateImageTagsCommand.class,
             GenerateImageTagsForDirectoryCommand.class,
             RandomizeGpsCoordinatesCommand.class,
             ReadFileMetadataCommand.class,
             RunMigrationsCommand.class,
             WriteTagsToLocalDbCommand.class
         })
public class MainCommand implements Runnable {

    static {
        // Ensure data directories are created before Flyway runs
        DataDirectoryInitializer.ensureDirectoriesExist();
    }

    @Override
    public void run() {
        // If no subcommand is specified, Picocli will show the usage help automatically
    }
}

