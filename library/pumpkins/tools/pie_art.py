"""The pumpkin pie's sprite: candidates drawn by hand as grids of letters, one of them the mod's texture.

    python library/pumpkins/tools/pie_art.py              writes the chosen candidate (CHOSEN) as the item's texture
    python library/pumpkins/tools/pie_art.py --sheet DIR  also a sheet of every candidate beside beta's own food, enlarged and
                                                          in slots, and a hotbar, into DIR (for looking at only: it holds
                                                          beta's sprites, read from local/cache/minecraft/b1.7.3-client.jar)

Our own drawing, not later versions' pie (the project's ground rules). Crust in the family of beta's bread and cookie, paler
than the filling; the filling a cooked-down shade of beta's pumpkin block (#e3901d, #d07c14, #a05a0b).
Claude Opus 5.5, 2026-11-11.
"""
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
TEXTURE = ROOT / 'library/pumpkins/src/main/resources/assets/pumpkins/textures/item/pumpkin_pie.png'
CLIENT = ROOT / 'local/cache/minecraft/b1.7.3-client.jar'
CHOSEN = 'l5b'  # the user's pick, 2026-11-11

CRUST = {
    'o': '#4a2c0c',  # outline
    'C': '#efc985',  # crust lit
    'c': '#d6a75f',  # crust
    'k': '#b07a3a',  # crust shade
    'd': '#835520',  # crust dark, the side in shadow
}
FILL = {
    'F': '#e3802c',  # filling lit
    'f': '#c9661f',  # filling
    'g': '#a95219',  # filling shade
    'h': '#8a4013',  # filling dark
}
TIN = {'t': '#353535', 'L': '#d8d8d8', 'l': '#a8a8a8', 'm': '#787878'}  # beta's bucket's greys

CANDIDATES = {
    # A: the whole pie from three-quarters up, as beta draws its cake, but low: a pie is shallow.
    'a': ([
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...ooCcCcCcoo...",
        "..oCcFFFFFfcko..",
        ".oCcFFFffffgcko.",
        ".oCFFffffffggko.",
        ".ocFffffffgggko.",
        ".ocCffggggghCko.",
        ".okCkCkCkCkCkdo.",
        "..ookdkdkdkdoo..",
        "....oooooooo....",
        "................",
        "................",
        "................",
    ], {**CRUST, **FILL}),
    # B: a slice, a wedge seen from above and in front, its crust at the wide end.
    'b': ([
        "................",
        "................",
        "................",
        "...........ooo..",
        ".........ooCcko.",
        ".......ooFFCcko.",
        ".....ooFFFfCcko.",
        "...ooFFFfffCkko.",
        ".ooFFFffffgCkdo.",
        "oFFffffggggCkdo.",
        "oggggghhhhhkkdo.",
        "oCCccccckkkkddo.",
        ".oooooooooooooo.",
        "................",
        "................",
        "................",
    ], {**CRUST, **FILL}),
    # C: A in a tin dish.
    'c': ([
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...ooCcCcCcoo...",
        "..oCcFFFFFfcko..",
        ".oCcFFFffffgcko.",
        ".oCFFffffffggko.",
        ".ocFffffffgggko.",
        ".ocCffggggghCko.",
        ".okCkCkCkCkCkdo.",
        "tLLLllllllllllmt",
        ".tLlllllllllmmt.",
        "..tttttttttttt..",
        "................",
        "................",
    ], {**CRUST, **FILL, **TIN}),
    # D: from nearly above, round as beta's cookie is drawn, the rim crimped all round.
    'd': ([
        "................",
        "................",
        ".....oooooo.....",
        "...ooCcCcCcoo...",
        "..oCcFFFFffcko..",
        ".oCcFFFFfffgkdo.",
        ".oCFFFFffffggko.",
        ".ocFFffffffggko.",
        ".oCFffffffgggdo.",
        ".ocfffffgggghko.",
        ".oCkffgggghhkdo.",
        "..okCkCkCkCkdo..",
        "...odkdkdkdko...",
        ".....oooooo.....",
        "................",
        "................",
    ], {**CRUST, **FILL}),
}

# A second round (Claude Fable 5.2, 2026-11-11), after the user found the first four shaded too much beside beta's food: beta
# fills a shape with two close tones speckled, not light falling across it (its bread and cookie), and outlines in a dark shade
# of the thing's own colour, sometimes only underneath. These differ from each other in view, outline and what marks them as pie.
FLAT = {
    'o': '#4a2a0e',  # outline
    'p': '#7a4a1c',  # the softer outline under a thing, as under beta's cookie
    'C': '#e8bd76', 'c': '#d6a45a', 'k': '#b3803c',  # crust
    'F': '#d9731f', 'f': '#cb661a', 'g': '#b35715',  # filling: deeper than the cookie's orange, which a round pie sits near
    'W': '#f4eee2', 'w': '#d9d0c0',  # cream
}
CANDIDATES.update({
    # E: from above, round as the cookie, a spoon of cream in the middle.
    'e': ([
        "................",
        "................",
        ".....cCcCcc.....",
        "...cCcFFFFcCc...",
        "..cCFFfFFFFFCc..",
        ".cCFFFFFFfFFFCc.",
        ".CFFfFFWWFFFFFc.",
        ".cFFFFWWWwFfFFk.",
        ".CFFFFWWwwFFFFc.",
        ".cFfFFFwwFFFFfk.",
        ".kcFFFFFFFFFgck.",
        "..kcgFFfFFggck..",
        "...pkcccccckp...",
        ".....pppppp.....",
        "................",
        "................",
    ], FLAT),
    # F: the whole pie as beta draws its cake, wide and low, filled flat.
    'f': ([
        "................",
        "................",
        "................",
        ".....cCcCcc.....",
        "...cCFFFFFFCc...",
        ".cCFFFfFFFFFFCc.",
        "cCFFFFFFFFfFFFCc",
        "cCFFfFFFFFFFFFCc",
        "kcCFFFFFFfFFFCck",
        "okcCCFFFFFFCCcko",
        "okkccCCCCCCcckko",
        ".ookkkccccckkoo.",
        "...oookkkkooo...",
        ".....oooooo.....",
        "................",
        "................",
    ], FLAT),
    # G: one slice from above, lying on the slant beta's bread lies on, crust along its round edge.
    'g': ([
        "................",
        "................",
        "..........ooo...",
        "........ooCcCo..",
        "........oFFcCco.",
        ".......oFFfFcCo.",
        "......oFFFFFCco.",
        ".....oFfFFFFcCo.",
        "....oFFFFFfFCco.",
        "...oFFFFFFFFgko.",
        "...oFfFFFFgooo..",
        "..oFFFFgooo.....",
        "..oFFgoo........",
        "..ooo...........",
        "................",
        "................",
    ], FLAT),
    # H: one slice from the side, cream on top.
    'h': ([
        "................",
        "................",
        "................",
        "................",
        "........WW......",
        ".......WWWwooo..",
        "......ooWwFFcCo.",
        "......oFFFFFcCo.",
        "....ooFFfFFFcCo.",
        "..ooFFFFFFfFcCo.",
        ".oFFfFFFFFFFcCo.",
        ".okcccccccccCCo.",
        "..oooooooooooo..",
        "................",
        "................",
        "................",
    ], FLAT),
    # I: the whole pie outlined all round as the bread is, in earthier colours nearer the bread's.
    'i': ([
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...ooCcCcCcoo...",
        ".ooCcFFFFFFcCoo.",
        "oCcFFFfFFFFFFcCo",
        "oCFFFFFFFFfFFFCo",
        "okCFFfFFFFFFFCko",
        "okkCCFFFFFFCCkko",
        ".ookkCCCCCCkkoo.",
        "...ookkkkkkoo...",
        ".....oooooo.....",
        "................",
        "................",
        "................",
    ], {'o': '#3a230c', 'C': '#d2a765', 'c': '#b98a47', 'k': '#946a2e', 'F': '#c46a1e', 'f': '#b55f19'}),
    # K: H with no cream and the near-black outline of beta's porkchop and apple.
    'k': ([
        "................",
        "................",
        "................",
        "................",
        "................",
        "..........oooo..",
        "........ooFFcCo.",
        "......ooFFFFcCo.",
        "....ooFFfFFFcCo.",
        "..ooFFFFFFfFcCo.",
        ".oFFfFFFFFFFcCo.",
        ".okcccccccccCCo.",
        "..oooooooooooo..",
        "................",
        "................",
        "................",
    ], {**FLAT, 'o': '#2a1808'}),
    # J: from above with nothing on it, the rim pinched all the way round.
    'j': ([
        "................",
        "................",
        ".....CkCkCk.....",
        "...CkCFFFFCkC...",
        "..kCFFfFFFFFkC..",
        ".CkFFFFFFfFFFCk.",
        ".kFFfFFFFFFFFFC.",
        ".CFFFFFFFFFfFFk.",
        ".kFFFFFfFFFFFFC.",
        ".CFfFFFFFFFFfFk.",
        ".kCFFFFFFFFFgkC.",
        "..CkgFFfFFggCk..",
        "...pCkCkCkCkp...",
        ".....pppppp.....",
        "................",
        "................",
    ], FLAT),
})
# A third round (Claude Fable 5.2, the same day): the user found the second overcorrected, wanted no spots in the filling, and
# asked for a middle between the two. One lighter patch where the light lands and one darker edge away from it, in whole areas.
SOFT = {
    'o': '#4a2a0e', 'p': '#7a4a1c',
    'C': '#ecc37e', 'c': '#d6a45a', 'k': '#b3803c', 'd': '#8f6228',
    'E': '#e58a34', 'F': '#d9731f', 'g': '#b85a16',
    'W': '#f4eee2', 'w': '#d9d0c0',
}
CANDIDATES.update({
    # L: the whole pie at the cake's angle, no dark line over the top.
    'l': ([
        "................",
        "................",
        "................",
        ".....cCCCCc.....",
        "...cCEEEEFFCc...",
        ".cCEEEEFFFFFFCc.",
        "cCEEEFFFFFFFFFCc",
        "cCEFFFFFFFFFFgCc",
        "kcCFFFFFFFFFggCk",
        "okcCCFFFFgggCCko",
        "okkccCCCCCCcckko",
        ".ookkkccccckkoo.",
        "...oookkkkooo...",
        ".....oooooo.....",
        "................",
        "................",
    ], SOFT),
    # M: the same pie outlined all round, as the bread is.
    'm': ([
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...ooCCCCCCoo...",
        ".ooCCEEEEFFCCoo.",
        "oCCEEEFFFFFFFCCo",
        "oCEEFFFFFFFFFgCo",
        "okCFFFFFFFFgggco",
        "okkccFFFFggcckko",
        ".ookkccccccdkoo.",
        "...ookkkkddoo...",
        ".....oooooo.....",
        "................",
        "................",
        "................",
    ], SOFT),
    # N: one slice from the side, cream on top.
    'n': ([
        "................",
        "................",
        "................",
        "................",
        "........WW......",
        ".......WWWwooo..",
        "......ooWwEFcCo.",
        "......oEEFFFcCo.",
        "....ooEEFFFFcCo.",
        "..ooEEFFFFFgcCo.",
        ".oEFFFFFggggcko.",
        ".okccccccccckko.",
        "..oooooooooooo..",
        "................",
        "................",
        "................",
    ], SOFT),
    # O: from above, plain.
    'o': ([
        "................",
        "................",
        ".....cCCCCc.....",
        "...cCCEEEFCCc...",
        "..cCEEEEFFFFCc..",
        ".cCEEEFFFFFFFCc.",
        ".CEEFFFFFFFFFFc.",
        ".CEFFFFFFFFFFgc.",
        ".cFFFFFFFFFFggk.",
        ".cFFFFFFFFFgggk.",
        ".kcFFFFFFFgggck.",
        "..kcFFFFggggck..",
        "...pkccccckkp...",
        ".....pppppp.....",
        "................",
        "................",
    ], SOFT),
    # P: O with a spoon of cream.
    'p': ([
        "................",
        "................",
        ".....cCCCCc.....",
        "...cCCEEEFCCc...",
        "..cCEEEEFFFFCc..",
        ".cCEEEFFFFFFFCc.",
        ".CEEFFFWWFFFFFc.",
        ".CEFFFWWWwFFFgc.",
        ".cFFFFWWwwFFggk.",
        ".cFFFFFwwFFgggk.",
        ".kcFFFFFFFgggck.",
        "..kcFFFFggggck..",
        "...pkccccckkp...",
        ".....pppppp.....",
        "................",
        "................",
    ], SOFT),
})
# The user's pick is L, with a slightly darker line over the top (2026-11-11): one more row, so the lit rim keeps its place
# inside the line. L2's line is a step under the crust's shade, L3's a step darker again; M's is the full outline.
L_LINED = [
    "................",
    "................",
    ".....qqqqqq.....",
    "...qqCCCCCCqq...",
    ".qqCCEEEEFFCCqq.",
    "qCCEEEEFFFFFFCCq",
    "qCEEEFFFFFFFFFCq",
    "qCEFFFFFFFFFFgCq",
    "qcCFFFFFFFFFggCq",
    "okcCCFFFFgggCCko",
    "okkccCCCCCCcckko",
    ".ookkkccccckkoo.",
    "...oookkkkooo...",
    ".....oooooo.....",
    "................",
    "................",
]
CANDIDATES.update({
    'l2': (L_LINED, {**SOFT, 'q': '#a5722f'}),
    'l3': (L_LINED, {**SOFT, 'q': '#8a5c22'}),
})
# Then the user asked for M with a larger bottom, and variants of M. M2 has the lit front lip L has and one more row of
# crust wall under it; the rest are M2 changed in one way each.
M_DEEP = [
    "................",
    "................",
    ".....oooooo.....",
    "...ooCCCCCCoo...",
    ".ooCCEEEEFFCCoo.",
    "oCCEEEFFFFFFFCCo",
    "oCEEFFFFFFFFFgCo",
    "okCFFFFFFFFgggCo",
    "okcCCFFFFggCCcko",
    "okkccCCCCCCcckko",
    "okkkkccccccckkdo",
    ".ookkkcccckddoo.",
    "...ookkkkddoo...",
    ".....oooooo.....",
    "................",
    "................",
]


def rows(base, changed):
    return [changed.get(y, row) for y, row in enumerate(base)]


CANDIDATES.update({
    'm2': (M_DEEP, SOFT),
    # M3: a row deeper again.
    'm3': (rows(M_DEEP, {11: "okkkkkcccckkkddo", 12: ".ookkkkkkkdddoo.", 13: "...ookkkdddoo...", 14: ".....oooooo....."}), SOFT),
    # M4: the rim pinched.
    'm4': (rows(M_DEEP, {3: "...ooCcCcCcoo...", 4: ".ooCcEEEEFFcCoo.", 5: "oCcEEEFFFFFFFcCo", 6: "ocEEFFFFFFFFFgco",
                         8: "okcCkFFFFggkCcko", 9: "okkcCkCkCkCkckko"}), SOFT),
    # M5: the line over the top half softer, the full outline only underneath.
    'm5': ([row.replace('o', 'p') if y <= 8 else row for y, row in enumerate(M_DEEP)], SOFT),
    # M6: a spoon of cream.
    'm6': (rows(M_DEEP, {5: "oCCEEEFWWFFFFCCo", 6: "oCEEFFWWWwFFFgCo", 7: "okCFFFFWwwFgggCo"}), SOFT),
    # M7: in a tin.
    'm7': (rows(M_DEEP, {10: "tLLLlllllllllmmt", 11: ".ttLlllllllmmtt.", 12: "...ttllllmmtt...", 13: ".....tttttt....."}),
           {**SOFT, 't': '#5f6464', 'L': '#d8d8d8', 'l': '#a8a8a8', 'm': '#787878'}),
})
# The user's pick (2026-11-11): L2, its outline darkening by steps from the top line to the line underneath, since the pie is
# one material all the way down (the cake's outline changes at once because the icing ends there).
STEPS = {5: ('q', 'r'), 6: ('q', 'r'), 7: ('q', 's'), 8: ('q', 's'), 9: ('o', 'u'), 10: ('o', 'u')}
CANDIDATES['l4'] = ([row.replace(*STEPS[y]) if y in STEPS else row for y, row in enumerate(L_LINED)],
                    {**SOFT, 'q': '#a5722f', 'r': '#916126', 's': '#7c501e', 'u': '#633d16'})
# And M2 a little shorter, a row less in one of two places: M2a off the top face, M2b off the wall.
BLANK = "................"
CANDIDATES['m2a'] = (M_DEEP[:6] + M_DEEP[7:] + [BLANK], SOFT)
CANDIDATES['m2b'] = (M_DEEP[:10] + M_DEEP[11:] + [BLANK], SOFT)
# L4 with the outline losing saturation as it nears the top, by three amounts (the user's ask): each step's colour keeps its
# hue and brightness and takes this share of its saturation, top step first.
def paler(hex_colour, share):
    import colorsys
    h = hex_colour.lstrip('#')
    hue, sat, val = colorsys.rgb_to_hsv(*(int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)))
    return '#%02x%02x%02x' % tuple(round(c * 255) for c in colorsys.hsv_to_rgb(hue, sat * share, val))


L4_GRID, L4_PALETTE = CANDIDATES['l4']
for key, shares in (('l5', (0.75, 0.85, 0.95, 1.0)), ('l6', (0.55, 0.7, 0.85, 1.0)), ('l7', (0.35, 0.55, 0.75, 0.95))):
    CANDIDATES[key] = (L4_GRID, {**L4_PALETTE, **{step: paler(L4_PALETTE[step], share) for step, share in zip('qrsu', shares)}})
# L5 with a less dark shade band at the bottom (the user's ask; which band wasn't said, so each and both): L5A the crust's
# band under the lip, L5B the filling's shaded edge, L5C both. Each is halfway to the tone above it.
L5_PALETTE = CANDIDATES['l5'][1]
CANDIDATES['l5a'] = (L4_GRID, {**L5_PALETTE, 'k': '#c4924b'})
CANDIDATES['l5b'] = (L4_GRID, {**L5_PALETTE, 'g': '#c8661a'})
CANDIDATES['l5c'] = (L4_GRID, {**L5_PALETTE, 'k': '#c4924b', 'g': '#c8661a'})
SHOWN = ['l5', 'l5a', 'l5b', 'l5c']


def draw(grid, palette):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                h = palette[ch].lstrip('#')
                im.putpixel((x, y), (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255))
    return im


def sheet(out):
    jar = zipfile.ZipFile(CLIENT)
    items = Image.open(BytesIO(jar.read('gui/items.png'))).convert('RGBA')
    gui = Image.open(BytesIO(jar.read('gui/gui.png'))).convert('RGBA')
    tile = lambda x, y: items.crop((x * 16, y * 16, x * 16 + 16, y * 16 + 16))
    pies = [(f'candidate {k.upper()}', draw(*CANDIDATES[k])) for k in SHOWN]
    beta = [('bread', tile(9, 2)), ('cake', tile(13, 1)), ('cookie', tile(12, 5)), ('apple', tile(10, 0)), ('cooked porkchop', tile(8, 5)),
            ('mushroom stew', tile(8, 4))]

    big, gap, label = 8, 24, 18
    font = ImageFont.load_default()
    cell_w = 16 * big + gap
    row_h = label + 16 * big + 12 + 48 + gap
    img = Image.new('RGBA', (gap + max(len(pies), len(beta)) * cell_w, gap + 2 * row_h + label), (198, 198, 198, 255))
    d = ImageDraw.Draw(img)

    def slot(icon, x, y, scale):
        # Beta's inventory slot at a GUI scale: 18 across, dark top and left, light bottom and right.
        s = 18 * scale
        d.rectangle([x, y, x + s - 1, y + s - 1], fill=(139, 139, 139))
        d.line([x, y, x + s - 1, y], fill=(55, 55, 55), width=scale)
        d.line([x, y, x, y + s - 1], fill=(55, 55, 55), width=scale)
        d.line([x, y + s - scale, x + s - 1, y + s - scale], fill=(255, 255, 255), width=scale)
        d.line([x + s - scale, y, x + s - scale, y + s - 1], fill=(255, 255, 255), width=scale)
        img.alpha_composite(icon.resize((16 * scale, 16 * scale), Image.NEAREST), (x + scale, y + scale))

    def row(things, y0, title):
        d.text((gap, y0 - label + 4), title, fill=(40, 40, 40), font=font)
        for i, (name, icon) in enumerate(things):
            x = gap + i * cell_w
            d.text((x, y0 + 2), name, fill=(40, 40, 40), font=font)
            img.alpha_composite(icon.resize((16 * big, 16 * big), Image.NEAREST), (x, y0 + label))
            ys = y0 + label + 16 * big + 12
            slot(icon, x, ys, 1)
            slot(icon, x + 26, ys, 2)
            slot(icon, x + 70, ys, 3)

    row(pies, gap + label, 'Pumpkin pie candidates (enlarged; in a slot at GUI scale 1, 2 and 3)')
    row(beta, gap + label + row_h, "Beta's own food, the same way")
    img.save(out / 'pie-candidates.png')

    # The hotbar at GUI scale 3 over grass: bread, cake and cookie, then each candidate.
    scale = 3
    bar = gui.crop((0, 0, 182, 22))
    hb = Image.new('RGBA', (182 * scale + 2 * gap, 22 * scale + 2 * gap), (120, 160, 90, 255))
    hb.alpha_composite(bar.resize((182 * scale, 22 * scale), Image.NEAREST), (gap, gap))
    for i, icon in enumerate([beta[0][1], beta[1][1], beta[2][1]] + [p[1] for p in pies]):
        hb.alpha_composite(icon.resize((16 * scale, 16 * scale), Image.NEAREST), (gap + (3 + i * 20) * scale, gap + 3 * scale))
    selected = gui.crop((0, 22, 24, 46))
    hb.alpha_composite(selected.resize((24 * scale, 24 * scale), Image.NEAREST), (gap + (-1 + 3 * 20) * scale, gap - scale))
    hb.save(out / 'pie-hotbar.png')


def main():
    draw(*CANDIDATES[CHOSEN]).save(TEXTURE)
    if '--sheet' in sys.argv:
        out = Path(sys.argv[sys.argv.index('--sheet') + 1])
        out.mkdir(parents=True, exist_ok=True)
        sheet(out)


if __name__ == '__main__':
    main()
