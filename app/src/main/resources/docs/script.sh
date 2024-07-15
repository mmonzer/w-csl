#!/bin/bash

DOC_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
CURRENT_PATH=$( pwd -P )

cd $DOC_PATH


# Check if pyenv installed
INSTALLED=$(dpkg -l | grep python3-venv)
if [ ! -n "$INSTALLED" ];
then
	echo 'ERROR : python3-venv needed but not installed' 
	exit
fi


## Get into the environnement
cd sphinx
CREATED=$(ls -a | grep .venv)
if [ ! -n "$CREATED" ]
then
    python3 -m venv .venv
    echo 'New pyenv created at .venv'
fi

source .venv/bin/activate


# Check if pip installed
INSTALLED=$(pip list 2> /dev/null)
if [ ! -n "$INSTALLED" ];
then
	echo 'INFO : installing pip.'
	python3 -m ensurepip --upgrade
	echo 'INFO : pip installed successfully.'
fi

# Check if sphinx installed
INSTALLED=$(pip list | grep -F sphinx)
if [ ! -n "$INSTALLED" ];
then
	echo 'INFO : installing sphinx.'
	pip install Sphinx
	echo 'INFO : sphinx installed successfully.'
fi
cd ..

# Update apihelp
cd api
if hash wget
then
	wget localhost:9900/apihelp -q -t 1
	mv ./apihelp ./apihelp.html 2> /dev/null
fi
# Check if api exists
if [ -f apihelp.html ]; 
then
	touch ../sphinx/source/APIDOC.rst
	echo 'INFO : Found API doc.'
else
	rm ../sphinx/source/APIDOC.rst 2> /dev/null
	echo 'WARNING : Failed to download the API doc.'
fi
cd ..

# javadoc
cd javadoc
if [ -f index.html ]; 
then
	touch ../sphinx/source/JAVADOC.rst
	echo 'INFO : Found Java doc.'
else
	rm ../sphinx/source/JAVADOC.rst 2> /dev/null
	echo 'WARNING : Failed to get the Java doc. It needs to be created by the user in the javadoc folder.'
fi
cd ..


# get source
cp -r source sphinx/source/
cp sphinx/source/index.default.rst sphinx/source/index.rst
while IFS= read -r line; do
    echo "   source/$line" >> sphinx/source/index.rst
done < sphinx/source/source/index


# Install sphinx
cd sphinx
# install required packages
if ! pip install sphinx 1> /dev/null;
then 
	echo 'ERROR : could not install sphinx.' 
	exit
fi
echo 'INFO : Package sphinx installed.'
if ! pip install myst-parser 1> /dev/null;
then 
	echo 'ERROR : could not install myst-parser.' 
	exit
fi
echo 'INFO : Package myst-parser installed.'



# build doc
if ! sphinx-build -M html source build
then 
	echo 'ERROR : sphinx building failed.' 
	exit
fi

# Create shortcut
rm ../index.html 2> /dev/null
rm -r source/source 2> /dev/null
rm -r source/APIDOC.rst 2> /dev/null
rm -r source/JAVADOC.rst 2> /dev/null

ln -s ./sphinx/build/html/index.html ../index.html

echo 'INFO : page locally available at '$DOC_PATH'/index.html'
cd $CURRENT_PATH



	

