Image Labeling Tools
====================

I love how Google Photos makes your images searchable, but I hate uploading all my personal memories to the cloud. Given the wide availability of AI models, surely we can do this locally.

Why can't I simply run a command on the terminal [is-pic-with-person ./my-img.jpeg](https://github.com/chriswininger/is-pic-with-person/tree/6afb00115e745a79f399c01df982e847cd403305) and get back a result that can be piped to other commands?

I've explored this 5 years ago with the models that were available then and had isPerson working fairly well, but those models were not very versatile. They output a static set of tags. Can modern multi-modal LLMs do better?

This repo is going to be a catch-all to explore this question. As concepts mature they may split out into their own repos.

## Structure

### [quarkus-cli-image-labeler](./quarkus-cli-image-labeler):

A command line tool built in Java with Quarkus. It leverages langchain4j and the Ollama runtime to expose commands for labeling images.

If you run the command `generate-image-tags-for-directory` it will produce a SQLite database and thumbnail folder under the data directory. These can be copied to the data directory of [searchable-gallery](./searchable-gallery)
and browsed visually.

### [searchable-gallery](./searchable-gallery)

This uses the output from the command-line tool above, exposing a gallery that you can browse and search by tags.

## Thoughts

Eventually I may fuse some of these into a more coherent product, although the system requirements of these models make it a bit challenging. Ideally you'd have a daemon and some hooks into the file system to automatically process new images
but this would eat a lot of system resources. So far I've been working with Gemma 3 (4B), which is much lighter for the quality than other models I've tried, but still not going to run well on a bargain basement PC.
