#!/bin/bash

# Get the ID of the latest run (including in_progress)
RUN_ID=$(gh run list --limit 1 --json databaseId --jq '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
  echo "No workflow runs found."
  exit 1
fi

echo "Monitoring Run ID: $RUN_ID"

while true; do
  # Fetch status and conclusion
  STATUS_JSON=$(gh run view "$RUN_ID" --json status,conclusion,url)
  STATUS=$(echo "$STATUS_JSON" | jq -r '.status')
  CONCLUSION=$(echo "$STATUS_JSON" | jq -r '.conclusion')
  URL=$(echo "$STATUS_JSON" | jq -r '.url')

  if [ "$STATUS" == "completed" ]; then
    if [ "$CONCLUSION" == "success" ]; then
      echo "✅ Build SUCCEEDED!"
      echo "URL: $URL"
      exit 0
    elif [ "$CONCLUSION" == "failure" ] || [ "$CONCLUSION" == "cancelled" ] || [ "$CONCLUSION" == "timed_out" ]; then
      echo "❌ Build FAILED with conclusion: $CONCLUSION"
      echo "Fetching failed logs..."
      gh run view "$RUN_ID" --log-failed
      exit 1
    else
      echo "Build finished with unknown conclusion: $CONCLUSION"
      exit 1
    fi
  else
    echo "Build is in progress... (Status: $STATUS)"
    sleep 10
  fi
done
