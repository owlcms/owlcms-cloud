#!/bin/bash -
fly deploy . --local-only --app owlcms-cloud --config owlcms-cloud.toml --ha=false --image-label $1 --build-arg VERSION=$1