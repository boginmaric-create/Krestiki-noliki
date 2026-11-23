"""Generates the 3D item models (cube elements) and the shared palette texture.
Run from the repository root: python3 tools/gen_models.py"""
import json, os
from PIL import Image

ROOT = 'src/main/resources/assets/edgerunners'
PALETTE = [(20,18,30),(95,100,115),(175,182,196),(240,244,250),(252,238,10),(0,240,255),(255,42,109),(57,255,136),
           (14,80,52),(212,160,23),(200,38,45),(110,16,24),(30,30,45),(150,20,70),(0,120,140),(60,64,76)]
DARK, GRAY, LIGHT, WHITE, YELLOW, CYAN, PINK, GREEN, DGREEN, GOLD, RED, DRED, BLACK, DPINK, DCYAN, STEEL = range(16)

def uv(c):
    x, y = (c % 4) * 4, (c // 4) * 4
    return [x + 1, y + 1, x + 3, y + 3]

def box(fr, to, color, glow=False):
    e = {"from": fr, "to": to,
         "faces": {d: {"uv": uv(color), "texture": "#palette"} for d in ("north", "south", "east", "west", "up", "down")}}
    if glow:
        e["shade"] = False
        e["forge_data"] = {"block_light": 15, "sky_light": 15}
    return e

def path(points, size, color, glow=False, steps=4):
    out, h = [], size / 2
    for (a, b) in zip(points, points[1:]):
        for i in range(steps):
            t = i / steps
            x, y, z = (a[k] + (b[k] - a[k]) * t for k in range(3))
            out.append(box([x - h, y - h, z - h], [x + h, y + h, z + h], color, glow))
    x, y, z = points[-1]
    out.append(box([x - h, y - h, z - h], [x + h, y + h, z + h], color, glow))
    return out

DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -25, 0], "translation": [1.5, 3, 1], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, -25, 0], "translation": [1.5, 3, 1], "scale": [0.6, 0.6, 0.6]},
    "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"scale": [0.8, 0.8, 0.8]},
    "head": {"translation": [0, 13, 7], "scale": [0.8, 0.8, 0.8]},
    "gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]},
}

M = {}

# Mantis blade: wrist mount + folding blade with a glowing edge
M['mantis_blade'] = [
    box([6, 0, 6], [10, 4, 10], GRAY), box([5.8, 1, 5.8], [10.2, 2, 10.2], YELLOW),
    box([7, 4, 7], [9, 6, 9], DARK),
    box([7.5, 5, 6], [8.5, 15, 10], LIGHT), box([7.6, 5, 10], [8.4, 15, 10.6], PINK, True),
    box([7.4, 6, 5.6], [8.6, 13, 6], DARK),
    box([7.5, 15, 7], [8.5, 17, 10], WHITE), box([7.6, 15, 10], [8.4, 17, 10.4], PINK, True),
    box([7.5, 17, 8.5], [8.5, 18.5, 10], WHITE),
]

# Sandevistan: spinal column with neon nodes and a glowing data cable
s = [box([7, 1, 7], [9, 15, 9], STEEL), box([7.5, 1, 10], [8.5, 15, 10.6], GREEN, True)]
for i, y in enumerate([1, 4, 7, 10, 13]):
    s.append(box([5, y, 6], [11, y + 2, 10], LIGHT))
    s.append(box([6.5, y + 0.5, 5.6], [9.5, y + 1.5, 6], DARK))
    c = YELLOW if i % 2 else GREEN
    s.append(box([4, y + 0.5, 7.5], [5, y + 1.5, 8.5], c, True))
    s.append(box([11, y + 0.5, 7.5], [12, y + 1.5, 8.5], GREEN if i % 2 else YELLOW, True))
M['sandevistan'] = s

# Kiroshi optics: cyber-eye with layered glowing iris
M['kiroshi_optics'] = [
    box([4, 4, 5], [12, 12, 11], GRAY), box([4.5, 4.5, 4.5], [11.5, 11.5, 5], LIGHT),
    box([5.5, 5.5, 4.2], [10.5, 10.5, 4.5], RED, True), box([6.5, 6.5, 4.0], [9.5, 9.5, 4.2], YELLOW, True),
    box([7.25, 7.25, 3.8], [8.75, 8.75, 4.0], BLACK),
    box([7, 7, 11], [9, 9, 14], DARK), box([3, 6, 6], [4, 10, 10], DARK), box([12, 6, 6], [13, 10, 10], DARK),
    box([4, 11.5, 6], [12, 12.5, 7], YELLOW),
]

# Monowire: wrist cuff, emitter and a curling glowing wire with a weight
mw = [box([4, 0, 4], [12, 4, 12], GRAY), box([3.8, 1.5, 3.8], [12.2, 2.5, 12.2], YELLOW),
      box([7, 4, 7], [9, 5, 9], DPINK)]
mw += path([(8, 5, 8), (8, 8, 9.5), (8, 11, 9), (8, 13, 7), (8.5, 14, 4.5), (9, 13, 2.5), (9, 11.5, 2)], 0.6, PINK, True)
mw.append(box([8.3, 10.3, 1.3], [9.7, 11.7, 2.7], WHITE, True))
M['monowire'] = mw

# Reinforced tendons: two muscle bundles held by steel clamps
t = [box([5, 2, 7], [7, 14, 9], RED), box([9, 2, 7], [11, 14, 9], RED), box([7, 3, 7.5], [9, 13, 8.5], DRED)]
for y in (1, 7, 13.5):
    t.append(box([4.5, y, 6.5], [11.5, y + 1.5, 9.5], LIGHT))
    t.append(box([7.5, y + 0.25, 6.2], [8.5, y + 1.25, 6.5], CYAN, True))
M['reinforced_tendons'] = t

# Gorilla arms: armoured fist with knuckle lights
g = [box([5, 0, 5], [11, 5, 11], GRAY), box([4.8, 3, 4.8], [11.2, 4, 11.2], YELLOW),
     box([3.5, 5, 4.5], [12.5, 11, 11.5], STEEL), box([4, 6, 11.5], [12, 10, 12], DARK),
     box([12.5, 6, 6], [14, 10, 10], LIGHT)]
for i in range(4):
    x = 3.6 + i * 2.25
    g.append(box([x, 11, 5], [x + 2, 14, 10], LIGHT))
    g.append(box([x + 0.5, 12, 4.7], [x + 1.5, 13, 5], CYAN, True))
M['gorilla_arms'] = g

# Immunosuppressant: auto-injector
M['immunosuppressant'] = [
    box([6.5, 3, 6.5], [9.5, 11, 9.5], DCYAN), box([6.3, 4, 7.4], [9.7, 10, 8.6], CYAN, True),
    box([6, 2, 6], [10, 3, 10], GRAY), box([6, 11, 6], [10, 12, 10], GRAY),
    box([4.5, 11, 7.5], [11.5, 11.6, 8.5], GRAY),
    box([7.5, 12, 7.5], [8.5, 15, 8.5], LIGHT), box([6.5, 15, 6.5], [9.5, 15.6, 9.5], GRAY),
    box([7.75, 0, 7.75], [8.25, 2, 8.25], WHITE),
]

# Ripperdoc kit: medical case
M['ripperdoc_kit'] = [
    box([1, 1, 4], [15, 8, 12], DRED), box([1, 8, 4], [15, 9.5, 12], RED),
    box([3, 6, 3.7], [5, 8.5, 4], GOLD), box([11, 6, 3.7], [13, 8.5, 4], GOLD),
    box([5.5, 9.5, 7.5], [6.5, 11, 8.5], GRAY), box([9.5, 9.5, 7.5], [10.5, 11, 8.5], GRAY),
    box([5.5, 11, 7.5], [10.5, 12, 8.5], GRAY),
    box([7.25, 2, 3.7], [8.75, 7, 4], WHITE), box([5.75, 3.75, 3.7], [10.25, 5.25, 4], WHITE),
    box([1, 0, 4], [15, 1, 12], DARK),
]

# Cyber chip: circuit board with pins and a glowing core
c = [box([3, 0, 3], [13, 1, 13], DGREEN), box([6, 1, 6], [10, 2, 10], BLACK), box([7, 2, 7], [9, 2.3, 9], CYAN, True),
     box([4, 1, 4], [6, 1.15, 4.6], GREEN, True), box([10, 1, 11.4], [12, 1.15, 12], GREEN, True),
     box([11.4, 1, 4], [12, 1.15, 6], GREEN, True), box([4, 1, 10], [4.6, 1.15, 12], GREEN, True)]
for i in (4.5, 6.5, 8.5, 10.5):
    c += [box([i, 0, 1.5], [i + 1, 0.6, 3], GOLD), box([i, 0, 13], [i + 1, 0.6, 14.5], GOLD),
          box([1.5, 0, i], [3, 0.6, i + 1], GOLD), box([13, 0, i], [14.5, 0.6, i + 1], GOLD)]
M['cyber_chip'] = c

if __name__ == '__main__':
    img = Image.new('RGBA', (16, 16))
    for i, col in enumerate(PALETTE):
        for y in range(4):
            for x in range(4):
                img.putpixel(((i % 4) * 4 + x, (i // 4) * 4 + y), col + (255,))
    img.save(f'{ROOT}/textures/item/palette.png')
    for name, elements in M.items():
        for e in elements:  # details were authored facing -Z; mirror so they face the viewer (+Z)
            e['from'][2], e['to'][2] = 16 - e['to'][2], 16 - e['from'][2]
        display = json.loads(json.dumps(DISPLAY))
        if name == 'cyber_chip':  # hold the flat chip upright
            for k in ('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand'):
                display[k]['rotation'] = [-70, 0, 0]
        model = {"textures": {"palette": "edgerunners:item/palette", "particle": f"edgerunners:item/{name}"},
                 "elements": elements, "display": display}
        with open(f'{ROOT}/models/item/3d/{name}.json', 'w') as f:
            json.dump(model, f, indent=1)
        flat = "minecraft:item/handheld" if name == 'mantis_blade' else "minecraft:item/generated"
        wrapper = {"loader": "forge:separate_transforms",
                   "base": {"parent": f"edgerunners:item/3d/{name}"},
                   "perspectives": {"gui": {"parent": flat, "textures": {"layer0": f"edgerunners:item/{name}"}}}}
        with open(f'{ROOT}/models/item/{name}.json', 'w') as f:
            json.dump(wrapper, f, indent=1)
    print('generated', len(M), 'models')
