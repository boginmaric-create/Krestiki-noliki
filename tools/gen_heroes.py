"""Hero personas: full-body character models drawn over the player, plus a 256px texture atlas per hero.

Original archetype characters inspired by the anime's roles (not copies of its characters).
Model format = chrome sets (part space: Y down, face -Z). Atlas key "h" is the hero's own texture.
Run from the repository root: python3 tools/gen_heroes.py"""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
import gen_models as gm
import gen_materials as mt
from gen_chrome import cube, group, rot_anim, move_anim, mirror_x

ROOT = 'src/main/resources/assets/edgerunners'
T = 64
# hero atlas tiles
SKIN, FACE, HAIR, HAIR2, TOP, TOP2, INNER, PANTS, BOOTS, BELT, GLOW1, GLOW2, METAL, METAL2, GLOVE, PRINT = range(16)
HERO_GLOWS = {GLOW1, GLOW2}
rng = np.random.default_rng(77)


def hc(fr, to, mat, glow=None):
    """Cube on the hero atlas."""
    if glow is None and mat in HERO_GLOWS:
        glow = 'pulse'
    return cube(fr, to, mat, 'h', glow=glow)


# ---------------------------------------------------------------- textures
def fabric(color, weave=4, seam=None):
    a = np.zeros((T, T, 3)) + color
    yy, xx = np.mgrid[0:T, 0:T]
    a += (((xx + yy) % weave == 0) * -10 + ((xx - yy) % weave == 0) * 6)[..., None]
    a += rng.normal(0, 3, (T, T))[..., None]
    a += np.linspace(10, -14, T)[:, None, None]
    img = mt.to_img(a)
    if seam:
        d = ImageDraw.Draw(img)
        for y in seam:
            d.line([(0, y), (T, y)], fill=tuple(int(c * 0.55) for c in color))
            for x in range(2, T, 6): d.point((x, y + 2), fill=tuple(int(c * 0.7) for c in color))
    return img


def leather(color):
    a = np.zeros((T, T, 3)) + color
    yy = np.linspace(0, 1, T)[:, None]
    a += (np.sin(yy * 7) ** 8 * 55).repeat(T, 1)[..., None]
    a += np.linspace(14, -20, T)[:, None, None] + rng.normal(0, 2.5, (T, T))[..., None]
    img = mt.to_img(a)
    d = ImageDraw.Draw(img)
    d.line([(0, 40), (T, 40)], fill=tuple(int(c * 0.5) for c in color))
    return img


def skin(color):
    a = np.zeros((T, T, 3)) + color
    a += np.linspace(8, -10, T)[:, None, None] + rng.normal(0, 1.2, (T, T))[..., None]
    return mt.to_img(a).filter(ImageFilter.SMOOTH)


def hair(color, dark):
    a = np.zeros((T, T, 3)) + color
    xx = np.arange(T)
    strands = (np.sin(xx * 0.9) * 10 + np.sin(xx * 2.3) * 6)[None, :].repeat(T, 0)
    a += strands[..., None]
    a += np.linspace(14, -26, T)[:, None, None]
    shine = np.exp(-((np.arange(T) - 18) ** 2) / 18)[:, None].repeat(T, 1)
    a += (shine * 70 * (0.6 + 0.4 * np.sin(xx * 0.5)[None, :]))[..., None]
    img = mt.to_img(a)
    d = ImageDraw.Draw(img)
    for x in range(3, T, 7):
        d.line([(x, 30), (x + 2, T)], fill=dark)
    return img


def boots(color, sole=(200, 200, 205)):
    img = leather(color)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 50, T, T], fill=sole)
    d.line([(0, 50), (T, 50)], fill=(30, 30, 30))
    for x in range(6, T, 10): d.line([(x, 8), (x + 4, 14)], fill=tuple(min(255, c + 60) for c in color))
    return img


def belt(color, buckle=(210, 180, 60)):
    img = leather(color)
    d = ImageDraw.Draw(img)
    d.rectangle([24, 18, 40, 46], outline=buckle, width=3)
    return img


def print_tile(fg, bg, kind):
    img = Image.new('RGB', (T * 4, T * 4), bg)
    d = ImageDraw.Draw(img)
    s = T * 4
    if kind == 'bolt':
        d.polygon([(s * .55, s * .1), (s * .3, s * .55), (s * .48, s * .55), (s * .4, s * .9), (s * .72, s * .4), (s * .53, s * .4)], fill=fg)
    elif kind == 'eye':
        d.ellipse([s * .15, s * .3, s * .85, s * .7], outline=fg, width=14); d.ellipse([s * .4, s * .4, s * .6, s * .6], fill=fg)
    elif kind == 'skull':
        d.ellipse([s * .25, s * .15, s * .75, s * .65], fill=fg); d.rectangle([s * .35, s * .6, s * .65, s * .8], fill=fg)
        d.ellipse([s * .33, s * .32, s * .45, s * .46], fill=bg); d.ellipse([s * .55, s * .32, s * .67, s * .46], fill=bg)
    elif kind == 'stripes':
        for i in range(0, s, 40): d.rectangle([0, i, s, i + 18], fill=fg)
    return img.resize((T, T), Image.LANCZOS)


def anime_face(skin_col, eye_col, brow, mouth=(150, 70, 80), blush=False, extra=None):
    S = T * 4
    img = Image.new('RGB', (S, S), skin_col)
    d = ImageDraw.Draw(img)
    for cx in (S * 0.30, S * 0.70):
        w, h, cy = S * 0.20, S * 0.20, S * 0.58
        d.ellipse([cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2], fill=(248, 248, 252))
        iw, ih = w * 0.62, h * 0.86
        d.ellipse([cx - iw / 2, cy - ih / 2 + 4, cx + iw / 2, cy + ih / 2 + 4], fill=tuple(int(c * 0.65) for c in eye_col))
        d.ellipse([cx - iw / 2 + 6, cy - ih / 2 + 16, cx + iw / 2 - 6, cy + ih / 2], fill=eye_col)
        d.ellipse([cx - iw / 4, cy - ih / 4 + 6, cx + iw / 4, cy + ih / 4 + 6], fill=(20, 16, 24))
        d.ellipse([cx - iw / 2 + 8, cy - ih / 2 + 10, cx - iw / 2 + 26, cy - ih / 2 + 28], fill=(255, 255, 255))
        d.ellipse([cx + iw / 4 - 4, cy + ih / 4 - 2, cx + iw / 4 + 6, cy + ih / 4 + 8], fill=(255, 255, 255))
        d.arc([cx - w / 2 - 4, cy - h / 2 - 2, cx + w / 2 + 4, cy + h / 2 + 10], 195, 345, fill=(24, 20, 28), width=11)
        d.line([(cx - w / 2 + 6, cy - h * 0.95), (cx + w / 2 - 4, cy - h * 0.85)], fill=brow, width=8)
    d.line([(S * .5, S * .70), (S * .47, S * .76)], fill=tuple(int(c * 0.8) for c in skin_col), width=5)
    d.line([(S * .43, S * .86), (S * .57, S * .86)], fill=mouth, width=6)
    if blush:
        for cx in (S * .22, S * .78):
            d.ellipse([cx - 24, S * .72, cx + 24, S * .78], fill=tuple(min(255, int(c * 1.0 + k)) for c, k in zip(skin_col, (20, -25, -15))))
    if extra: extra(d, S)
    return img.resize((T, T), Image.LANCZOS)


def visor_face():
    S = T * 4
    a = np.asarray(mt.to_img(mt.brushed((70, 74, 84))).resize((S, S)), float)
    img = Image.fromarray(a.astype('uint8'))
    d = ImageDraw.Draw(img)
    d.rectangle([S * .12, S * .5, S * .88, S * .62], fill=(30, 0, 4))
    d.rectangle([S * .16, S * .53, S * .84, S * .59], fill=(255, 30, 40))
    for x in np.linspace(S * .2, S * .8, 7): d.line([(x, S * .7), (x, S * .9)], fill=(25, 25, 30), width=8)
    d.line([(S * .5, 0), (S * .5, S * .45)], fill=(40, 42, 50), width=10)
    return img.resize((T, T), Image.LANCZOS)


def build_atlas(tiles, path):
    atlas = Image.new('RGBA', (T * 4, T * 4))
    for i, t in enumerate(tiles):
        atlas.paste(t.convert('RGBA'), ((i % 4) * T, (i // 4) * T))
    atlas.save(path)


def metal(color):
    return mt.panel_lines(mt.to_img(mt.brushed(color)), 32, rivets=True)


# ---------------------------------------------------------------- geometry helpers
def part(items):
    """Top-level group of a body part: plain cubes plus animated child groups."""
    return group((0, 0, 0), [i for i in items if 'from' in i], children=[i for i in items if 'pivot' in i] or None)


def head(extra_hair):
    return [hc((-4, -8, -4), (4, 0, 4), SKIN), hc((-4, -8, -4.06), (4, 0, -4.0), FACE)] + extra_hair


def bangs(y0, y1_list, z=-4.6, x0=-4.2, width=None, mat=HAIR):
    n = len(y1_list); w = width or (8.4 / n)
    return [hc((x0 + i * w, y0, z), (x0 + (i + 1) * w + 0.05, y1, -4.04), mat if i % 2 else HAIR2 if mat == HAIR else mat) for i, y1 in enumerate(y1_list)]


def spike(px, py, pz, length, rx, rz, thick=1.6, mat=HAIR):
    return group((px, py, pz), [hc((px - thick / 2, py - length, pz - thick / 2), (px + thick / 2, py, pz + thick / 2), mat)], rot=[rx, 0, rz])


def standard_arm(sleeve, cuff, hand_mat=SKIN, sleeve_to=8.5, wide=0.35, extra=()):
    w = wide
    return [hc((-3 - w, -2 - w, -2 - w), (1 + w, sleeve_to, 2 + w), sleeve),
            hc((-3 - w - 0.05, sleeve_to - 0.7, -2 - w - 0.05), (1 + w + 0.05, sleeve_to, 2 + w + 0.05), cuff),
            hc((-3 + 0.1, sleeve_to, -2 + 0.1), (1 - 0.1, 10, 2 - 0.1), hand_mat)] + list(extra)


def standard_leg(pants, boot, boot_from=9.0, cuff=None):
    out = [hc((-2.15, 0, -2.15), (2.15, boot_from, 2.15), pants),
           hc((-2.3, boot_from, -2.75), (2.3, 12.15, 2.3), boot)]
    if cuff is not None: out.append(hc((-2.25, boot_from - 0.6, -2.25), (2.25, boot_from, 2.25), cuff))
    return out


HEROES = {}

# ================================================================ 1. Runner kid
kid_tiles = [skin((236, 196, 170)),
             anime_face((236, 196, 170), (70, 190, 120), (40, 34, 40)),
             hair((38, 36, 48), (18, 18, 24)), hair((30, 120, 110), (10, 60, 55)),
             leather((24, 150, 140)), fabric((26, 26, 32), seam=(20,)), fabric((232, 232, 236)), fabric((48, 50, 58), seam=(30,)),
             boots((236, 236, 240), sole=(60, 60, 66)), belt((30, 30, 34)),
             mt.to_img(mt.glow((50, 255, 130))), mt.to_img(mt.glow((0, 220, 255))),
             metal((170, 176, 188)), metal((70, 74, 86)), leather((30, 30, 34)), print_tile((24, 150, 140), (232, 232, 236), 'bolt')]
kid_hair = [hc((-4.4, -8.9, -4.4), (4.4, -6.6, 4.4), HAIR), hc((-4.45, -8, 3.5), (4.45, -2.5, 4.6), HAIR),
            hc((-4.6, -8, -3.6), (-3.9, -4, 4.4), HAIR), hc((3.9, -8, -3.6), (4.6, -4, 4.4), HAIR)] + \
           bangs(-8.4, [-5.2, -6.0, -4.8, -5.8, -5.0, -6.2]) + \
           [spike(x, -8.3, z, L, rx, rz, mat=HAIR if i % 2 else HAIR2) for i, (x, z, L, rx, rz) in enumerate([
               (-2.5, 2.5, 3.5, -40, 25), (0, 2.8, 4.0, -45, 0), (2.5, 2.5, 3.5, -40, -25), (-3, -0.5, 3.0, -15, 40),
               (3, -0.5, 3.0, -15, -40), (-1.2, -2.5, 2.8, 25, 20), (1.4, -2.5, 2.8, 25, -15), (0, 0, 3.2, -20, 0)])]
kid_body = [hc((-4, 0, -2), (4, 12, 2), INNER), hc((-2.6, 2, -2.06), (2.6, 6.5, -2.0), PRINT),
            hc((-4.1, 9.6, -2.1), (4.1, 10.5, 2.1), BELT),
            hc((-4.35, 0, -2.35), (-1.4, 11.2, -2.0), TOP), hc((1.4, 0, -2.35), (4.35, 11.2, -2.0), TOP),
            hc((-4.35, 0, -2.0), (-4.0, 11.2, 2.35), TOP), hc((4.0, 0, -2.0), (4.35, 11.2, 2.35), TOP),
            hc((-4.35, 0, 2.0), (4.35, 11.2, 2.35), TOP),
            hc((-4.4, 10.4, -2.4), (-1.35, 11.6, 2.4), TOP2), hc((1.35, 10.4, -2.4), (4.4, 11.6, 2.4), TOP2),
            hc((-4.5, -1.0, -2.6), (4.5, 0.6, 2.6), TOP2),
            hc((-1.5, -1.0, -2.65), (1.5, 0.6, -2.55), INNER)] + \
           [hc((-0.5, 1.2 + i * 2.0, 2.36), (0.5, 2.0 + i * 2.0, 2.6), GLOW1) for i in range(5)] + \
           [hc((-3.0, 3.0, 2.36), (3.0, 3.4, 2.4), GLOW2, glow='pulse')]
kid_arm = standard_arm(TOP, TOP2, extra=[hc((-3.4, 4.0, -2.4), (-3.35, 7.5, 2.4), GLOW2, glow='pulse')])
kid_leg = standard_leg(PANTS, BOOTS, cuff=TOP2) + [hc((-2.25, 3, -2.25), (-1.0, 6, -2.15), TOP2)]
HEROES['runner_kid'] = {"scale": 1.0, "tiles": kid_tiles, "glow": gm.G_GREEN, "parts": {
    "head": [part(head(kid_hair))], "body": [part(kid_body)],
    "right_arm": [part(kid_arm)], "left_arm": mirror_x([part(kid_arm)]),
    "right_leg": [part(kid_leg)], "left_leg": [part(kid_leg)]}}

# ================================================================ 2. Netrunner girl
def net_extra(d, S):
    for cx in (S * .30, S * .70):
        d.line([(cx - 24, S * .74), (cx + 18, S * .74)], fill=(120, 240, 255), width=5)
        d.line([(cx + 18, S * .74), (cx + 26, S * .80)], fill=(120, 240, 255), width=5)
net_tiles = [skin((242, 214, 200)),
             anime_face((242, 214, 200), (170, 90, 230), (150, 130, 200), mouth=(190, 90, 120), blush=True, extra=net_extra),
             hair((205, 196, 238), (150, 130, 200)), hair((160, 140, 215), (110, 90, 170)),
             leather((236, 236, 242)), fabric((120, 60, 190)), fabric((22, 20, 30)), fabric((22, 20, 30)),
             boots((40, 30, 60), sole=(25, 25, 30)), belt((30, 26, 40), buckle=(190, 120, 255)),
             mt.to_img(mt.glow((170, 60, 255))), mt.to_img(mt.glow((120, 230, 255))),
             metal((190, 196, 210)), metal((70, 60, 90)), fabric((22, 20, 30)), print_tile((170, 60, 255), (236, 236, 242), 'eye')]
lock = lambda x0, x1, z0, z1: group(((x0 + x1) / 2, -7, (z0 + z1) / 2), [hc((x0, -8, z0), (x1, 2.5, z1), HAIR), hc((x0 + 0.1, 0.5, z0 + 0.1), (x1 - 0.1, 3.5, z1 - 0.1), HAIR2)],
                                    anims=[rot_anim("x", "sway", amp=7, phase=0.4), rot_anim("x", "breath", amp=2)])
net_hair = [hc((-4.45, -8.9, -4.45), (4.45, -6.7, 4.45), HAIR), hc((-4.55, -8, 3.4), (4.55, 0.5, 4.7), HAIR),
            hc((-4.6, -8, -3.2), (-3.9, -1, 4.5), HAIR), hc((3.9, -8, -3.2), (4.6, -1, 4.5), HAIR),
            group((0, 0, 4), [hc((-4.3, 0.5, 3.6), (4.3, 3.5, 4.6), HAIR2)], anims=[rot_anim("x", "sway", amp=6, phase=1.0)]),
            lock(-4.8, -3.5, -4.5, -2.4), lock(3.5, 4.8, -4.5, -2.4)] + \
           bangs(-8.5, [-5.6, -6.2, -5.3, -6.0, -5.4, -6.4, -5.5]) + \
           [hc((-3.0, -2.85, -4.12), (-1.2, -2.7, -4.07), GLOW2, glow='flicker'), hc((1.2, -2.85, -4.12), (3.0, -2.7, -4.07), GLOW2, glow='flicker'),
            hc((-1.0, -1.0, 4.0), (1.0, 0.0, 4.4), METAL2), hc((-0.5, -0.8, 4.4), (0.5, -0.2, 4.5), GLOW1)]
net_body = [hc((-4, 0, -2), (4, 12, 2), INNER),
            hc((-0.15, 1, -2.06), (0.15, 11, -2.0), GLOW1, glow='pulse'), hc((-4.06, 2, -0.15), (-4.0, 11, 0.15), GLOW1, glow='pulse'),
            hc((4.0, 2, -0.15), (4.06, 11, 0.15), GLOW1, glow='pulse'),
            hc((-4.35, 0, -2.35), (-1.8, 6.2, -2.0), TOP), hc((1.8, 0, -2.35), (4.35, 6.2, -2.0), TOP),
            hc((-4.35, 0, -2.0), (-4.0, 6.2, 2.35), TOP), hc((4.0, 0, -2.0), (4.35, 6.2, 2.35), TOP), hc((-4.35, 0, 2.0), (4.35, 6.6, 2.35), TOP),
            hc((-2.5, 1.5, 2.36), (2.5, 5.5, 2.42), PRINT),
            hc((-4.5, -0.8, -2.6), (4.5, 0.5, 2.6), TOP2), hc((-4.1, 9.8, -2.1), (4.1, 10.6, 2.1), BELT),
            hc((-2.5, 7.0, 2.0), (2.5, 9.8, 3.3), METAL2), hc((-2.0, 7.6, 3.3), (2.0, 9.2, 3.4), GLOW2, glow='flicker')]
net_arm = standard_arm(TOP, TOP2, hand_mat=GLOVE, extra=[hc((-3.4, 8.8, -2.4), (1.4, 9.2, 2.4), GLOW1, glow='pulse')])
net_leg = standard_leg(PANTS, BOOTS, boot_from=7.5) + [hc((-2.25, 2.5, -2.25), (2.25, 3.2, 2.25), BELT), hc((-0.15, 3.5, -2.2), (0.15, 7.4, -2.15), GLOW1, glow='pulse')]
HEROES['netrunner_girl'] = {"scale": 0.95, "tiles": net_tiles, "glow": gm.G_PINK, "parts": {
    "head": [part(head(net_hair))], "body": [part(net_body)],
    "right_arm": [part(net_arm)], "left_arm": mirror_x([part(net_arm)]),
    "right_leg": [part(net_leg)], "left_leg": [part(net_leg)]}}

# ================================================================ 3. Gunner girl
gun_tiles = [skin((240, 206, 186)),
             anime_face((240, 206, 186), (60, 220, 120), (200, 40, 120), mouth=(200, 70, 90), blush=True,
                        extra=lambda d, S: d.polygon([(S * .53, S * .86), (S * .56, S * .86), (S * .545, S * .91)], fill=(255, 255, 255))),
             hair((236, 60, 150), (150, 20, 90)), hair((180, 30, 110), (110, 10, 70)),
             leather((28, 28, 34)), fabric((236, 60, 150)), fabric((40, 40, 46)), fabric((36, 36, 44), seam=(10,)),
             boots((30, 30, 36), sole=(236, 60, 150)), belt((30, 30, 34), buckle=(236, 60, 150)),
             mt.to_img(mt.glow((255, 40, 140))), mt.to_img(mt.glow((60, 255, 160))),
             metal((40, 170, 160)), metal((60, 64, 76)), metal((60, 64, 76)), print_tile((236, 60, 150), (28, 28, 34), 'skull')]
def tail(s):
    """Bouncing twin tail on side s (+1 right, -1 left)."""
    x0, x1 = sorted((s * 3.0, s * 6.2))
    xa, xb = sorted((s * 3.3, s * 5.9))
    xe, xf = sorted((s * 3.7, s * 5.5))
    xc, xd = sorted((s * 3.2, s * 5.4))
    return group((s * 4.2, -6.8, 4.4), [hc((xc, -7.8, 3.4), (xd, -6.4, 5.4), GLOW1, glow='pulse'),
                                        hc((x0, -6.4, 3.5), (x1, 0.5, 6.2), HAIR),
                                        hc((xa, 0.5, 3.8), (xb, 5.0, 6.0), HAIR),
                                        hc((xe, 5.0, 4.1), (xf, 8.0, 5.7), HAIR2)],
                 anims=[rot_anim("z", "sway", amp=-s * 9, phase=0.3), rot_anim("x", "sway", amp=8, phase=1.2), rot_anim("z", "breath", amp=-s * 3)])


gun_hair = [hc((-4.45, -8.9, -4.45), (4.45, -6.6, 4.45), HAIR), hc((-4.5, -8, 3.5), (4.5, -3, 4.6), HAIR),
            hc((-4.6, -8, -3.4), (-3.9, -3.5, 4.4), HAIR), hc((3.9, -8, -3.4), (4.6, -3.5, 4.4), HAIR),
            tail(1), tail(-1)] + bangs(-8.5, [-6.4, -5.4, -6.6, -5.6, -6.3])
gun_body = [hc((-4, 0, -2), (4, 12, 2), INNER), hc((-2.5, 1.5, -2.06), (2.5, 5.5, -2.0), PRINT),
            hc((-4.7, 0, -2.75), (-1.6, 12.2, -2.4), TOP), hc((1.6, 0, -2.75), (4.7, 12.2, -2.4), TOP),
            hc((-4.7, 0, -2.4), (-4.3, 12.2, 2.75), TOP), hc((4.3, 0, -2.4), (4.7, 12.2, 2.75), TOP), hc((-4.7, 0, 2.4), (4.7, 12.4, 2.75), TOP),
            hc((-1.65, 0.5, -2.7), (-1.55, 12, -2.4), TOP2), hc((1.55, 0.5, -2.7), (1.65, 12, -2.4), TOP2),
            hc((-4.0, -1.5, 2.2), (4.0, 1.2, 3.6), TOP), hc((-3.6, 1.2, 2.75), (3.6, 1.4, 3.0), TOP2),
            hc((-4.1, 9.6, -2.1), (4.1, 10.4, 2.1), BELT)]
gun_arm = [hc((-3.9, -2.6, -2.9), (1.9, 5.0, 2.9), TOP), hc((-3.95, 4.3, -2.95), (1.95, 5.0, 2.95), TOP2),
           hc((-3.2, 5.0, -2.2), (1.2, 9.2, 2.2), METAL), hc((-3.25, 6.0, -0.2), (-3.2, 8.8, 0.2), GLOW1, glow='pulse'),
           hc((1.2, 6.0, -0.2), (1.25, 8.8, 0.2), GLOW1, glow='pulse'),
           hc((-3.0, 9.2, -2.0), (1.0, 10.3, 2.0), METAL2), hc((-2.0, 9.6, -2.1), (0.0, 10.0, -2.0), GLOW2)]
gun_leg = [hc((-2.2, 0, -2.2), (2.2, 3.5, 2.2), PANTS), hc((-2.05, 3.5, -2.05), (2.05, 8.0, 2.05), INNER),
           hc((-2.4, 8.0, -2.8), (2.4, 12.2, 2.4), BOOTS), hc((-2.45, 8.0, -2.85), (2.45, 8.6, 2.45), TOP2)]
HEROES['gunner_girl'] = {"scale": 0.88, "tiles": gun_tiles, "glow": gm.G_PINK, "parts": {
    "head": [part(head(gun_hair))], "body": [part(gun_body)],
    "right_arm": [part(gun_arm)], "left_arm": mirror_x([part(gun_arm)]),
    "right_leg": [part(gun_leg)], "left_leg": [part(gun_leg)]}}

# ================================================================ 4. Veteran borg
def borg_face_extra(d, S):
    for i in range(160):
        x, y = rng.integers(int(S * .2), int(S * .8)), rng.integers(int(S * .76), int(S * .98))
        d.point((x, y), fill=(120, 96, 84))
    d.ellipse([S * .58, S * .48, S * .82, S * .68], fill=(40, 40, 46)); d.ellipse([S * .64, S * .52, S * .76, S * .64], fill=(255, 60, 30))
    d.line([(S * .22, S * .38), (S * .34, S * .66)], fill=(170, 110, 100), width=6)
vet_tiles = [skin((206, 160, 130)),
             anime_face((206, 160, 130), (90, 80, 70), (40, 34, 30), mouth=(120, 70, 60), extra=borg_face_extra),
             hair((60, 58, 62), (40, 38, 42)), hair((90, 88, 92), (60, 58, 62)),
             fabric((96, 104, 64), seam=(16, 44)), fabric((70, 76, 46)), fabric((40, 42, 46)), fabric((58, 60, 50), seam=(28,)),
             boots((50, 42, 34), sole=(30, 30, 30)), belt((60, 50, 36)),
             mt.to_img(mt.glow((255, 120, 20))), mt.to_img(mt.glow((255, 40, 30))),
             metal((150, 156, 168)), metal((64, 68, 80)), metal((64, 68, 80)), print_tile((255, 120, 20), (96, 104, 64), 'stripes')]
vet_head = head([hc((-4.25, -8.4, -4.25), (4.25, -7.0, 4.25), HAIR), hc((-4.3, -7.5, 3.6), (4.3, -3.5, 4.35), HAIR),
                 hc((-4.35, -7.5, -2), (-4.0, -4.5, 4.3), HAIR2), hc((4.0, -7.5, -2), (4.35, -4.5, 4.3), HAIR2),
                 hc((2.0, -5.2, -4.25), (4.3, -1.5, -2.5), METAL2), hc((2.6, -4.3, -4.32), (3.4, -3.5, -4.25), GLOW1)])
vet_body = [hc((-4, 0, -2), (4, 12, 2), INNER),
            hc((-4.6, 0, -2.6), (4.6, 9.5, 2.6), TOP), hc((-3.8, 3, -3.2), (-1.0, 6, -2.6), TOP2), hc((1.0, 3, -3.2), (3.8, 6, -2.6), TOP2),
            hc((-3.0, 6.5, -3.0), (3.0, 8.5, -2.6), TOP2), hc((-2.5, 1.0, 2.6), (2.5, 6.0, 2.7), PRINT),
            hc((-4.2, 9.6, -2.2), (4.2, 10.6, 2.2), BELT), hc((-4.8, -1.2, -2.8), (4.8, 0.4, 2.8), METAL2)]
vet_arm = [hc((-4.6, -2.9, -3.1), (1.6, 1.6, 3.1), METAL2), hc((-4.7, 0.6, -3.2), (1.7, 1.0, 3.2), GLOW1, glow='breath'),
           hc((-3.7, 1.6, -2.7), (1.7, 4.6, 2.7), METAL), hc((-4.0, 4.2, -1.5), (2.0, 5.2, 1.5), METAL2),
           hc((-4.6, 5.0, -3.3), (2.3, 10.4, 3.3), METAL), hc((-4.7, 6.0, -1.0), (-4.6, 9.5, 1.0), GLOW1, glow='heat'),
           hc((-4.3, 10.4, -3.3), (2.0, 12.0, 3.0), METAL2)] + \
          [hc((-4.0 + k * 1.6, 11.0, -3.45), (-3.0 + k * 1.6, 11.8, -3.3), GLOW1) for k in range(4)]
vet_leg = standard_leg(PANTS, BOOTS, boot_from=8.0, cuff=PANTS) + [hc((-2.4, 4, -2.6), (2.4, 6.5, -2.1), METAL2), hc((-2.3, 1, -2.3), (-1.0, 4, -2.2), TOP2)]
HEROES['veteran_borg'] = {"scale": 1.12, "tiles": vet_tiles, "glow": gm.G_RED, "parts": {
    "head": [part(vet_head)], "body": [part(vet_body)],
    "right_arm": [part(vet_arm)], "left_arm": mirror_x([part(vet_arm)]),
    "right_leg": [part(vet_leg)], "left_leg": [part(vet_leg)]}}

# ================================================================ 5. Chrome tyrant
ty_tiles = [metal((60, 64, 76)), visor_face(), metal((40, 42, 50)), metal((110, 20, 26)),
            metal((150, 156, 170)), metal((120, 22, 30)), metal((34, 36, 44)), metal((80, 84, 96)),
            metal((40, 42, 50)), metal((30, 30, 36)),
            mt.to_img(mt.glow((255, 30, 40))), mt.to_img(mt.glow((255, 120, 20))),
            metal((190, 196, 210)), metal((70, 74, 86)), metal((40, 42, 50)), mt.chrome_tiles[9]]
ty_head = [hc((-4.3, -8.4, -4.3), (4.3, 0.2, 4.3), METAL2), hc((-4.3, -8.4, -4.38), (4.3, 0.2, -4.3), FACE),
           hc((-3.6, -4.9, -4.48), (3.6, -4.2, -4.38), GLOW1, glow='pulse'),
           hc((-0.6, -10.5, -1.5), (0.6, -8.4, 3.5), TOP2), hc((-4.6, -6, -1), (-4.3, -2, 2), METAL), hc((4.3, -6, -1), (4.6, -2, 2), METAL)]
ty_body = [hc((-5, -0.5, -3.2), (5, 8, 3.2), TOP), hc((-4.4, 8, -2.8), (4.4, 12, 2.8), INNER),
           hc((-2, 2, -3.45), (2, 5, -3.2), GLOW1, glow='breath'), hc((-5.2, 1, -3.35), (-3, 7, -3.2), TOP2), hc((3, 1, -3.35), (5.2, 7, -3.2), TOP2)] + \
          [hc((-4.0, 8.4 + i * 1.2, -2.95), (4.0, 9.2 + i * 1.2, -2.8), METAL2) for i in range(3)] + \
          [hc((-4, 0.5, 3.2), (-0.8, 7, 5.2), METAL2), hc((0.8, 0.5, 3.2), (4, 7, 5.2), METAL2),
           hc((-3.6, 1, 5.2), (-1.2, 6.5, 5.3), PRINT, glow='heat'), hc((1.2, 1, 5.2), (3.6, 6.5, 5.3), PRINT, glow='heat')]
ty_arm = [hc((-6, -3.6, -3.6), (1.8, 1.6, 3.6), TOP2), hc((-6.1, 0.8, -3.7), (1.9, 1.2, 3.7), GLOW1, glow='pulse'),
          hc((-3.8, 1.6, -2.8), (1.8, 4.6, 2.8), METAL), hc((-4.1, 4.2, -1.6), (2.1, 5.2, 1.6), METAL2),
          hc((-4.4, 5.0, -3.2), (2.4, 10.6, 3.2), TOP), hc((-4.5, 6.0, -1.2), (-4.4, 9.5, 1.2), GLOW2, glow='heat'),
          hc((-4.0, 10.6, -3.0), (2.0, 12.0, 3.0), METAL2)] + \
         [hc((-3.6 + k * 1.7, 12.0, -3.0), (-2.8 + k * 1.7, 13.4, -1.6), METAL) for k in range(4)]
ty_leg = [hc((-2.6, 0, -2.6), (2.6, 8, 2.6), METAL2), hc((-2.8, 3.5, -3.2), (2.8, 6.5, -2.6), TOP2),
          hc((-2.9, 8, -3.4), (2.9, 12.2, 2.9), TOP), hc((-0.5, 4.5, -3.3), (0.5, 5.5, -3.2), GLOW1)]
HEROES['chrome_tyrant'] = {"scale": 1.3, "tiles": ty_tiles, "glow": gm.G_RED, "parts": {
    "head": [part(ty_head)], "body": [part(ty_body)],
    "right_arm": [part(ty_arm)], "left_arm": mirror_x([part(ty_arm)]),
    "right_leg": [part(ty_leg)], "left_leg": [part(ty_leg)]}}


def chip_item(hid, glow_mat):
    els = [gm.box([4, 0, 2], [12, 1.2, 14], gm.PCB), gm.box([4.5, 1.2, 3], [11.5, 2.2, 9], gm.PANEL),
           gm.box([5.5, 2.2, 4], [10.5, 2.5, 8], glow_mat), gm.box([5, 1.2, 10], [11, 1.8, 13], gm.CERAMIC),
           gm.box([6, 0.1, 0.5], [10, 1.1, 2], gm.GOLD), gm.box([4.2, 1.2, 9.4], [11.8, 1.4, 9.8], glow_mat)]
    for e in els:
        for f in e['faces'].values(): f['texture'] = '#atlas'
    disp = gm.display_for(els)
    disp['gui'] = {"rotation": [55, 200, 0], "scale": [0.8, 0.8, 0.8]}
    gm.write(f'{ROOT}/models/item/3d/persona_{hid}.json', {"textures": {"atlas": "edgerunners:item/cyberware", "particle": "edgerunners:item/cyberware"},
                                                           "elements": els, "display": disp})
    gm.write(f'{ROOT}/models/item/persona_{hid}.json', {"parent": f"edgerunners:item/3d/persona_{hid}"})


if __name__ == '__main__':
    os.makedirs(f'{ROOT}/hero', exist_ok=True)
    os.makedirs(f'{ROOT}/textures/hero', exist_ok=True)
    for hid, spec in HEROES.items():
        build_atlas(spec['tiles'], f'{ROOT}/textures/hero/{hid}.png')
        gm.write(f'{ROOT}/hero/{hid}.json', {"textures": {"h": f"edgerunners:textures/hero/{hid}.png"}, "scale": spec['scale'], "parts": spec['parts']})
        chip_item(hid, spec['glow'])
    print('heroes:', ', '.join(HEROES))
