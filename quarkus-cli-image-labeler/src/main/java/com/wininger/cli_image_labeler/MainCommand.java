package com.wininger.cli_image_labeler;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@TopCommand
@Command(name = "app", mixinStandardHelpOptions = true, 
         subcommands = {GreetingCommand.class, TransactionCommand.class})
public class MainCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        // Default to greeting command when no subcommand is specified
        // This preserves the behavior where running with no args runs greeting
        GreetingCommand greeting = new GreetingCommand();
        greeting.name = "picocli"; // Set default value
        greeting.run();
    }
}

