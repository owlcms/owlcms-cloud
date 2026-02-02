#!/bin/bash -
VERSION=1.8.0

# Check if release already exists
if gh release view $VERSION --repo owlcms/owlcms-cloud &>/dev/null; then
    echo "Error: Release $VERSION already exists. Please update the VERSION number."
    exit 1
fi

fly deploy . --local-only --app owlcms-cloud --config owlcms-cloud.toml --ha=false --image-label $VERSION --build-arg VERSION=$VERSION
gh release create $VERSION --title "Release $VERSION" --notes-file ReleaseNotes.md