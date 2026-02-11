#!/bin/bash

GRADLE_FILE="app/build.gradle.kts"

# Extract current versionCode and increment
CURRENT_VC=$(grep "versionCode =" $GRADLE_FILE | sed 's/[^0-9]*//g')
NEW_VC=$((CURRENT_VC + 1))

# Extract current versionName (e.g., 0.1.8)
current_version=$(grep "versionName =" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')

# Assumes format like X.Y.Z
# Assumes format like X.Y.Z-MOD
BASE_VN=$(echo $CURRENT_VN | cut -d'-' -f1)
SUFFIX=$(echo $CURRENT_VN | cut -d'-' -f2-)

MAJOR=$(echo $BASE_VN | cut -d'.' -f1)
MINOR=$(echo $BASE_VN | cut -d'.' -f2)
PATCH=$(echo $BASE_VN | cut -d'.' -f3)

NEW_PATCH=$((PATCH + 1))
NEW_VN="${MAJOR}.${MINOR}.${NEW_PATCH}-${SUFFIX}"

# Update the file
sed -i "s/versionCode = $CURRENT_VC/versionCode = $NEW_VC/" $GRADLE_FILE
sed -i "s/versionName = \"$CURRENT_VN\"/versionName = \"$NEW_VN\"/" $GRADLE_FILE

echo "Bumped versionCode from $CURRENT_VC to $NEW_VC"
echo "Bumped versionName from $CURRENT_VN to $NEW_VN"
echo "NEW_VN=$NEW_VN" >> $GITHUB_ENV
