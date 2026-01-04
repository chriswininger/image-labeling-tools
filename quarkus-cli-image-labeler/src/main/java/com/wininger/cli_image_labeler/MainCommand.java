package com.wininger.cli_image_labeler;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(name = "app", mixinStandardHelpOptions = true, 
         subcommands = { 
             GenerateImageTagsCommand.class, 
             GenerateImageTagsForDirectoryCommand.class,
             WriteTagsToLocalDbCommand.class
         })
public class MainCommand implements Runnable {

    @Override
    public void run() {
        // If no subcommand is specified, Picocli will show the usage help automatically
    }
}

