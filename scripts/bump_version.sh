#!/bin/bash

# Usage: ./scripts/bump_version.sh [patch|minor|major] OR ./scripts/bump_version.sh 1.2.3

TYPE=$1
if [ -z "$TYPE" ]; then
  TYPE="patch"
fi

GRADLE_FILE="app/build.gradle.kts"

# Extract current version
CURRENT_VERSION=$(grep 'versionName = "' $GRADLE_FILE | cut -d'"' -f2)
CURRENT_CODE=$(grep 'versionCode =' $GRADLE_FILE | awk '{print $3}')

echo "Current Version: $CURRENT_VERSION ($CURRENT_CODE)"

if [[ "$TYPE" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  NEW_VERSION=$TYPE
else
  # Split version into parts
  IFS='.' read -r -a PARTS <<< "$CURRENT_VERSION"
  MAJOR=${PARTS[0]}
  MINOR=${PARTS[1]}
  PATCH=${PARTS[2]}

  if [ "$TYPE" == "major" ]; then
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
  elif [ "$TYPE" == "minor" ]; then
    MINOR=$((MINOR + 1))
    PATCH=0
  else
    PATCH=$((PATCH + 1))
  fi
  NEW_VERSION="$MAJOR.$MINOR.$PATCH"
fi

NEW_CODE=$((CURRENT_CODE + 1))

echo "🚀 Bumping to: $NEW_VERSION ($NEW_CODE)"

# Update gradle file using sed (compatible with Linux/Termux)
sed -i "s/versionName = \"$CURRENT_VERSION\"/versionName = \"$NEW_VERSION\"/" $GRADLE_FILE
sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" $GRADLE_FILE

echo "✅ Updated $GRADLE_FILE"

# Git commit and tag
git add $GRADLE_FILE
git commit -m "chore: Bump version to v$NEW_VERSION"
git tag "v$NEW_VERSION"

echo "🎉 Ready to push! Run: git push && git push --tags"
