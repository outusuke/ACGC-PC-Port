#!/bin/sh
set -e

version=2.32.10
sha256=5f5993c530f084535c65a6879e9b26ad441169b3e25d789d83287040a9ca5165
dir="$(cd "$(dirname "$0")" && pwd)/third_party"

[ -d "$dir/SDL2-$version" ] && exit 0
mkdir -p "$dir"
curl -fsSL -o "$dir/sdl2.tar.gz" "https://github.com/libsdl-org/SDL/releases/download/release-$version/SDL2-$version.tar.gz"
echo "$sha256  $dir/sdl2.tar.gz" | sha256sum -c -
tar -xzf "$dir/sdl2.tar.gz" -C "$dir"
rm "$dir/sdl2.tar.gz"
