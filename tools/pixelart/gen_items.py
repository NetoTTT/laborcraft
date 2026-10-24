"""Gera as texturas 16x16 dos itens do LaborCraft e uma folha de preview."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
TEX_DIR = ROOT / "src" / "main" / "resources" / "assets" / "laborcraft" / "textures" / "item"
PREVIEW_DIR = ROOT / "build" / "art-preview"

PAL = {
    ".": (0, 0, 0, 0),
    "O": (0x33, 0x20, 0x0F, 255),
    "S": (0x55, 0x36, 0x1B, 255),
    "D": (0x6E, 0x47, 0x23, 255),
    "B": (0x8A, 0x5C, 0x30, 255),
    "L": (0xA3, 0x70, 0x3C, 255),
    "H": (0xBD, 0x8A, 0x51, 255),
    "p": (0xE9, 0xD7, 0xAE, 255),
    "q": (0xD2, 0xBC, 0x8E, 255),
    "r": (0xB3, 0x9A, 0x6C, 255),
    "i": (0x6B, 0x4F, 0x2E, 255),
    "a": (0xF0, 0xA9, 0x2E, 255),
    "b": (0xBD, 0x7A, 0x17, 255),
    "c": (0xFF, 0xD3, 0x6A, 255),
    "m": (0x3C, 0x43, 0x4C, 255),
    "n": (0x7C, 0x87, 0x94, 255),
    "o": (0xC2, 0xCB, 0xD4, 255),
    "u": (0x3F, 0x6E, 0xA8, 255),
    "w": (0xA8, 0x42, 0x2F, 255),
    "x": (0x7A, 0x2B, 0x20, 255),
    "y": (0xC4, 0x60, 0x4A, 255),
}

SIZE = 16


def blank(ch="."):
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


def frame(g, x0, y0, x1, y1, ch="O"):
    hline(g, x0, x1, y0, ch)
    hline(g, x0, x1, y1, ch)
    vline(g, x0, y0, y1, ch)
    vline(g, x1, y0, y1, ch)


def beam(g, layers, s0, s1):
    for t, ch in layers:
        for s in range(s0, s1 + 1):
            if (t + s) % 2:
                continue
            x = (t + s) // 2
            y = (t - s) // 2
            if 0 <= x < SIZE and 0 <= y < SIZE:
                g[y][x] = ch


def item_contract():
    g = blank()
    rect(g, 2, 2, 13, 13, "p")
    frame(g, 2, 2, 13, 13, "O")
    hline(g, 3, 12, 3, "q")
    vline(g, 12, 3, 12, "q")
    hline(g, 3, 12, 12, "q")

    hline(g, 4, 11, 4, "a")
    g[4][4] = "c"
    g[4][11] = "b"

    hline(g, 4, 9, 6, "i")
    hline(g, 4, 10, 7, "i")
    hline(g, 4, 7, 8, "i")

    g[10][4] = "i"
    g[9][5] = "i"
    g[10][6] = "i"
    g[9][7] = "i"
    g[10][8] = "i"

    g[8][9] = "y"
    g[8][10] = "w"
    g[8][11] = "w"
    g[9][9] = "w"
    g[9][10] = "x"
    g[9][11] = "w"
    g[10][9] = "w"
    g[10][10] = "w"
    g[10][11] = "x"

    g[11][9] = "w"
    g[12][9] = "x"
    g[11][11] = "w"
    g[12][11] = "x"
    return g


def item_house_plan():
    g = blank()
    rect(g, 3, 4, 14, 12, "p")
    frame(g, 3, 4, 14, 12, "O")
    vline(g, 13, 5, 11, "q")

    hline(g, 1, 3, 3, "q")
    hline(g, 1, 3, 13, "q")
    vline(g, 1, 3, 13, "q")
    vline(g, 2, 4, 12, "p")
    vline(g, 3, 4, 12, "q")
    g[3][1] = "O"
    g[13][1] = "O"
    hline(g, 1, 3, 8, "q")

    g[5][8] = "a"
    g[6][7] = "a"
    g[6][9] = "a"
    g[7][6] = "a"
    g[7][10] = "a"
    hline(g, 4, 12, 8, "a")
    vline(g, 4, 9, 10, "a")
    vline(g, 12, 9, 10, "b")
    hline(g, 4, 7, 11, "a")
    hline(g, 9, 12, 11, "b")
    g[9][8] = "a"
    return g


def item_survey_stake():
    g = blank()
    vline(g, 7, 3, 14, "L")
    vline(g, 8, 3, 14, "B")
    vline(g, 9, 4, 13, "D")
    g[2][7] = "n"
    g[2][8] = "n"
    g[2][9] = "m"
    hline(g, 7, 9, 14, "D")

    for y, x0, x1 in [(4, 10, 14), (5, 10, 15), (6, 10, 15), (7, 10, 14), (8, 10, 13)]:
        for x in range(x0, x1 + 1):
            g[y][x] = "a"
    hline(g, 10, 14, 4, "b")
    hline(g, 10, 13, 8, "b")
    g[7][14] = "b"
    g[7][15] = "b"
    g[5][11] = "c"
    g[5][12] = "c"
    g[6][13] = "c"
    return g


def to_image(g):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = PAL[g[y][x]]
    return img


def font(size):
    for name in ("segoeuib.ttf", "arialbd.ttf", "arial.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def make_sheet(items):
    scale = 12
    cell = SIZE * scale
    gap = 26
    pad = 8
    header = 48
    w = cell * 3 + gap * 4
    h = header + cell + gap + 34
    sheet = Image.new("RGB", (w, h), (24, 27, 31))
    d = ImageDraw.Draw(sheet)
    d.text((gap, 12), "LaborCraft - Items 16x16", font=font(22), fill=(240, 169, 46))
    for i, (name, img) in enumerate(items):
        x = gap + i * (cell + gap)
        y = header
        d.rectangle((x - pad, y - pad, x + cell + pad - 1, y + cell + pad - 1), fill=(58, 63, 70))
        d.rectangle((x - pad + 2, y - pad + 2, x + cell + pad - 3, y + cell + pad - 3), outline=(86, 92, 102))
        sheet.paste(img.resize((cell, cell), Image.NEAREST), (x, y), img.resize((cell, cell), Image.NEAREST))
        d.text((x, y + cell + 10), name, font=font(18), fill=(220, 224, 230))
    return sheet


def main():
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

    items = {
        "worker_contract": to_image(item_contract()),
        "house_plan": to_image(item_house_plan()),
        "survey_stake": to_image(item_survey_stake()),
    }
    for name, img in items.items():
        img.save(TEX_DIR / f"{name}.png")

    sheet = make_sheet(
        [("Worker Contract", items["worker_contract"]), ("House Plan", items["house_plan"]), ("Survey Stake", items["survey_stake"])]
    )
    sheet.save(PREVIEW_DIR / "items_sheet.png")

    print("textures:", TEX_DIR)
    for p in sorted(TEX_DIR.glob("*.png")):
        print(" ", p.name, Image.open(p).size)
    print("preview:", PREVIEW_DIR / "items_sheet.png")


if __name__ == "__main__":
    main()
