"""Gera as texturas 16x16 do Workstation (LaborCraft) e um preview isometrico."""

import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
TEX_DIR = ROOT / "src" / "main" / "resources" / "assets" / "laborcraft" / "textures" / "block"
PREVIEW_DIR = ROOT / "build" / "art-preview"

PAL = {
    "O": (0x33, 0x20, 0x0F),
    "S": (0x55, 0x36, 0x1B),
    "D": (0x6E, 0x47, 0x23),
    "B": (0x8A, 0x5C, 0x30),
    "L": (0xA3, 0x70, 0x3C),
    "H": (0xBD, 0x8A, 0x51),
    "p": (0xE9, 0xD7, 0xAE),
    "q": (0xD2, 0xBC, 0x8E),
    "r": (0xAA, 0x8D, 0x5E),
    "a": (0xF0, 0xA9, 0x2E),
    "b": (0xBD, 0x7A, 0x17),
    "c": (0xFF, 0xD3, 0x6A),
    "m": (0x3C, 0x43, 0x4C),
    "n": (0x7C, 0x87, 0x94),
    "o": (0xC2, 0xCB, 0xD4),
    "u": (0x3F, 0x6E, 0xA8),
}

SIZE = 16


def blank(ch="O"):
    return [[ch] * SIZE for _ in range(SIZE)]


def rect(g, x0, y0, x1, y1, ch):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            g[y][x] = ch


def hline(g, x0, x1, y, ch):
    for x in range(x0, x1 + 1):
        g[y][x] = ch


def vline(g, x, y0, y1, ch):
    for y in range(y0, y1 + 1):
        g[y][x] = ch


def wood(seed=0, seams=(5, 10), joints=((10, 1, 4), (4, 6, 9), (12, 11, 14))):
    rng = random.Random(seed)
    g = [["B"] * SIZE for _ in range(SIZE)]
    for y in range(1, SIZE - 1):
        if y in seams or rng.random() > 0.45:
            continue
        x0 = rng.randint(1, 9)
        length = rng.randint(3, 8)
        tone = rng.choice(["L", "L", "D"])
        for x in range(x0, min(SIZE - 1, x0 + length)):
            g[y][x] = tone
        if tone == "L" and x0 > 1:
            g[y][x0 - 1] = "H"
    hline(g, 0, SIZE - 1, 0, "O")
    hline(g, 0, SIZE - 1, SIZE - 1, "O")
    vline(g, 0, 0, SIZE - 1, "O")
    vline(g, SIZE - 1, 0, SIZE - 1, "O")
    for y in seams:
        hline(g, 1, SIZE - 2, y, "D")
    for x, y0, y1 in joints:
        vline(g, x, y0, y1, "S")
    hline(g, 1, SIZE - 2, 1, "H")
    return g


def face_top():
    g = blank()
    hline(g, 1, 14, 1, "H")
    vline(g, 1, 2, 14, "L")
    vline(g, 14, 2, 14, "B")
    hline(g, 1, 14, 13, "B")
    hline(g, 1, 14, 14, "D")
    g[13][5] = "D"
    g[13][10] = "L"

    rect(g, 2, 2, 13, 12, "p")
    hline(g, 2, 13, 12, "q")
    vline(g, 13, 2, 12, "q")

    hline(g, 3, 9, 4, "a")
    hline(g, 3, 9, 9, "a")
    vline(g, 3, 4, 9, "a")
    vline(g, 9, 4, 9, "a")

    g[6][6] = "a"
    g[7][5] = "a"
    g[7][6] = "O"
    g[7][7] = "a"
    g[8][6] = "a"

    g[3][11] = "u"
    g[3][12] = "q"

    g[10][3] = "o"
    g[10][4] = "n"
    for x in range(5, 11):
        g[10][x] = "c"
    g[10][11] = "H"
    g[10][12] = "m"
    g[11][3] = "n"
    g[11][4] = "m"
    for x in range(5, 11):
        g[11][x] = "a"
    g[11][11] = "H"
    g[11][12] = "m"
    return g


def face_front():
    g = wood(seed=11, seams=(5,), joints=((10, 1, 4), (12, 10, 13)))
    rect(g, 3, 2, 12, 8, "p")
    hline(g, 3, 12, 8, "q")
    vline(g, 12, 2, 8, "q")
    g[2][3] = "n"
    g[2][12] = "n"
    hline(g, 4, 11, 3, "a")
    hline(g, 4, 8, 5, "r")
    hline(g, 4, 9, 6, "r")
    hline(g, 4, 7, 7, "r")
    rect(g, 10, 6, 11, 7, "a")
    g[7][11] = "b"
    hline(g, 3, 12, 9, "D")

    rect(g, 2, 10, 13, 14, "O")
    rect(g, 3, 11, 12, 13, "L")
    hline(g, 3, 12, 11, "H")
    hline(g, 3, 12, 13, "B")
    hline(g, 6, 9, 12, "a")
    g[11][5] = "L"
    g[12][4] = "B"
    g[12][10] = "D"
    g[13][5] = "S"
    return g


def face_side():
    g = wood(seed=7, seams=(9,), joints=((10, 1, 4), (4, 6, 8), (12, 10, 13)))
    hline(g, 1, 14, 9, "H")
    hline(g, 1, 14, 10, "S")

    for x in range(4, 9):
        g[6][x] = "p"
        g[7][x] = "p"
        g[8][x] = "q"
    g[6][4] = "q"
    g[7][4] = "q"
    g[8][4] = "S"
    g[6][8] = "q"
    g[7][8] = "q"
    g[8][8] = "S"
    for x in (5, 7):
        g[6][x] = "a"
        g[7][x] = "b"
        g[8][x] = "b"

    g[3][12] = "n"
    g[4][12] = "n"
    hline(g, 11, 13, 5, "m")
    g[5][12] = "n"
    g[6][11] = "m"
    g[6][12] = "c"
    g[6][13] = "m"
    g[7][11] = "m"
    g[7][12] = "a"
    g[7][13] = "m"
    hline(g, 11, 13, 8, "m")
    return g


def face_bottom():
    g = wood(seed=3, seams=(5, 10), joints=((10, 1, 4), (4, 6, 9), (12, 11, 14)))
    dark = {"B": "D", "L": "B", "H": "L", "D": "S", "S": "O"}
    for y in range(1, 15):
        for x in range(1, 15):
            g[y][x] = dark.get(g[y][x], g[y][x])
    vline(g, 7, 1, 14, "n")
    vline(g, 8, 1, 14, "m")
    hline(g, 1, 14, 7, "n")
    hline(g, 1, 14, 8, "m")
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        g[y][x] = "o"
    return g


def to_image(g):
    img = Image.new("RGB", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = PAL[g[y][x]]
    return img


def shade(img, factor):
    out = img.copy()
    px = out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            px[x, y] = (int(r * factor), int(g * factor), int(b * factor), a)
    return out


def affine_face(tex, coeffs, w):
    return tex.convert("RGBA").transform(
        (w * 2, w * 2), Image.AFFINE, coeffs, resample=Image.NEAREST
    )


def iso_image(faces, w=128, pad=14):
    s = SIZE
    top = affine_face(
        faces["top"], (s / (2 * w), s / w, -s / 2, -s / (2 * w), s / w, s / 2), w
    )
    right = affine_face(
        faces["right"], (-s / w, 0, 2 * s, s / (2 * w), s / w, -1.5 * s), w
    )
    left = affine_face(
        faces["left"], (-s / w, 0, s, -s / (2 * w), s / w, -0.5 * s), w
    )

    canvas = Image.new("RGBA", (w * 2 + pad * 2, w * 2 + pad * 2), (0, 0, 0, 0))
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.ellipse(
        (pad + w * 0.06, pad + w * 1.80, pad + w * 1.94, pad + w * 2.00),
        fill=(0, 0, 0, 90),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(5))
    canvas.alpha_composite(shadow)
    canvas.alpha_composite(top, (pad, pad))
    canvas.alpha_composite(shade(left, 0.86), (pad, pad))
    canvas.alpha_composite(shade(right, 0.68), (pad, pad))
    return canvas


def label_font(size):
    for name in ("segoeuib.ttf", "arialbd.ttf", "arial.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def make_sheet(faces):
    scale = 10
    cell = SIZE * scale
    gap = 28
    header = 46
    w = cell * 4 + gap * 5
    h = header + cell + gap * 2
    sheet = Image.new("RGB", (w, h), (24, 27, 31))
    d = ImageDraw.Draw(sheet)
    font = label_font(18)
    title = label_font(22)
    d.text((gap, 12), "LaborCraft - Workstation 16x16", font=title, fill=(240, 169, 46))
    for i, (name, key) in enumerate(
        [("TOP", "top"), ("FRONT / south", "front"), ("SIDE", "side"), ("BOTTOM", "bottom")]
    ):
        x = gap + i * (cell + gap)
        y = header
        d.rectangle((x - 2, y - 2, x + cell + 1, y + cell + 1), fill=(44, 48, 54))
        sheet.paste(faces[key].resize((cell, cell), Image.NEAREST), (x, y))
        d.text((x, y + cell + 6), name, font=font, fill=(220, 224, 230))
    return sheet


def main():
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

    faces = {
        "top": to_image(face_top()),
        "front": to_image(face_front()),
        "side": to_image(face_side()),
        "bottom": to_image(face_bottom()),
    }

    for name, img in faces.items():
        img.save(TEX_DIR / f"workstation_{name}.png")

    sheet = make_sheet(faces)
    sheet.save(PREVIEW_DIR / "workstation_faces.png")

    iso = iso_image(
        {"top": faces["top"], "right": faces["side"], "left": faces["front"]}
    )
    iso.save(PREVIEW_DIR / "workstation_iso.png")

    print("textures:", TEX_DIR)
    for p in sorted(TEX_DIR.glob("workstation_*.png")):
        print(" ", p.name, Image.open(p).size)
    print("preview:", PREVIEW_DIR / "workstation_iso.png")
    print("preview:", PREVIEW_DIR / "workstation_faces.png")


if __name__ == "__main__":
    main()
