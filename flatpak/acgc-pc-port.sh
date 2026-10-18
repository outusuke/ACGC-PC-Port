#!/bin/sh
# The game resolves rom/, save/ and settings.ini relative to the working directory.
data="${XDG_DATA_HOME:-$HOME/.local/share}/acgc-pc-port"
mkdir -p "$data/rom" "$data/save" "$data/texture_pack"
ln -sfn /app/bin/shaders "$data/shaders"
cd "$data" || exit 1
exec /app/bin/AnimalCrossing "$@"
