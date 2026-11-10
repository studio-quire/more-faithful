"""The site's cards for the Faithful Library and More Faithful: seals over a frame, pixel-sharp, drawn plain (--plain). More
Faithful's has its own seal alone (148,148 --seals morefaithful --plain) until it has a picture of its own, which
tools/cover_card.py makes as the editions' are; the library's has the two packs' seals side by side. Without --plain each
seal sits in a smooth glow of its own hue, as the library's card had its editions' seals through 0.4.0; nothing uses it now
(More Faithful's had a gold one for an hour; the user: no glow, 2026-11-10). A seal is a name in prism/release/icons/final/
or the path of a PNG.

    python tools/library_card.py <frame.png> <out.png> [x1,y1,x2,y2] [--seals a,b] [--plain]   at those centres, or CENTRES
    python tools/library_card.py colours [seal ...]                                             the glows' colours, for the link preview's CSS

Claude Opus 5.5, 2026-11-05.
"""
import colorsys
import sys
from pathlib import Path

from PIL import Image, ImageFilter

FINAL = Path(__file__).resolve().parent.parent / "prism/release/icons/final"
W, H = 1120, 700
EDITIONS = ["stillfaithful", "oldfaithful"]
# Side by side in the sky, leaving the bay and the cliffs to be seen.
CENTRES = [(148, 148), (398, 148)]
SEAL = 208  # 6.5 times the 32 grid, drawn from the 512
GLOW = [(14, 1.6), (52, 1.4)]  # blur radius and strength: a close bright edge inside a wide falloff
# Violet sits close to the card's periwinkle sky, so its glow is deeper and stronger than green's to show as much. A seal not
# named here gets green's.
LIGHTNESS = {"stillfaithful": 0.72, "oldfaithful": 0.8}
BOOST = {"stillfaithful": 1.3, "oldfaithful": 1.0}


def seal(edition):
    path = Path(edition) if edition.lower().endswith(".png") else FINAL / f"{edition}-512.png"
    return Image.open(path).convert("RGBA")


def glow_colour(edition):
    """The seal's own hue, lightened: the mean of its saturated pixels."""
    pixels = [(r, g, b) for r, g, b, a in seal(edition).getdata() if a > 200 and max(r, g, b) - min(r, g, b) > 50]
    mean = [sum(p[i] for p in pixels) / len(pixels) / 255 for i in range(3)]
    h, _, _ = colorsys.rgb_to_hls(*mean)
    return tuple(round(c * 255) for c in colorsys.hls_to_rgb(h, LIGHTNESS.get(edition, 0.8), 0.85))


def add_seal(card, edition, centre, glow=True):
    # By nearest neighbour, like the other cards' seals: 16 source pixels to a grid cell.
    big = seal(edition).resize((SEAL, SEAL), Image.NEAREST)
    left, top = centre[0] - SEAL // 2, centre[1] - SEAL // 2
    if not glow:
        card.paste(big, (left, top), big)
        return card
    mask = Image.new("L", (W, H), 0)
    mask.paste(big.getchannel("A"), (left, top))
    colour = Image.new("RGB", (W, H), glow_colour(edition))
    for radius, strength in reversed(GLOW):
        alpha = mask.filter(ImageFilter.GaussianBlur(radius)).point(lambda v: min(255, int(v * strength * BOOST.get(edition, 1.0))))
        card = Image.composite(colour, card, alpha)
    card.paste(big, (left, top), big)
    return card


def main():
    args = sys.argv[1:]
    if args[0] == "colours":
        for e in args[1:] or EDITIONS:
            print(e, glow_colour(e))
        return
    editions = EDITIONS
    plain = "--plain" in args
    if plain:
        args.remove("--plain")
    if "--seals" in args:
        i = args.index("--seals")
        editions = args[i + 1].split(",")
        del args[i:i + 2]
    centres = CENTRES
    if len(args) > 2:
        xy = list(map(int, args[2].split(",")))
        centres = list(zip(xy[0::2], xy[1::2]))
    card = Image.open(args[0]).convert("RGB").resize((W, H), Image.LANCZOS)
    for edition, centre in zip(editions, centres):
        card = add_seal(card, edition, centre, glow=not plain)
    card.save(args[1], optimize=True)


if __name__ == "__main__":
    main()
