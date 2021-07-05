#!/bin/bash
git clone --sparse https://hub.spigotmc.org/stash/scm/spigot/craftbukkit.git 
mv craftbukkit nms-patches
cd nms-patches
git sparse-checkout set nms-patches
mv nms-patches/* ./
rmdir nms-patches
