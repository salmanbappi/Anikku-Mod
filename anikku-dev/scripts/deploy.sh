#!/bin/bash
set -e

MESSAGE="$1"
if [ -z "$MESSAGE" ]; then
  MESSAGE="chore: Update"
fi

echo "🚀 Committing and pushing..."
git add .
git commit -m "$MESSAGE" || echo "Nothing to commit"
git push

echo "⏳ Waiting for workflow to start..."
sleep 5

# Get the latest run ID
RUN_ID=$(gh run list --limit 1 --json databaseId --jq '.[0].databaseId')
echo "👀 Watching Build ID: $RUN_ID"

# Watch the build (this blocks until done)
gh run watch "$RUN_ID"

# Get the final status
CONCLUSION=$(gh run view "$RUN_ID" --json conclusion --jq '.conclusion')

if [ "$CONCLUSION" == "success" ]; then
  echo "✅ Build SUCCESS!"
else
  echo "❌ Build FAILED!"
  echo "🔍 Fetching failure logs..."
  gh run view "$RUN_ID" --log | grep -C 10 "Error" > build_error.log
  echo "Logs saved to build_error.log"
  exit 1
fi
