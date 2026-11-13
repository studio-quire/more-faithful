"""Contact sheets from a look run's pictures (look.sh), for judging at a glance: the back, the three side by side from each angle, the golem alone enlarged, moving and against stone, and
near. Usage: look_sheets.py <folder of look-*.png>"""
import os
import sys

from PIL import Image

HERE = sys.argv[1]


def crop(name, box, scale=1):
    t = Image.open(os.path.join(HERE, "look-" + name + ".png")).convert("RGB").crop(box)
    return t.resize((t.width * scale, t.height * scale), Image.NEAREST) if scale != 1 else t


def sheet(name, tiles, columns, gap=6):
    w, h = tiles[0].size
    rows = (len(tiles) + columns - 1) // columns
    out = Image.new("RGB", (columns * w + (columns + 1) * gap, rows * h + (rows + 1) * gap), (24, 24, 28))
    for i, t in enumerate(tiles):
        out.paste(t, (gap + (i % columns) * (w + gap), gap + (i // columns) * (h + gap)))
    out.save(os.path.join(HERE, "sheet-" + name + ".png"))


# The frames are 854 by 480 with whoever is pictured in the middle.
THREE = (107, 40, 747, 390)
SOLO = (352, 110, 512, 370)
CLOSE = (227, 0, 627, 450)
ANGLES = ["front", "front-empty", "side", "side-empty", "quarter", "quarter-empty", "back-empty", "quarter-back-empty"]
sheet("back", [crop(n, CLOSE) for n in ["close-back", "close-quarter-back", "close-quarter-back-left"]], 3)
sheet("back-beside-zombie-and-player", [crop(n, THREE) for n in ["back-empty", "back", "quarter-back-empty", "quarter-back", "quarter-back-left"]], 2)
sheet("beside-zombie-and-player", [crop(n, THREE) for n in ANGLES], 2)
sheet("golem-angles", [crop(n, SOLO, 2) for n in ANGLES], 4)
sheet("moving-and-stone", [crop(n, THREE) for n in ["walking", "walking-empty", "swing", "stone-front", "stone-quarter", "stone-crew"]], 2)
sheet("close", [crop(n, CLOSE) for n in ["close-side", "close-empty", "close", "close-back"]], 4)
