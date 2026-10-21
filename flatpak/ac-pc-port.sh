#!/bin/sh
# The game resolves rom/, save/ and shaders/ relative to the working directory.
data="${XDG_DATA_HOME:-$HOME/.local/share}"
mkdir -p "$data/rom" "$data/save" "$data/texture_pack"
ln -sfn /app/share/ac-pc-port/shaders "$data/shaders"
cd "$data" || exit 1
case " $* " in
  *" --verbose "*|*" -v "*)
    /app/bin/AnimalCrossing "$@" 2>&1 | tee "$data/verbose.log"
    exit ;;
esac
exec /app/bin/AnimalCrossing "$@"
