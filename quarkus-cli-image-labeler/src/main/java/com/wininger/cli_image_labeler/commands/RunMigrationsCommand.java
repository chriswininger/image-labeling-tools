package com.wininger.cli_image_labeler.commands;

import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import picocli.CommandLine.Command;

@Command(name = "run-migrations", mixinStandardHelpOptions = true,
         description = "Run database migrations")
public class RunMigrationsCommand implements Runnable {

    @Inject
    Flyway flyway;

    @Override
    public void run() {
        // Flyway migrations run automatically at startup, so if we made it this far,
        // the migrations have been run successfully
        System.out.println("The database is up to date.");

        // Print information about the last applied migration
        MigrationInfo[] appliedMigrations = flyway.info().applied();
        if (appliedMigrations.length > 0) {
            MigrationInfo lastMigration = appliedMigrations[appliedMigrations.length - 1];
            System.out.println("\nLast applied migration:");
            System.out.println("  Version: " + lastMigration.getVersion());
            System.out.println("  Description: " + lastMigration.getDescription());
            System.out.println("  Type: " + lastMigration.getType());
            System.out.println("  Installed On: " + lastMigration.getInstalledOn());
            System.out.println("  Execution Time: " + lastMigration.getExecutionTime() + "ms");
        } else {
            System.out.println("\nNo migrations have been applied yet.");
        }

        // Print summary
        MigrationInfo[] pendingMigrations = flyway.info().pending();
        System.out.println("\nMigration Summary:");
        System.out.println("  Applied: " + appliedMigrations.length);
        System.out.println("  Pending: " + pendingMigrations.length);
    }
}
