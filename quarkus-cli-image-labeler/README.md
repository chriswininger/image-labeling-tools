quarkus-cli-image-labeler
===============================

This is a command line application used to generate tags and descriptions of images. It also captures image metadata.
It can output this data over standard out or persist it to a SQLite database.

For more information see: [Usage.md](Usage.md)

## Building

Build the application:
```shell script
./gradlew build
```
or  (no tests)
```shell script
./gradlew build -x test
```
Run the publish script (keeps the frontend schema up to date for integration tests):

```shell script
./publish-test-database-for-searchable-gallery.sh
```

### Populate a SQLite database with image information for a directory or individual image
`java -jar ./build/quarkus-app/quarkus-run.jar write-tags-to-local-db ~/Pictures/test-images/`

## Schema Changes
When you make changes to the database schema via migrations in this module, you need to publish those changes to the 
`searchable-gallery` module for integration testing.

To publish them run `./publish-test-database-for-searchable-gallery.sh`

## Dependencies
This project uses:
* [Quarkus](https://quarkus.io/).
* ([guide](https://quarkus.io/guides/picocli)).
* Ollama (see more below)

### Ollama

These models need to be available in ollama

* gemma3:4b: For image description and tagging
* deepseek-ocr:3b: For OCR, currently disabled
* nomic-embed-text: For semantic similarity calculations

example: `ollama pull nomic-embed-text`

For more information on OLLAMA setup and management see: [Ollama_Notes.md](Ollama_Notes.md)

## Notes about Packaging and running the application

Building produces `quarkus-run.jar` file in the `build/quarkus-app/` directory.

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
