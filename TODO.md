## quarkus-cli-image-labeler

* move db file into ./data under quarkus-cli-image-labeler X
* Persist the resized image into ./data/thumbnails X
    * Store the name of the thumbnail in the image-info table X
* Capture metadata such as:
    * gps corrdiante,
    * time taken,
    * in additional add a catchall json field (if possilbe with sql lite), to just store all the file metadata
* Generate and store a title along with the description and the tags
* Add a flag, --update_existing which reprocessing images already in the database

## searchable-gallery

* Update types to account for the changes made above in the quarkus-cli-image-labeler
* Show the labels as pill boxes under each image
* add .gitignore to ./data under quarkus-cli-image-labeler and take our db out of git X
* Dispaly the thumbnails in the gallery rather than loading the complete images X
* Show the title below the image rather than the full description
* When you click on an image show the full description to the right of the window in a slide out
* Add buttons that let you easily copy the full path to the image or it's parent folder and/or open the folder or image
* Add search capability
    * search by tags
    * constrain by date taken ranges
    * search within scription

## top level

* Add a script that coppies the db and thumbnails from quarkus-cli-image-labeler into searchable-gallery
* Add a script that runs the image labeling for a specified directory and then runs the copy script