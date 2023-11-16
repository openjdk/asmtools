#!/bin/bash

#####################################################################################################
# Symlinks are forbidden in openjdk project, thus we generate them on the fly together with pom.xml #
#####################################################################################################

set -eo pipefail

## resolve folder of this script, following all symlinks,
## http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SCRIPT_SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
  SCRIPT_SOURCE="$(readlink "$SCRIPT_SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ $SCRIPT_SOURCE != /* ]] && SCRIPT_SOURCE="$SCRIPT_DIR/$SCRIPT_SOURCE"
done
readonly SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
readonly PROJECT_DIR="$( readlink -f "$SCRIPT_DIR/.." )"

PRODUCT_INFO="$PROJECT_DIR/build/productinfo.properties"
BUILD_INFO="$PROJECT_DIR/build/build.properties"

function readProperty() {
  if [ "x${2}" = "x" ] ; then
    local file="$PRODUCT_INFO"
  else
    local file="${2}"
  fi
  cat "$file" | grep "$1\s*=" | sed "s/.*=\s*//"
}

PRODUCT_NAME=$(readProperty "PRODUCT_NAME")
PRODUCT_VERSION=$(readProperty "PRODUCT_VERSION")
PRODUCT_MILESTONE=$(readProperty "PRODUCT_MILESTONE")
PRODUCT_BUILDNUMBER=$(readProperty "PRODUCT_BUILDNUMBER")
PRODUCT_NAME_LONG=$(readProperty "PRODUCT_NAME_LONG")
TARGET=$(readProperty "javac.target.version" "$BUILD_INFO")
SOURCE=$(readProperty "javac.source.version" "$BUILD_INFO")

echo "Generating $SCRIPT_DIR/pom.xml for $PRODUCT_NAME $PRODUCT_VERSION $PRODUCT_MILESTONE $PRODUCT_BUILDNUMBER ($PRODUCT_NAME_LONG)"
cat "$SCRIPT_DIR/pom.xml.in" | \
sed "s/\[TARGET\]/$TARGET/g" | \
sed "s/\[SOURCE\]/$SOURCE/g" | \
sed "s/\[PRODUCT_NAME\]/$PRODUCT_NAME/g" | \
sed "s/\[PRODUCT_VERSION\]/$PRODUCT_VERSION/g" | \
sed "s/\[PRODUCT_MILESTONE\]/$PRODUCT_MILESTONE/g" | \
sed "s/\[PRODUCT_BUILDNUMBER\]/$PRODUCT_BUILDNUMBER/g" | \
sed "s/\[PRODUCT_NAME_LONG\]/$PRODUCT_NAME_LONG/g" > "$SCRIPT_DIR/pom.xml"
echo "Done"

echo "Creating symlinks to symulate maven structure"
FILES_LINKS="src/main/java/org=../../../../src/org/
src/test=../../test
src/main/resources/org/openjdk/asmtools/i18n.properties=../../../../../../../src/org/openjdk/asmtools/i18n.properties
src/main/resources/org/openjdk/asmtools/jasm/i18n.properties=../../../../../../../../src/org/openjdk/asmtools/jasm/i18n.properties
src/main/resources/org/openjdk/asmtools/jcoder/i18n.properties=../../../../../../../../src/org/openjdk/asmtools/jcoder/i18n.properties
src/main/resources/org/openjdk/asmtools/jdec/i18n.properties=../../../../../../../../src/org/openjdk/asmtools/jdec/i18n.properties
src/main/resources/org/openjdk/asmtools/jdis/i18n.properties=../../../../../../../../src/org/openjdk/asmtools/jdis/i18n.properties
src/main/resources/org/openjdk/asmtools/util/productinfo.properties=../../../../../../../../build/productinfo.properties"

pushd $SCRIPT_DIR > /dev/null
  for FILE_LINK in $FILES_LINKS ; do
    FILE=$(echo "$FILE_LINK" | sed "s/=.*//")
    LINK=$(echo "$FILE_LINK" | sed "s/.*=//")
    DIR=$(dirname "$FILE")
    NAME=$(basename "$FILE")
    if [ ! -e "$SCRIPT_DIR/$FILE" ] ; then
      mkdir -vp "$SCRIPT_DIR/$DIR"
      pushd "$SCRIPT_DIR/$DIR" > /dev/null
        echo "$SCRIPT_DIR/$DIR"
        ln -sv "$LINK" "$NAME"
      popd > /dev/null
    fi
  done
popd > /dev/null
echo "Done"
