#!/bin/bash

# Get the ID of the latest workflow run using jq-style query
RUN_ID=$(gh run list --limit 1 --json databaseId --jq '.[0].databaseId')

if [ -z "$RUN_ID" ] || [ "$RUN_ID" == "null" ]; then
    echo "No recent build found."
    exit 1
fi

echo "Watching build ID: $RUN_ID..."
gh run watch "$RUN_ID"