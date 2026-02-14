Image Labeling Tools
====================

I love that Google Photos makes images searchable, but I hate uploading my memories to the cloud. Given the
availability of open-weight AI models, surely this can be done locally.

Why can't I simply run a command [is-pic-with-person ./my-img.jpeg](https://github.com/chriswininger/is-pic-with-person/tree/6afb00115e745a79f399c01df982e847cd403305) and get results that can
be piped to other commands?

I tried this 5 years ago with the models that were available then and had isPerson working, but it was not
very versatile. The models output a static set of tags. If they weren't trained to tag lizard, for example, no lizards
without retraining.

Can modern multi-modal LLMs do better?

This repository is a catch-all to explore this question. As concepts mature they may split out into their own repos.

![Screenshot_2026-02-14_13-00-48-scale.png](searchable-gallery/readme-assets/Screenshot_2026-02-14_13-00-48.png)

## Structure

### [quarkus-cli-image-labeler](./quarkus-cli-image-labeler/README.md):

A command line tool built in Java with Quarkus. It leverages langchain4j and the Ollama runtime to expose commands for
labeling images.

The command `write-tags-to-local-db`, for example creates SQLite database filled with tags and descriptions for all your
images along with thumbnail sized copies of the images processed. This database can be read and display by
[searchable-gallery](./searchable-gallery/README.md).

For more on using this tool see [the usage guide](./quarkus-cli-image-labeler/Usage.md).

### [searchable-gallery](./searchable-gallery/README.md)

A search gallery of images using the database produced by quarkus-cli-image-labeler. It allows you to search your
images by tags

## Thoughts

The system requirements of LLMs limit adoption of purely local AI solutions like this. This may change in the future,
though with the current RAM crisis it may take time. That said for image labeling I have been having good luck using
Gemma 3(4B) which seems to punch above its weight. It runs usable fast on a MacBook Pro with an M1, cheap, similarly
on a laptop with and RTX 2070 and extremely well on an RTX 5070 TI. These are not bargain-basement PCs, but they are
consumer grade.
