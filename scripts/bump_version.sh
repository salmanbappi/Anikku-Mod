#!/bin/bash

GRADLE_FILE="app/build.gradle.kts"

# Extract current versionCode and increment
CURRENT_VC=$(grep "versionCode =" $GRADLE_FILE | sed 's/[^0-9]*//g')
NEW_VC=$((CURRENT_VC + 1))

# Extract current versionName (e.g., 0.1.298)
CURRENT_VN=$(grep "versionName =" $GRADLE_FILE | sed 's/.*"\(.*\)".*/\1/')

# Clean up version name if it still has -MOD (safety check)
CLEAN_VN=$(echo $CURRENT_VN | sed 's/-MOD//')

# Extract components
MAJOR=$(echo $CLEAN_VN | cut -d'.' -f1)
MINOR=$(echo $CLEAN_VN | cut -d'.' -f2)
PATCH=$(echo $CLEAN_VN | cut -d'.' -f3)

# Increment patch
NEW_PATCH=$((PATCH + 1))
NEW_VN="${MAJOR}.${MINOR}.${NEW_PATCH}"

# Update the file
sed -i "s/versionCode = $CURRENT_VC/versionCode = $NEW_VC/" $GRADLE_FILE
sed -i "s/versionName = \"$CURRENT_VN\"/versionName = \"$NEW_VN\"/" $GRADLE_FILE

echo "Bumped versionCode from $CURRENT_VC to $NEW_VC"
echo "Bumped versionName from $CURRENT_VN to $NEW_VN"

# Set environment variable for GitHub Actions
if [ -n "$GITHUB_ENV" ]; then
    echo "NEW_VN=$NEW_VN" >> $GITHUB_ENV
fi