#!/bin/bash
git clone --sparse https://hub.spigotmc.org/stash/scm/spigot/craftbukkit.git
cd craftbukkit
git sparse-checkout set src
rsync -a src/ ../src/
cd ../
rm -rf craftbukkit
