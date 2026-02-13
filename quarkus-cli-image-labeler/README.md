quarkus-cli-image-labeler
===============================

## Building

Quick Build (no tests): `./gradlew build -x test`

## Models Used

These models need to be available in ollama

* gemma3:4b: For image description and tagging
* deepseek-ocr:3b: For OCR, currently disabled
* nomic-embed-text: For semantic similarity calculations

example: `ollama pull nomic-embed-text`

## Running Examples

### Populate a SqlLite database with image information for a directory or individual image
`java -jar ./build/quarkus-app/quarkus-run.jar write-tags-to-local-db ~/Pictures/test-images/`

## Schema Changes

When you make changes to the database schema via migrations in this module, you need to publish those changes to the 
`searchable-gallery` module for integration testing.

To publish them run `./publish-test-database-for-searchable-gallery.sh`

## Quarkus

This project uses [Quarkus](https://quarkus.io/).

1. First, build the application:

```shell script
./gradlew build
```

2. Run the publish script to apply migrations to the test database:

```shell script
./publish-test-database-for-searchable-gallery.sh
```

This script runs the migrations against `../searchable-gallery/test-data/` so that the frontend tests use an up-to-date schema.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/quarkus-cli-image-labeler-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.

## Related Guides

- Picocli ([guide](https://quarkus.io/guides/picocli)): Develop command line applications with Picocli

## Provided Code

### Picocli Example

Hello and goodbye are civilization fundamentals. Let's not forget it with this example picocli application by changing the <code>command</code> and <code>parameters</code>.

[Related guide section...](https://quarkus.io/guides/picocli#command-line-application-with-multiple-commands)

Also for picocli applications the dev mode is supported. When running dev mode, the picocli application is executed and on press of the Enter key, is restarted.

As picocli applications will often require arguments to be passed on the commandline, this is also possible in dev mode via:

```shell script
./gradlew quarkusDev --quarkus-args='Quarky'
```
