"""Draws the detail screen's tab icons (16 by 16) in the style of vanilla item sprites.

Each icon is built from shapes. A shape is filled with a base colour, lit on the edges that face the
top-left and shaded on the edges that face the bottom-right, as vanilla items are; the whole icon then
gets a one-pixel outline in a dark tone of its own colours.

    python tools/tab-icons/draw.py            # writes the textures
    python tools/tab-icons/draw.py --preview  # also writes preview.png beside this script (8x, on tabs)
"""
import math
import os
import sys

from PIL import Image

SIZE = 16
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src', 'client', 'resources', 'assets', 'shannonuimod', 'textures')


def rgb(value):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255, 255)


class Icon:
    def __init__(self):
        self.px = {}
        self.solid = set()

    def put(self, x, y, color, solid=True):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            self.px[(x, y)] = rgb(color)
            if solid:
                self.solid.add((x, y))

    def shape(self, cells, base, light=None, dark=None):
        """Fills `cells`, lighting cells whose top or left neighbour is outside the shape, shading bottom or right."""
        cells = set(cells)
        for (x, y) in cells:
            color = base
            if dark is not None and ((x + 1, y) not in cells or (x, y + 1) not in cells):
                color = dark
            if light is not None and ((x - 1, y) not in cells or (x, y - 1) not in cells):
                color = light
            self.put(x, y, color)
        return cells

    def outline(self, color, cells=None):
        """A dark edge around `cells` (default: everything drawn so far), on the outside, 4-connected."""
        cells = set(cells) if cells is not None else set(self.solid)
        for (x, y) in list(cells):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, y + dy)
                if n not in cells and n not in self.solid:
                    self.put(n[0], n[1], color)

    def image(self):
        img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
        for (x, y), color in self.px.items():
            img.putpixel((x, y), color)
        return img


def rect(x0, y0, x1, y1):
    return {(x, y) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)}


def disc(cx, cy, r):
    return {(x, y) for x in range(SIZE) for y in range(SIZE) if (x - cx) ** 2 + (y - cy) ** 2 <= r * r}


def ellipse(cx, cy, rx, ry):
    return {(x, y) for x in range(SIZE) for y in range(SIZE) if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1}


# ===== The icons =====

def tasks():
    """A clipboard with a checklist: the plan and what is done."""
    icon = Icon()
    board = icon.shape(rect(2, 2, 13, 14), 0x9C6B30, light=0xB98A4C, dark=0x6E4A1F)
    paper = rect(4, 4, 11, 13)
    icon.shape(paper, 0xF1EDE0, dark=0xCFC6AE)
    for top in (5, 9):
        for (dx, dy) in ((3, 0), (0, 1), (2, 1), (1, 2)):
            icon.put(4 + dx, top + dy, 0x2FA82F)
        for x in range(8, 11):
            icon.put(x, top + 1, 0x8C8577)
    icon.put(5, 12, 0x8C8577)
    icon.put(6, 12, 0x8C8577)
    icon.put(5, 13, 0xCFC6AE)
    icon.outline(0x3A2410)
    # The clip, over the board's top edge.
    clip = icon.shape(rect(6, 1, 9, 3), 0xC6C6C6, light=0xEFEFEF, dark=0x8A8A8A)
    icon.put(7, 2, 0x4A4A4A)
    icon.put(8, 2, 0x4A4A4A)
    icon.outline(0x2E2E2E, clip)
    return icon


def inventory():
    """A chest, as the player's own inventory sits in one."""
    icon = Icon()
    lid = icon.shape(rect(1, 3, 14, 7), 0xA46A27, light=0xC48A40, dark=0x7A4C18)
    body = icon.shape(rect(1, 8, 14, 14), 0x965F21, light=0xB27A36, dark=0x6A4314)
    for x in range(1, 15):
        icon.put(x, 8, 0x4F3210)
    for x in range(2, 14):
        icon.put(x, 11, 0x7E5019)
    icon.outline(0x2A1A08)
    latch = icon.shape(rect(7, 7, 8, 9), 0xD8D8D8, light=0xF4F4F4, dark=0x9A9A9A)
    icon.outline(0x2E2E2E, latch)
    return icon


def request():
    """A book and quill: writing down what to ask for."""
    icon = Icon()
    book = icon.shape(rect(1, 4, 10, 15), 0x7A3E1C, light=0x9A5530, dark=0x55260E)
    for y in range(5, 15):
        icon.put(10, y, 0xE8E2CF)
    icon.put(10, 15, 0xBFB59B)
    for y in range(4, 16):
        icon.put(1, y, 0x4A200A)
    label = icon.shape(rect(3, 7, 7, 9), 0xD9B25A, light=0xF2D27E, dark=0xA8832E)
    icon.outline(0x2A1206)
    # The quill, from the nib at the bottom left up to the top right, its vane widening towards the top.
    shaft = [(5 + i, 12 - i) for i in range(10)]
    vane = set()
    for i, (x, y) in enumerate(shaft):
        if i >= 3:
            vane |= {(x, y - 1), (x - 1, y - 1)}
        if 5 <= i <= 8:
            vane |= {(x - 1, y - 2), (x + 1, y)}
    vane -= set(shaft)
    vane = {(x, y) for (x, y) in vane if 0 <= x < SIZE and 0 <= y < SIZE}
    icon.shape(vane, 0xF4F4F4, light=0xFFFFFF, dark=0xC8C8C8)
    for (x, y) in shaft[2:]:
        icon.put(x, y, 0xB0B0B0)
    icon.outline(0x3A3A3A, vane | set(shaft[2:]))
    icon.put(5, 12, 0x1E1E1E)
    icon.put(6, 11, 0x4A4A4A)
    return icon


def journey():
    """An eye of ender: the way to the End."""
    icon = Icon()
    eye = icon.shape(disc(7.5, 7.5, 6.6), 0x2F9C7A, light=0x63D1AA, dark=0x1C6A52)
    iris = icon.shape(ellipse(7.5, 7.5, 2.9, 4.4), 0xB9DB5A, light=0xDDF08A, dark=0x8DB236)
    # A cat's slit, narrow at the ends.
    for y in range(6, 10):
        icon.put(7, y, 0x10201A)
        icon.put(8, y, 0x10201A)
    icon.put(7, 5, 0x2E4A22)
    icon.put(8, 10, 0x2E4A22)
    icon.put(4, 4, 0xC8F5E2)
    icon.put(5, 3, 0xC8F5E2)
    icon.outline(0x0D3A2C)
    return icon


def chat():
    """A speech bubble with three dots."""
    icon = Icon()
    bubble = disc(7.5, 6.5, 6.6) & rect(0, 1, 15, 11)
    bubble |= rect(2, 2, 13, 11) - {(2, 2), (13, 2), (2, 11), (13, 11)}
    tail = {(3, 12), (4, 12), (3, 13)}
    icon.shape(bubble | tail, 0xF2F2F2, light=0xFFFFFF, dark=0xC4C4C4)
    for x0 in (4, 7, 10):
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            icon.put(x0 + dx, 6 + dy, 0x4A4A4A)
    icon.outline(0x242424)
    return icon


def advancements():
    """A gold medal on a ribbon: what she has achieved."""
    icon = Icon()
    left = {(x, y) for y in range(9, 16) for x in (5, 6, 7) if not (y == 15 and x == 6)}
    right = {(x, y) for y in range(9, 16) for x in (8, 9, 10) if not (y == 15 and x == 9)}
    icon.shape(left, 0xC23434, light=0xE05454, dark=0x8E1E1E)
    icon.shape(right, 0x2F5FC0, light=0x4F7FE0, dark=0x1E3E8E)
    icon.outline(0x2A0A0A)
    medal = disc(7.5, 6.5, 5.6)
    icon.shape(medal, 0xF0C232, light=0xFFE88A, dark=0xB8861A)
    # An inner rim, as on a coin, and a bright face inside it.
    inner = disc(7.5, 6.5, 3.4)
    for (x, y) in medal - inner:
        if any((x + dx, y + dy) in inner for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            icon.put(x, y, 0xB8861A)
    icon.shape(inner, 0xF7D24A, light=0xFFF0A8, dark=0xE0AE2A)
    icon.outline(0x5A3A06, medal)
    return icon


def settings():
    """An iron gear."""
    icon = Icon()
    body = disc(7.5, 7.5, 5.0)
    teeth = rect(6, 1, 9, 2) | rect(6, 13, 9, 14) | rect(1, 6, 2, 9) | rect(13, 6, 14, 9)
    for (cx, cy) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        teeth |= rect(cx - 1, cy - 1, cx + 1, cy + 1) - {(cx - 1 if cx < 8 else cx + 1, cy - 1 if cy < 8 else cy + 1)}
    hole = disc(7.5, 7.5, 1.9)
    gear = (body | teeth) - hole
    icon.shape(gear, 0xB4B4B4, light=0xE6E6E6, dark=0x7A7A7A)
    icon.outline(0x2E2E2E)
    # The hole, dark, its top-left rim in shadow as a hole's is.
    for (x, y) in hole:
        rim = (x - 1, y) not in hole or (x, y - 1) not in hole
        icon.put(x, y, 0x242424 if rim else 0x3A3A3A, solid=False)
    return icon


ICONS = {
    'tasktree': tasks,
    'inventory': inventory,
    'request': request,
    'journey': journey,
    'chat': chat,
    'advancements': advancements,
    'settings': settings,
}


def preview(images, path):
    """The icons on vanilla-coloured tabs, 8 times enlarged, the first as the selected tab."""
    scale, pad = 8, 6
    cell = (SIZE + pad * 2) * scale
    sheet = Image.new('RGBA', (cell * len(images), cell + 2 * scale), (24, 24, 24, 255))
    for i, img in enumerate(images):
        tab = Image.new('RGBA', (SIZE + pad * 2, SIZE + pad * 2), (198, 198, 198, 255) if i == 0 else (143, 143, 143, 255))
        tab.alpha_composite(img, (pad, pad))
        sheet.alpha_composite(tab.resize((cell, cell), Image.NEAREST), (i * cell, 0))
        small = Image.new('RGBA', (SIZE + pad * 2, SIZE + pad * 2), (198, 198, 198, 255))
        small.alpha_composite(img, (pad, pad))
    sheet.save(path)


def main():
    images = []
    for name, draw in ICONS.items():
        img = draw().image()
        img.save(os.path.join(OUT, name + '.png'))
        images.append(img)
    if '--preview' in sys.argv:
        preview(images, os.path.join(HERE, 'preview.png'))


if __name__ == '__main__':
    main()
