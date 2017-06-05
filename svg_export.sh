#!/bin/bash
mkdir -p icons/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}
inkscape -z -f $1 -e icons/ic_launcher.png -w 512 -h 512
inkscape -z -f $1 -e icons/mipmap-mdpi/ic_launcher.png -w 48 -h 48
inkscape -z -f $1 -e icons/mipmap-hdpi/ic_launcher.png -w 72 -h 72
inkscape -z -f $1 -e icons/mipmap-xhdpi/ic_launcher.png -w 96 -h 96
inkscape -z -f $1 -e icons/mipmap-xxhdpi/ic_launcher.png -w 144 -h 144
inkscape -z -f $1 -e icons/mipmap-xxxhdpi/ic_launcher.png -w 192 -h 192
