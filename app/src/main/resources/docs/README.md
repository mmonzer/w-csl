# Modify the documentation
## Organisation of docs
- `api` folder : contains the api.html if fetched. No need to touch, the script downloads it.
- `javadoc` folder : contains the javadoc. For the moment, it needs to be created and updated with a manual cmd or IntelliJ>Tools>Create JavaDoc. Sphinx will look for the index.html file of this folder.
- `sphinx` folder : contains the configuration for the creation of the documentation.
- `source` folder : contains the source of the documentation. The md/rst files will go in this folder. It must have a file `index` with the list of files to show.
- `script.sh` : executes the creation of the sphinx website (tested in linux).
- `index.html` : if it exists, its a slink to the doc website. The real file is at `sphinx/build/html/index.html`.

## Add new documentation
New .md or .rst files should go into `source` folder. They must be written down into the `index`, without extension.
If these files dont appear in the new website, you probably have some warnings in the creation with sphinx and need to recheck this.
```
README
Documentation
module/documentation
```
The output does not depend on the number of files, but on the content of the files.
For example, a top level title (in mardown marked with #) will create an entry in the index at the same level of other top level title, even it they are in other or same file.

