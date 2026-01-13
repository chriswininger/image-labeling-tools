## quarkus-cli-image-labeler

* move db file into ./data under quarkus-cli-image-labeler X
* Persist the resized image into ./data/thumbnails X
    * Store the name of the thumbnail in the image-info table X
* Capture metadata such as:
    * gps corrdiante,
    * time image taken,
    * original name,
    * file system hash,
    * in additional add a catchall json field (if possilbe with sql lite), to just store all the file metadata
* Generate and store a title along with the description and the tags
* Add a flag, --update_existing which reprocessing images already in the database X

## searchable-gallery

* Update types to account for the changes made above in the quarkus-cli-image-labeler X
* Show the labels as pill boxes under each image X
* add .gitignore to ./data under quarkus-cli-image-labeler and take our db out of git X
* Dispaly the thumbnails in the gallery rather than loading the complete images X
* Show the title below the image rather than the full description
* When you click on an image show the full description to the right of the window in a slide out
* Add buttons that let you easily copy the full path to the image or it's parent folder and/or open the folder or image X
* Add search capability
    * search by tags X
    * constrain by date taken ranges
    * search within scription
* Create some kind of re-usable multi-select tool similar to htts://coreui.io/react/docs/forms/multi-select/ X
* Update electron app to return list of strings rather than comma separated list now that the backend is fixed

## top level

* Add a script that coppies the db and thumbnails from quarkus-cli-image-labeler into searchable-gallery X
* Add a script that runs the image labeling for a specified directory and then runs the copy script

## Problem images
* works, but not seeing chickens:  java -jar ./build/quarkus-app/quarkus-run.jar generate-image-tags "/Users/chriswininger/Pictures/test-images/26-01-07 13-31-25 4020.jpg"
* "/Users/chriswininger/Pictures/halloween-collage.png", "java.lang.RuntimeException"
  "/Users/chriswininger/Pictures/test-images/25-12-18 08-13-56 3828.png", "com.wininger.cli_image_labeler.image.tagging.exceptions.ExceededRetryLimitForModelRequest"
  "/Users/chriswininger/Pictures/test-images/25-12-21 16-00-01 3840.jpg", "com.wininger.cli_image_labeler.image.tagging.exceptions.ExceededRetryLimitForModelRequest"
* This is an odd /home/chris/Pictures/test-images/25-12-17 08-50-55 3819.png
    A photo showing a group of chickens in a red and white painted barn. The barn is located outdoors, likely on a farm. The chickens are scattered throughout the image. The scene appears bright and sunny.
* clearly chickens but :shrug: /home/chris/Pictures/test-images/24-11-14 14-47-58 8380.jpg
