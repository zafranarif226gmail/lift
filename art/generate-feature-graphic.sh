#!/bin/bash

echo "Generating feature graphics to ~/lift-icons/lift-feature-graphic.png..."
mkdir -p ~/lift-icons/
rsvg-convert feature-graphic.svg > ~/lift-icons/feature-graphic.png
