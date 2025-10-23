#!/bin/bash

# Check current dimensions of mmi_app_icon.png
echo "Checking current dimensions of mmi_app_icon.png..."
sips -g pixelWidth -g pixelHeight app/src/main/res/raw/mmi_app_icon.png

# Create backup
echo "Creating backup..."
cp app/src/main/res/raw/mmi_app_icon.png app/src/main/res/raw/mmi_app_icon_backup.png

# Resize to optimal size for adaptive icons (192x192 for XXXHDPI)
echo "Resizing to 192x192 pixels (optimal for XXXHDPI adaptive icons)..."
sips --resampleWidth 192 --resampleHeight 192 app/src/main/res/raw/mmi_app_icon.png

# Verify new dimensions
echo "New dimensions:"
sips -g pixelWidth -g pixelHeight app/src/main/res/raw/mmi_app_icon.png

echo "Icon resized successfully! The 192x192 size is optimal for:"
echo "- XXXHDPI adaptive icons (48dp x 4 = 192px)"
echo "- Better performance and smaller APK size"
echo "- Proper display within the 66dp safe zone"