"""Generates the 256x256 material atlas used by every 3D model and the player cyberware layer.
4x4 grid of 64x64 tiles; tile index = material id (see MATERIALS in gen_models.py)."""
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

T = 64
rng = np.random.default_rng(2077)

def noise(scale=1.0, blur=0):
    n = rng.normal(0, 1, (T, T))
    if blur:
        img = Image.fromarray(((n - n.min()) / (np.ptp(n) + 1e-9) * 255).astype('uint8')).filter(ImageFilter.GaussianBlur(blur))
        n = np.asarray(img, float) / 255 - 0.5
    return n * scale

def to_img(rgb):
    return Image.fromarray(np.clip(rgb, 0, 255).astype('uint8'), 'RGB')

def base(color, n=8, blur=0):
    a = np.zeros((T, T, 3)) + color
    a += noise(n, blur)[..., None]
    return a

def panel_lines(img, step=32, dark=(20, 22, 28), light=(200, 206, 216), rivets=True):
    d = ImageDraw.Draw(img)
    for p in range(0, T, step):
        d.line([(p, 0), (p, T)], fill=dark); d.line([(p + 1, 0), (p + 1, T)], fill=light)
        d.line([(0, p), (T, p)], fill=dark); d.line([(0, p + 1), (T, p + 1)], fill=light)
    if rivets:
        for x in range(0, T, step):
            for y in range(0, T, step):
                for (ox, oy) in ((5, 5), (step - 6, 5), (5, step - 6), (step - 6, step - 6)):
                    d.ellipse([x + ox - 1.5, y + oy - 1.5, x + ox + 1.5, y + oy + 1.5], fill=light, outline=dark)
    return img

def brushed(color):
    a = np.zeros((T, T, 3)) + color
    streak = rng.normal(0, 1, (1, T)).repeat(T, 0) * 6 + rng.normal(0, 1, (T, T)) * 3
    a += streak[..., None]
    grad = np.linspace(14, -14, T)[:, None, None]
    return a + grad

def glow(color):
    y, x = np.mgrid[0:T, 0:T] / (T - 1) * 2 - 1
    d = np.maximum(abs(x), abs(y))
    core = np.clip(1.15 - d ** 4, 0, 1)
    c = np.array(color, float)
    white = np.array([255, 255, 255], float)
    a = c * (0.55 + 0.45 * core[..., None]) + (white - c) * (core[..., None] ** 6) * 0.65
    scan = (np.arange(T) % 4 == 0)[:, None, None] * -18
    return a + scan

tiles = []
# 0 brushed steel
tiles.append(panel_lines(to_img(brushed((150, 156, 168)))))
# 1 dark steel panel
img = to_img(base((52, 56, 66), 5) + np.linspace(10, -10, T)[:, None, None])
d = ImageDraw.Draw(img)
for y in range(8, T, 16):
    d.rectangle([6, y, T - 7, y + 3], fill=(30, 32, 40)); d.line([(6, y + 4), (T - 7, y + 4)], fill=(80, 86, 98))
tiles.append(panel_lines(img, 64, rivets=True))
# 2 chrome
y = np.linspace(0, 1, T)[:, None]
band = 150 + 95 * np.sin(y * 9.5) ** 2 + 20 * np.cos(y * 23)
a = np.repeat(band, T, 1)[..., None] * np.array([0.95, 0.98, 1.05]) + noise(4)[..., None]
tiles.append(to_img(a))
# 3 carbon fibre
a = np.zeros((T, T, 3)) + 28
yy, xx = np.mgrid[0:T, 0:T]
weave = ((xx // 4 + yy // 4) % 2) * 2 - 1
shade = np.where(weave > 0, (xx % 4) * 6, (yy % 4) * 6)
a += (shade + weave * 7)[..., None] + noise(3)[..., None]
tiles.append(to_img(a))
# 4 black rubber / leather
a = base((24, 24, 28), 6, 1)
tiles.append(to_img(a).filter(ImageFilter.SMOOTH))
# 5 hazard paint (worn)
a = np.zeros((T, T, 3))
stripe = ((xx + yy) // 12) % 2
a[stripe == 0] = (236, 214, 20); a[stripe == 1] = (24, 24, 28)
wear = noise(1, 2) > 0.18
a[wear] = (110, 112, 118)
tiles.append(to_img(a + noise(6)[..., None]))
# 6-9 glows: cyan, pink, green, red
for col in ((0, 220, 255), (255, 40, 110), (50, 255, 130), (255, 30, 40)):
    tiles.append(to_img(glow(col)))
# 10 synthetic muscle
a = np.zeros((T, T, 3)) + (150, 30, 40)
fib = np.sin(xx * 1.4 + noise(3, 2) * 4) * 22
a += fib[..., None] * np.array([1, 0.4, 0.4]) + noise(8)[..., None]
img = to_img(a); d = ImageDraw.Draw(img)
for x in range(4, T, 9):
    d.line([(x, 0), (x + 2, T)], fill=(90, 14, 22))
tiles.append(img)
# 11 lens glass
yy2, xx2 = np.mgrid[0:T, 0:T] / (T - 1) * 2 - 1
r = np.sqrt(xx2 ** 2 + yy2 ** 2)
a = np.zeros((T, T, 3)) + (10, 12, 22)
a += (np.clip(1 - r, 0, 1) * 60)[..., None] * np.array([0.6, 0.8, 1.3])
hl = np.exp(-(((xx2 + 0.35) ** 2 + (yy2 + 0.35) ** 2) / 0.03))
a += hl[..., None] * 230
tiles.append(to_img(a))
# 12 gold contacts
a = brushed((200, 158, 40)) * np.array([1, 0.95, 0.6])
img = to_img(a); d = ImageDraw.Draw(img)
for x in range(0, T, 8):
    d.line([(x, 0), (x, T)], fill=(120, 86, 10))
tiles.append(img)
# 13 PCB
a = base((18, 82, 52), 5)
img = to_img(a); d = ImageDraw.Draw(img)
for i in range(22):
    x, y = rng.integers(0, T, 2); L = rng.integers(8, 30)
    if rng.random() < 0.5: d.line([(x, y), (x + L, y)], fill=(70, 200, 120)); d.line([(x + L, y), (x + L + 6, y + 6)], fill=(70, 200, 120))
    else: d.line([(x, y), (x, y + L)], fill=(70, 200, 120))
    d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=(212, 170, 50))
tiles.append(img)
# 14 white ceramic / plastic
a = base((222, 226, 232), 3) + np.linspace(12, -16, T)[:, None, None]
img = to_img(a); d = ImageDraw.Draw(img)
d.line([(0, 40), (T, 40)], fill=(170, 176, 186))
tiles.append(img)
# 15 red plastic
a = base((176, 26, 36), 4) + np.linspace(16, -18, T)[:, None, None]
img = to_img(a); d = ImageDraw.Draw(img)
for y in (14, 46): d.line([(0, y), (T, y)], fill=(110, 10, 20)); d.line([(0, y + 1), (T, y + 1)], fill=(220, 70, 80))
tiles.append(img)

def build(path):
    atlas = Image.new('RGBA', (T * 4, T * 4))
    for i, t in enumerate(tiles):
        atlas.paste(t.convert('RGBA'), ((i % 4) * T, (i // 4) * T))
    atlas.save(path)

if __name__ == '__main__':
    build('src/main/resources/assets/edgerunners/textures/item/cyberware.png')
    print('atlas ok')


# ---------------------------------------------------------------- chrome atlas (external chrome sets)
def hexes(img, size=8, line=(14, 14, 18), hi=(70, 72, 84)):
    d = ImageDraw.Draw(img)
    h = size * math.sqrt(3) / 2
    for row in range(-1, int(T / h) + 2):
        for col in range(-1, int(T / (size * 1.5)) + 2):
            cx = col * size * 1.5; cy = row * h * 2 + (h if col % 2 else 0)
            pts = [(cx + size * math.cos(math.radians(a)), cy + size * math.sin(math.radians(a))) for a in range(0, 360, 60)]
            d.polygon(pts, outline=line)
            d.line(pts[4:6], fill=hi)
    return img

def anodized(color):
    a = brushed(color) * 0.9
    a += np.linspace(18, -22, T)[:, None, None]
    img = to_img(a)
    d = ImageDraw.Draw(img)
    d.line([(0, 20), (T, 20)], fill=tuple(int(c * 0.45) for c in color)); d.line([(0, 21), (T, 21)], fill=tuple(min(255, int(c * 1.4)) for c in color))
    return img

import math
chrome_tiles = []
# 0 matte black armour with hex pattern
chrome_tiles.append(hexes(to_img(base((34, 35, 42), 4) + np.linspace(14, -10, T)[:, None, None])))
# 1 gunmetal panels
chrome_tiles.append(panel_lines(to_img(brushed((88, 92, 102))), 32, rivets=True))
# 2 anodised teal, 3 anodised purple
chrome_tiles.append(anodized((40, 170, 160)))
chrome_tiles.append(anodized((120, 60, 190)))
# 4 white composite
a = base((226, 228, 232), 3) + np.linspace(16, -20, T)[:, None, None]
img = to_img(a); d = ImageDraw.Draw(img)
for y in (16, 48): d.line([(0, y), (T, y)], fill=(160, 164, 172))
d.line([(32, 0), (32, 16)], fill=(160, 164, 172))
chrome_tiles.append(img)
# 5 copper
chrome_tiles.append(to_img(brushed((184, 104, 62))))
# 6 glow orange, 7 glow purple, 8 glow ice-blue
for col in ((255, 120, 20), (170, 60, 255), (150, 220, 255)):
    chrome_tiles.append(to_img(glow(col)))
# 9 heat vent: dark slats with hot glow between them
a = np.zeros((T, T, 3)) + (20, 18, 18)
yy, xx = np.mgrid[0:T, 0:T]
slot = (yy % 8) < 3
heat = np.clip(1 - np.abs(xx - T / 2) / (T / 2), 0, 1) ** 0.6
a[slot] = (np.array([255, 110, 20]) * (0.55 + 0.45 * heat[slot][:, None]))
a[~slot] += np.linspace(30, 0, T)[:, None, None].repeat(T, 1)[~slot]
chrome_tiles.append(to_img(a))
# 10 braided cable
a = np.zeros((T, T, 3)) + 26
braid = (np.sin((xx + yy) * 0.8) * np.sin((xx - yy) * 0.8))
a += (braid * 26)[..., None] + np.array([6, 4, 14])
chrome_tiles.append(to_img(a + noise(4)[..., None]))
# 11 red armour (worn paint)
a = base((168, 22, 30), 5) + np.linspace(20, -24, T)[:, None, None]
wear = noise(1, 2) > 0.22
a[wear] = (96, 98, 106)
img = to_img(a + noise(3)[..., None]); panel_lines(img, 64, rivets=True)
chrome_tiles.append(img)
# 12 exposed servo / mechanical detail
img = to_img(base((60, 62, 70), 6)); d = ImageDraw.Draw(img)
for cx, cy, r in ((16, 16, 11), (46, 18, 8), (22, 46, 9), (48, 48, 12)):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(120, 124, 134), outline=(20, 20, 26))
    d.ellipse([cx - r / 2.5, cy - r / 2.5, cx + r / 2.5, cy + r / 2.5], fill=(40, 42, 50))
    for k in range(8):
        ang = k * math.pi / 4
        d.line([(cx + math.cos(ang) * r * 0.5, cy + math.sin(ang) * r * 0.5), (cx + math.cos(ang) * r, cy + math.sin(ang) * r)], fill=(80, 82, 92))
d.line([(0, 32), (T, 32)], fill=(200, 160, 40), width=2)
chrome_tiles.append(img)
# 13 dark chrome
band = 70 + 70 * np.sin(np.linspace(0, 1, T)[:, None] * 8) ** 2
chrome_tiles.append(to_img(np.repeat(band, T, 1)[..., None] * np.array([0.9, 0.95, 1.08]) + noise(3)[..., None]))
# 14 neon circuit (emissive)
img = to_img(base((16, 10, 30), 3)); d = ImageDraw.Draw(img)
for i in range(16):
    x, y = (int(v) for v in rng.integers(2, T - 2, 2)); L = int(rng.integers(8, 26))
    col = (210, 120, 255) if i % 2 else (120, 230, 255)
    if i % 3: d.line([(x, y), (min(T - 1, x + L), y), (min(T - 1, x + L + 5), min(T - 1, y + 5))], fill=col, width=2)
    else: d.line([(x, y), (x, min(T - 1, y + L))], fill=col, width=2)
    d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(255, 255, 255))
chrome_tiles.append(img)
# 15 tinted visor glass
yy2, xx2 = np.mgrid[0:T, 0:T] / (T - 1)
a = np.zeros((T, T, 3)) + (60, 10, 40)
a += ((1 - yy2) * 90)[..., None] * np.array([1.0, 0.3, 0.6])
a += (np.exp(-((xx2 - 0.3) ** 2 + (yy2 - 0.25) ** 2) / 0.01) * 200)[..., None]
chrome_tiles.append(to_img(a))


def build_chrome(path):
    atlas = Image.new('RGBA', (T * 4, T * 4))
    for i, t in enumerate(chrome_tiles):
        atlas.paste(t.convert('RGBA'), ((i % 4) * T, (i // 4) * T))
    atlas.save(path)
