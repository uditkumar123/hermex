param(
    [switch]$ConvertFeatureGraphic
)

# Requires: Python + Pillow
# Install: pip install Pillow

if ($ConvertFeatureGraphic) {
    python -c "
from PIL import Image
img = Image.open('docs/play-assets/feature-graphic.svg')
# SVG rasterization needs cairosvg or svglib
print('Use an online converter (e.g. svgtopng.com) or install cairosvg: pip install cairosvg')
"
}
