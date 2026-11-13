"""Draws the golem's skin and its cracks, the boundary pole and the core's icon, in beta's manner: a few tones, hard pixels, a little noise. Rerun to change them; the PNGs are what ships.

The skin is drawn face by face from rows of letters, one letter a pixel, so a face reads here as it does in the game. The boxes are GolemModel's."""
import random
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/golems/textures"

# Iron as beta draws it (the block, the armour, the door): pale, with white where it catches the light and a darker rim to each piece.
# Lightest to darkest, the shade leaning a little blue as it deepens. A mob is drawn at about three quarters of its skin's brightness on
# its front and half on its sides, so the iron itself reads as a light grey in the world, lighter than stone, and only the white as white.
# "d" is the bands: the seam at the hips, the cuffs, the feet. They were a dark grey (104, 110, 123) until the user, seeing the golems in the
# game, asked for them near the skin's lighter lines and a little darker (2026-11-10). "e" is the top of the eyes, which went lighter with
# the bands and then a little darker again. Both are the user's final call.
TONES = {
    "W": (252, 252, 252),
    "H": (234, 235, 237),
    "h": (216, 218, 221),
    ".": (198, 200, 204),
    ",": (184, 187, 192),
    "s": (167, 171, 178),
    "S": (149, 154, 163),
    "m": (128, 134, 145),
    "d": (146, 151, 161),
    "e": (110, 116, 129),
    "D": (82, 87, 100),
    "k": (30, 32, 40),
}
NOISE = 3


def box_faces(u, v, w, h, d):
    """Where beta's ModelPart reads each face of a box from. Right is the face on the model's low x, a right limb's outer side."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def paint(img, rng, face, rows):
    """A face from its rows, top row first; spaces are only for reading. One row alone fills the face."""
    x, y, w, h = face
    rows = [r.replace(" ", "") for r in rows]
    if len(rows) == 1:
        rows = rows * h
    assert len(rows) == h and all(len(r) == w for r in rows), (face, rows)
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            n = 0 if ch == "k" else rng.randint(-NOISE, NOISE)
            img.putpixel((x + i, y + j), tuple(min(255, c + n) for c in TONES[ch]) + (255,))


def flip(rows):
    return [r.replace(" ", "")[::-1] for r in rows]


# How many rows of a leg are foot, under its band. It was 2; the user asked for the bands a little higher, for a larger foot, and of 3 and 4
# chose 3 (2026-11-10). The one crack that runs down a leg ends just over the band wherever it is.
FOOT = 3


def skin(foot=None, out=None):
    foot = foot or FOOT
    rng = random.Random(1907)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # The boxes are beta's own biped's, exactly, where its zombie and player keep theirs on the skin: a head 8 each way, a body 8 by 12 by
    # 4, limbs 4 by 12 by 4. No hat.

    # The head. A brow that catches the light, two square eyes under it in a mid grey, a shade deeper along their top where the brow
    # shades them, and no mouth. A side's columns run back to front on the right and front to back on the left.
    head = box_faces(0, 0, 8, 8, 8)
    paint(img, rng, head["front"], [
        "s , . h h . , s",
        ", . . h h . . ,",
        ", . h H H h . ,",
        ". h H W W H h ,",
        "s e e , , e e s",
        ", m m . . m m ,",
        "s , . . . . , s",
        "m S s s s s S m",
    ])
    # The eyes' shade turns the corner onto each side as a soft mark that fades as it runs back, no darker than the front's own edge: it
    # was a darker stroke from when the eyes were near black, and the user saw it no longer followed from the face (2026-11-10). Over it the
    # brow's light turns the corner too, as a band one row high: it had a pixel standing on it, an L, which they had taken off, and one pixel of it
    # a step brighter than the rest, which they had evened out.
    side = [
        "s , . h h . , s",
        ", . . . . . . ,",
        ", . . . . . . ,",
        ", . . . . h h .",
        "s , . . . , s s",
        "s , . . . . . ,",
        "s , , . . . , s",
        "m S s s s s S m",
    ]
    paint(img, rng, head["right"], side)
    paint(img, rng, head["left"], flip(side))
    # The back is plain worked iron: its top edge catching the light as the sides' do, a little unevenness, nothing with a shape of its own.
    paint(img, rng, head["back"], [
        "s , . h h . , s",
        ", . . . . . . ,",
        ", . . . . . . ,",
        ", . . . . . , ,",
        ", . . . . . . ,",
        ", , . . . . . ,",
        "s , . . . . , s",
        "m S s s s s S m",
    ])
    # The top's last row is its front edge.
    paint(img, rng, head["top"], [
        "s , , , , , , s",
        ", . . . . . . ,",
        ", . . h h . . ,",
        ", . h h h h . ,",
        ", h H H h . . ,",
        ", h W H h . . ,",
        ", . h h . . . ,",
        "s , , , , , , s",
    ])
    # The underside overhangs the body in front and behind. A face that points down is drawn at two fifths of its brightness, so this is
    # white to come out as the jaw's shadow under it does, not as a black line.
    paint(img, rng, head["bottom"], ["W W W W W W W W"])

    # The body. The jaw's shadow, a broad chest over a belly, and at the hips the seam where one block of iron sat on the other.
    body = box_faces(16, 16, 8, 12, 4)
    paint(img, rng, body["front"], [
        "m S S S S S S m",
        "s h H W H h . s",
        "s . h H h . . s",
        "s . . . . . , s",
        "s , . . . , , S",
        "S s s s s s s S",
        "s . h . . . , s",
        "s . . . . . , s",
        "s . . . . . , s",
        "s . . . . , , s",
        "s , . . . , s S",
        "d d d d d d d d",
    ])
    # The back likewise: light along one shoulder, and plain down to the seam.
    paint(img, rng, body["back"], [
        "S s s s s s s S",
        "s . h h h . . s",
        "s . . . . . . s",
        "s . . . . . , s",
        "s . . . . . . s",
        "s , . . . . . s",
        "s . . . . , . s",
        "s , . . . . , s",
        "s , . . . . , s",
        "s , , . . . , s",
        "S s , , , , s S",
        "d d d d d d d d",
    ])
    side = [
        "S s s S",
        "s . . s",
        "s . . s",
        "s . . s",
        "s . . s",
        "s . . s",
        "s . . s",
        "s . . s",
        "s . . s",
        "s , , s",
        "S s s S",
        "d d d d",
    ]
    paint(img, rng, body["right"], side)
    paint(img, rng, body["left"], side)
    paint(img, rng, body["top"], [". . . . . . . ."])
    paint(img, rng, body["bottom"], ["m m m m m m m m"])

    # An arm. Lit at the shoulder, a cuff, a fist as big as the arm is thick. Rows are drawn outer edge first, and the back is read the
    # other way round.
    arm = box_faces(40, 16, 4, 12, 4)
    paint(img, rng, arm["front"], [
        "h W H h",
        ". H h .",
        ". h . ,",
        ". . . s",
        ", . , s",
        ", , , s",
        "s , s S",
        "d d d d",
        ". h . ,",
        ". . . s",
        ", . , s",
        "S s s m",
    ])
    paint(img, rng, arm["back"], flip([
        "h H h h",
        ". h . ,",
        ". . . ,",
        ". . . s",
        ", . , s",
        ", , , s",
        "s , s S",
        "d d d d",
        ". . . ,",
        ". . . s",
        ", . , s",
        "S s s m",
    ]))
    paint(img, rng, arm["right"], [
        "h H W h",
        ". h H .",
        ", . h .",
        ", . . .",
        "s , . ,",
        "s , , ,",
        "S s , s",
        "d d d d",
        ", . h .",
        ", . . ,",
        "s , . ,",
        "m S s S",
    ])
    paint(img, rng, arm["left"], [
        "S s s S",
        "S s s S",
        "S s s S",
        "S s s S",
        "S s s S",
        "S s s S",
        "m S S m",
        "d d d d",
        "S s s S",
        "S s s S",
        "S s s S",
        "m S S m",
    ])
    paint(img, rng, arm["top"], ["h H H h", "H W H h", "h H h h", ". h h ."])
    paint(img, rng, arm["bottom"], ["d d d d"])

    # A leg: the band, and under it the foot. The side beside the other leg is in shade. A taller foot takes its rows from the leg above.
    leg = box_faces(0, 16, 4, 12, 4)
    paint(img, rng, leg["front"], [", . . s", ". h , s", ". h , s"] + [". . , s"] * (6 - foot) + [", . , s", "s , s S", "d d d d", ". h , s"] + [". . , s"] * (foot - 2) + ["S s s m"])
    paint(img, rng, leg["back"], flip([", . . s"] + [". . , s"] * (8 - foot) + [", . , s", "s , s S", "d d d d"] + [". . , s"] * (foot - 1) + ["S s s m"]))
    paint(img, rng, leg["right"], ["s . . ,", "s . h .", "s . h ."] + ["s . . ."] * (5 - foot) + ["s . . ,", "s , . ,", "S s , s", "d d d d", "s . h ,"] + ["s . . ,"] * (foot - 2) + ["m S s S"])
    paint(img, rng, leg["left"], ["m S S m"] * (10 - foot) + ["m m m m", "d d d d"] + ["m S S m"] * (foot - 1) + ["m m m m"])
    # Its top is under the body, and shows when it swings out from under: in the body's shade.
    paint(img, rng, leg["top"], ["S S S S"])
    paint(img, rng, leg["bottom"], ["D D D D"])

    out = out or ASSETS / "mob/golem.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)


# Damage shows as cracks: three overlays on the skin's own layout, clear but for the cracks, for a golem below three quarters, a half and a
# quarter of its health. One map a face, a digit a pixel: the stage it first shows at. So each stage holds the one before it and grows from
# its ends, and mending steps back the same way. A crack is drawn in the skin's own shades: a hairline in its shade where it's new, deepening as it
# stays open to a dark grey that was the bands' own when the user approved them, and no further. They're toned for how cracked iron looks near to, not to be made out from far off (the user
# found darker ones too dark, 2026-11-10).
# They run where a casting gives: down from the crown between the eyes, in from a shoulder across the chest to the hip seam, up the back.
# The head and the body carry them, with an arm's and a leg's outer side: a limb's front would show the same crack on both limbs at once,
# since the two share their skin.
CRACKS = {
    ("head", "front"): [
        ". . . . 1 . . .",
        ". . . . 1 . . .",
        ". . . 2 . . . .",
        ". . . 2 . . . .",
        ". . . 3 . . . .",
        ". . . . 3 . . .",
        ". . . . 3 . . .",
        ". . . . . 3 . .",
    ],
    # Over the edge from the front's: the top's last row is its front edge.
    ("head", "top"): [
        ". . . . . . . .",
        ". . . . . . . .",
        ". . . . . . 3 .",
        ". . . . . . 3 .",
        ". . . . . 3 . .",
        ". . . . . 2 . .",
        ". . . . 2 . . .",
        ". . . . 1 . . .",
    ],
    ("head", "back"): [
        ". . . . . . . .",
        ". . . . . . . .",
        ". . . 3 . . . .",
        ". . . 3 . . . .",
        ". . 3 . . . . .",
        ". . 2 . . . . .",
        ". 2 . . . . . .",
        ". 2 . . . . . .",
    ],
    ("head", "right"): [
        ". . . . . . . .",
        ". . . . . . . .",
        ". . . . . . . .",
        ". . . . 3 . . .",
        ". . . . . 3 . .",
        ". . . . . 2 . .",
        ". . . . . . 2 .",
        ". . . . . . 2 .",
    ],
    ("head", "left"): [
        ". . 3 . . . . .",
        ". . 3 . . . . .",
        ". . . 3 . . . .",
        ". . . . . . . .",
        ". . . . . . . .",
        ". . . . . . . .",
        ". . . . . . . .",
        ". . . . . . . .",
    ],
    ("body", "front"): [
        ". . . . . . . .",
        ". 1 . . . . 3 3",
        ". . 1 . . . . 3",
        ". . 1 . . . . .",
        ". . . 1 . . . 3",
        ". . . 2 . . 3 .",
        ". . . . 2 3 . .",
        ". . . . 2 . . .",
        ". . . . . 3 . .",
        ". . . . . 3 . .",
        ". . . . 3 . . .",
        ". . . . 3 . . .",
    ],
    ("body", "back"): [
        ". . . . . . . .",
        ". . . . . . 2 .",
        ". . . . . 1 . .",
        ". . . . . 1 . .",
        ". . . . 1 . . .",
        ". . . . 2 . . .",
        ". . . 2 . 3 . .",
        ". . . 2 . . 3 .",
        ". . 3 . . . 3 .",
        ". . 3 . . . . .",
        ". . . 3 . . . .",
        ". . . 3 . . . .",
    ],
    # A limb's outer side: its columns run back to front.
    ("arm", "right"): [
        ". . . .",
        ". . . .",
        ". . 2 .",
        ". . 2 .",
        ". 2 . .",
        ". 3 . .",
        ". . 3 .",
        ". . . .",
        ". . . .",
        ". . . 3",
        ". . 3 .",
        ". . . .",
    ],
    ("leg", "right"): [
        ". . . .",
        ". . . .",
        ". . . .",
        ". . . 3",
        ". . 3 .",
        ". 2 . .",
        ". 2 . .",
        ". . 2 .",
        ". . 3 .",
        ". . . .",
        ". . . .",
        ". . . .",
    ],
}
# By how many stages a crack has been open: new, older, oldest.
# The first two are the skin's shades; the last is its own, kept as approved now that the bands are lighter.
CRACK_TONES = [TONES["S"], TONES["m"], (104, 110, 123)]
PARTS = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 4, 12, 4), "leg": (0, 16, 4, 12, 4)}


def cracks(foot=None, folder=None):
    foot = foot or FOOT
    folder = folder or ASSETS / "mob"
    for stage in (1, 2, 3):
        img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        for (part, side), rows in CRACKS.items():
            x, y, w, h = box_faces(*PARTS[part])[side]
            rows = [r.replace(" ", "") for r in rows]
            assert len(rows) == h and all(len(r) == w for r in rows), (part, side)
            if part == "leg":
                # It's drawn to end just over the band of a foot 2 high.
                rows = rows[foot - 2:] + rows[:foot - 2]
            for j, row in enumerate(rows):
                for i, ch in enumerate(row):
                    if ch != "." and int(ch) <= stage:
                        img.putpixel((x + i, y + j), CRACK_TONES[stage - int(ch)] + (255,))
        img.save(folder / ("golem_cracks_%d.png" % stage))


# The boundary pole: a short iron rod with a ring of redstone at its top, four of which mark out a golem's work. It began as a taller
# ranging pole with a red head over this ring; the user had the head and the iron under it taken off (2026-11-10). As a block it's one box,
# 2 pixels square and 10 high in the middle of its cell, a torch's size, textured as beta textures any block's box: each face shows the part
# of its tile the box covers, so of the side's tile only the middle two columns of its lower ten rows are seen, a lit one and a shaded one.
# The tiles are drawn right across all the same, so a thicker post would wear them too. Redstone's reds are the core icon's.
# The pole glows while a golem works by it, as a redstone torch does, and is dull when none does (the user's ask, 2026-11-10): two pairs of
# tiles, the plain names the dull one. Only the ring differs. "g" and "G" are the glow, a hot pale red and a bright one, kept red where
# beta's torch goes to yellow and white.
POLE_TONES = dict(TONES, g=(255, 176, 160), G=(255, 92, 62), r=(232, 72, 52), R=(176, 32, 24), e=(124, 20, 18), E=(84, 14, 16), K=(62, 66, 78))
# The rod is the golem's pale iron. It was tried in a dark iron for three rounds, so as to be quiet when the whole pole is drawn full bright
# at night; the user went back to this and has the drawing dim the rod under the ring instead (2026-11-10).
# Top row first, the lit half's letter and then the shaded half's: the red ring, and plain iron down to a foot in shade. The six rows over
# it are left clear, as a torch's tile is over its flame.
POLE_SHAFT = ["h.", ".,", ".,", ".,", ".,", ".,", ",s", "sS"]
# The ring's two rows and the top's tone: dull a step under the item's icon, glowing a step over it.
POLE_RING = {"": (["Re", "eE"], "R"), "_lit": (["gG", "Gr"], "g")}
# The item's icon is the pole standing, seen front on, as beta's torches show in a slot: the dull side's middle two columns where they are
# on the tile, and nothing round them (the user's ask, 2026-11-10; it leaned as a stick does before).


def pole():
    rng = random.Random(1907)
    noise = [[rng.randint(-NOISE, NOISE) for _ in range(16)] for _ in range(16)]
    (ASSETS / "block").mkdir(parents=True, exist_ok=True)
    for name, (ring, crown) in POLE_RING.items():
        rows = ring + POLE_SHAFT
        side = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        top = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for j in range(16):
            for i in range(16):
                n = noise[j][i]
                top.putpixel((i, j), tuple(min(255, c + n) for c in POLE_TONES[crown]) + (255,))
                if j >= 16 - len(rows):
                    row = rows[j - 16 + len(rows)]
                    side.putpixel((i, j), tuple(min(255, c + n) for c in POLE_TONES[row[0 if i < 8 else 1]]) + (255,))
        side.save(ASSETS / ("block/boundary_pole_side%s.png" % name))
        top.save(ASSETS / ("block/boundary_pole_top%s.png" % name))
    # Its bits, when it's hit and when it comes loose (Claude Opus 5.5, 2026-11-10, in these tones): beta cuts each out of a block's bottom
    # tile, a 4-pixel square from anywhere in it, and the top's red or the side's clear rows made red or missing bits. So a tile of its own,
    # the plain side's two rows of ring and then its iron again down the rest: mostly iron, a few bits with the red.
    rows = POLE_RING[""][0] + POLE_SHAFT
    bit_rows = rows[:2] + rows[2:9] * 2
    bits = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for j in range(16):
        for i in range(16):
            n = rng.randint(-NOISE, NOISE)
            bits.putpixel((i, j), tuple(min(255, c + n) for c in POLE_TONES[bit_rows[j][0 if i < 8 else 1]]) + (255,))
    bits.save(ASSETS / "block/boundary_pole_bits.png")
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.paste(Image.open(ASSETS / "block/boundary_pole_side.png").crop((7, 0, 9, 16)), (7, 0))
    icon.save(ASSETS / "item/boundary_pole.png")


# The Golem Core's icon: a ball of iron the size of beta's snowball with a small diamond set in it. It was a picture of its recipe (a diamond
# in gold with redstone corners) until the user asked for something plainer that needn't show what it's made of; of the candidates they chose
# this one, without a seam round it (2026-11-10). The diamond is beta's diamond a little quieter, in a setting of the iron's shade, and its
# shaded corner is kept light at their ask.
CORE_TONES = dict(TONES, c=(150, 236, 220), C=(74, 208, 186), q=(56, 180, 160))
CORE = """
________________
________________
______DDDD______
____DDhHh.DD____
___DhHWHh..,D___
___DhWHh...,D___
__D.Hh.mm..,sD__
__D.h.mcCm.,sD__
__D...mCqm,ssD__
__D,...mm.,sSD__
___D,....,sSD___
___D,,.,,sSSD___
____DDssSSDD____
______DDDD______
________________
________________
"""


def core():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for j, row in enumerate(CORE.split()):
        for i, ch in enumerate(row):
            if ch != "_":
                img.putpixel((i, j), CORE_TONES[ch] + (255,))
    out = ASSETS / "item/golem_core.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)


if __name__ == "__main__":
    skin()
    cracks()
    pole()
    core()
