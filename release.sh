#!/bin/bash -
VERSION=1.7.0
fly deploy . --local-only --app owlcms-cloud --config owlcms-cloud.toml --ha=false --image-label $VERSION --build-arg VERSION=$VERSION
gh release create $VERSION --title "Release $VERSION" --notes-file ReleaseNotes.md