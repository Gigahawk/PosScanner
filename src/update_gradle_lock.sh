#!/usr/bin/env bash

gradle2nix --task="resolveAllArtifacts,:app:assembleDebug,:app:assembleRelease"
