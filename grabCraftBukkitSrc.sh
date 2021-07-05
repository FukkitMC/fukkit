#!/bin/bash
cd src
git clone --sparse https://hub.spigotmc.org/stash/scm/spigot/craftbukkit.git
cd craftbukkit
git sparse-checkout set src
mv src/* ./
rmdir src
