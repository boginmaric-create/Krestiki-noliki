"""Generates the detailed 3D item models, the ripperdoc chair block model and the material atlas.
Run from the repository root: python3 tools/gen_models.py

Every face samples a 4x4-unit tile of textures/item/cyberware.png (256x256, 16 materials),
so a model unit gets up to 16 texels of detail."""
import json, os, math
import gen_materials

ROOT = 'src/main/resources/assets/edgerunners'

# Material ids = tile index in the atlas (see gen_materials.py)
STEEL, PANEL, CHROME, CARBON, RUBBER, HAZARD, G_CYAN, G_PINK, G_GREEN, G_RED, MUSCLE, LENS, GOLD, PCB, CERAMIC, REDPL = range(16)
GLOWS = {G_CYAN, G_PINK, G_GREEN, G_RED}

_counter = [0]


def _face_uv(mat, w, h):
    tx, ty = (mat % 4) * 4, (mat // 4) * 4
    w, h = max(0.25, min(w, 4)), max(0.25, min(h, 4))
    # small faces sample different spots of the tile so neighbours don't look identical
    k = _counter[0]
    ox = ((k * 1.37) % (4 - w)) if w < 4 else 0
    oy = ((k * 0.73) % (4 - h)) if h < 4 else 0
    return [round(tx + ox, 3), round(ty + oy, 3), round(tx + ox + w, 3), round(ty + oy + h, 3)]


def box(fr, to, mat, glow=None):
    _counter[0] += 1
    dx, dy, dz = (to[i] - fr[i] for i in range(3))
    dims = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}
    e = {"from": list(fr), "to": list(to),
         "faces": {d: {"uv": _face_uv(mat, *wh), "texture": "#atlas"} for d, wh in dims.items()}}
    if glow if glow is not None else mat in GLOWS:
        e["shade"] = False
        e["forge_data"] = {"block_light": 15, "sky_light": 15}
    return e


def rot(e, angle, origin, axis='x'):
    e['rotation'] = {"origin": list(origin), "axis": axis, "angle": angle}
    return e


def path(points, size, mat, steps=4):
    out, h = [], size / 2
    for (a, b) in zip(points, points[1:]):
        for i in range(steps):
            t = i / steps
            x, y, z = (a[k] + (b[k] - a[k]) * t for k in range(3))
            out.append(box([x - h, y - h, z - h], [x + h, y + h, z + h], mat))
    x, y, z = points[-1]
    out.append(box([x - h, y - h, z - h], [x + h, y + h, z + h], mat))
    return out


M = {}

# ---------------------------------------------------------------- Mantis blade
mb = [
    box([5.5, 0, 5.5], [10.5, 5, 10.5], PANEL),
    box([5, 0.5, 6], [5.5, 4.5, 10], STEEL), box([10.5, 0.5, 6], [11, 4.5, 10], STEEL),
    box([6, 1, 5], [10, 2, 5.5], HAZARD), box([6.5, 2.5, 5.1], [9.5, 4.5, 5.5], CARBON),
    box([6.5, 5, 7], [9.5, 7, 9], CHROME), box([5.8, 5.5, 7.5], [10.2, 6.5, 8.5], STEEL),
    box([7.4, 4, 9.3], [8.6, 6.5, 10.5], PANEL), box([7.6, 6.5, 9.6], [8.4, 9, 10.2], CHROME),
    # three folding blade segments, each a little thinner
    box([7.5, 7, 6], [8.5, 12, 10], STEEL),
    box([7.55, 12, 6.3], [8.45, 17, 10], STEEL),
    box([7.6, 17, 6.8], [8.4, 21, 10], CHROME),
    rot(box([7.62, 20, 7.4], [8.38, 23.5, 9.6], CHROME), 22.5, (8, 21, 8.5)),
    # glowing cutting edge + chrome bevel
    box([7.8, 7.5, 10], [8.2, 21, 10.25], CHROME),
    box([7.85, 8, 10.25], [8.15, 21, 10.6], G_PINK),
    rot(box([7.85, 20.5, 9.5], [8.15, 23, 9.8], G_PINK), 22.5, (8, 21, 8.5)),
    # spine with serrations
    box([7.45, 7, 5.6], [8.55, 19, 6.1], PANEL),
    # segment joints
    box([8.45, 11.8, 7.2], [8.6, 12.2, 9.4], G_CYAN), box([7.4, 11.8, 7.2], [7.55, 12.2, 9.4], G_CYAN),
    box([8.4, 16.8, 7.4], [8.55, 17.2, 9.4], G_CYAN), box([7.45, 16.8, 7.4], [7.6, 17.2, 9.4], G_CYAN),
]
for y in range(8, 19, 2):
    mb.append(box([7.55, y, 5.2], [8.45, y + 0.8, 5.6], STEEL))
M['mantis_blade'] = mb

# ---------------------------------------------------------------- Sandevistan
s = [box([7.25, 0, 7.25], [8.75, 16, 8.75], CHROME),
     box([6, 0, 10.5], [6.8, 16, 11.3], RUBBER), box([9.2, 0, 10.5], [10, 16, 11.3], RUBBER),
     box([7.7, 0, 11.6], [8.3, 16, 12.0], G_GREEN),
     box([6, 15.6, 6.5], [10, 16.6, 9.5], GOLD)]
for i in range(6):
    y = 0.4 + i * 2.6
    nodes = G_GREEN if i % 2 == 0 else G_CYAN
    s += [box([5, y, 6], [11, y + 1.6, 10], STEEL),
          box([5.3, y + 1.6, 6.3], [10.7, y + 1.85, 9.7], PANEL),
          box([7.3, y + 0.2, 10], [8.7, y + 1.4, 12.4], PANEL),
          box([3.5, y + 0.4, 7.3], [5, y + 1.2, 8.7], STEEL), box([11, y + 0.4, 7.3], [12.5, y + 1.2, 8.7], STEEL),
          box([3, y + 0.45, 7.5], [3.5, y + 1.15, 8.5], nodes), box([12.5, y + 0.45, 7.5], [13, y + 1.15, 8.5], nodes),
          box([7, y + 0.45, 5.6], [9, y + 1.15, 6], G_GREEN)]
M['sandevistan'] = s

# ---------------------------------------------------------------- Kiroshi optics
M['kiroshi_optics'] = [
    box([4, 4, 6], [12, 12, 11], PANEL),
    box([3.5, 4.5, 5], [4.5, 11.5, 10], STEEL), box([11.5, 4.5, 5], [12.5, 11.5, 10], STEEL),
    box([4.5, 11.5, 5], [11.5, 12.5, 10], STEEL), box([4.5, 3.5, 5], [11.5, 4.5, 10], STEEL),
    box([4.5, 4.5, 5], [11.5, 11.5, 5.6], CHROME),
    box([5.2, 5.2, 4.8], [10.8, 10.8, 5], RUBBER),
    box([5.8, 5.8, 4.6], [10.2, 10.2, 4.8], G_RED),
    box([6.4, 6.4, 4.4], [9.6, 9.6, 4.6], LENS),
    box([7.3, 7.3, 4.2], [8.7, 8.7, 4.4], RUBBER),
    box([8.8, 8.8, 4.15], [9.3, 9.3, 4.25], CERAMIC, glow=True),
    box([5, 12.5, 6], [11, 13, 10], CERAMIC),
    box([7, 7, 11], [9, 9, 14.2], RUBBER), box([6.6, 6.6, 14.2], [9.4, 9.4, 15.2], GOLD),
    box([3, 6.5, 6.5], [3.5, 9.5, 9.5], CARBON), box([12.5, 6.5, 6.5], [13, 9.5, 9.5], CARBON),
]

# ---------------------------------------------------------------- Monowire
mw = [box([4, 0, 5], [12, 5, 11], PANEL), box([5, 0, 4], [11, 5, 12], PANEL),
      box([3.8, 1.5, 4.8], [12.2, 2.3, 11.2], HAZARD), box([4.8, 1.5, 3.8], [11.2, 2.3, 12.2], HAZARD),
      box([3.85, 2.8, 6], [4.0, 4.5, 10], CARBON), box([12.0, 2.8, 6], [12.15, 4.5, 10], CARBON),
      box([6.5, 5, 6.5], [9.5, 6.2, 9.5], CHROME), box([6.2, 5.5, 6.2], [9.8, 5.8, 9.8], G_PINK),
      box([7.4, 6.2, 7.4], [8.6, 7, 8.6], STEEL)]
mw += path([(8, 7, 8), (8, 9, 9.5), (8, 11.5, 9.6), (8, 13.5, 8), (8.4, 14.6, 5.5), (9, 14, 3),
            (9.4, 12.5, 2), (9.6, 11, 2.4)], 0.35, G_PINK, steps=5)
mw += [box([9.0, 9.8, 1.8], [10.2, 11.0, 3.0], CHROME), box([9.2, 9.5, 2.0], [10.0, 9.8, 2.8], G_PINK)]
M['monowire'] = mw

# ---------------------------------------------------------------- Reinforced tendons
t = [box([4, 13.5, 6], [12, 15.5, 10], STEEL), box([4, 0.5, 6], [12, 2.5, 10], STEEL),
     box([4.4, 7.3, 6.4], [11.6, 8.5, 9.6], PANEL), box([7.5, 7.6, 6.1], [8.5, 8.2, 6.4], G_CYAN),
     box([7, 2.5, 9.6], [9, 8, 10.6], PANEL), box([7.5, 8, 9.8], [8.5, 13.5, 10.4], CHROME)]
for i, x in enumerate([5, 6.3, 7.6, 8.9, 10.2]):
    z = 6.8 + (i % 2) * 0.3
    t.append(box([x, 2.5, z], [x + 1.05, 13.5, z + 1.6], MUSCLE))
for x in (4.8, 10.6):
    for y in (14.9, 0.4):
        t.append(box([x, y, 5.8], [x + 0.6, y + 0.4, 6.0], GOLD))
M['reinforced_tendons'] = t

# ---------------------------------------------------------------- Gorilla arms
g = [box([5, 0, 5], [11, 4, 11], PANEL), box([4.8, 3.2, 4.8], [11.2, 4, 11.2], HAZARD),
     box([4, 2, 6], [4.6, 6, 6.6], CHROME), box([3.8, 1, 5.8], [4.8, 3, 6.8], PANEL),
     box([11.4, 2, 9.4], [12, 6, 10], CHROME), box([11.2, 1, 9.2], [12.2, 3, 10.2], PANEL),
     box([3.8, 4, 4.6], [12.2, 10, 11.4], STEEL),
     box([4, 9, 4.2], [12, 10.4, 5], PANEL),
     box([4.4, 5, 11.4], [11.6, 9.5, 11.8], CARBON),
     box([12.2, 5.5, 4.8], [13.8, 9.5, 8.5], STEEL), box([11.5, 8.5, 4], [13.5, 10, 5.5], CHROME)]
for i in range(4):
    x = 4 + i * 2.1
    g += [box([x, 10, 4.6], [x + 1.9, 13, 9.5], STEEL),
          box([x + 0.1, 10.4, 3.6], [x + 1.8, 12.8, 4.6], PANEL),
          box([x + 0.6, 11.3, 3.4], [x + 1.3, 12, 3.6], G_CYAN),
          box([x + 0.2, 13, 5.5], [x + 1.7, 13.4, 9], CHROME)]
M['gorilla_arms'] = g

# ---------------------------------------------------------------- Immunosuppressant
M['immunosuppressant'] = [
    box([6.5, 3, 6.5], [9.5, 11, 9.5], CERAMIC),
    box([6.3, 4.5, 7.3], [9.7, 9.5, 8.7], G_CYAN),
    box([6.4, 9.6, 6.4], [9.6, 10.4, 9.6], REDPL),
    box([6, 2, 6], [10, 3, 10], STEEL), box([6, 11, 6], [10, 12, 10], PANEL),
    box([4.5, 11, 7.5], [11.5, 11.6, 8.5], PANEL),
    box([7.5, 12, 7.5], [8.5, 14.5, 8.5], CHROME), box([6.8, 14.5, 6.8], [9.2, 15.3, 9.2], REDPL),
    box([7, 1.4, 7], [9, 2, 9], STEEL), box([7.8, 0, 7.8], [8.2, 1.4, 8.2], CHROME),
]

# ---------------------------------------------------------------- Ripperdoc kit
rk = [box([1, 1, 4], [15, 7.8, 12], REDPL), box([1, 8.1, 4], [15, 9.6, 12], REDPL),
      box([1.1, 7.8, 4.1], [14.9, 8.1, 11.9], PANEL),
      box([2, 0, 5], [4, 1, 11], RUBBER), box([12, 0, 5], [14, 1, 11], RUBBER),
      box([3, 6.5, 3.6], [5, 8.8, 4], STEEL), box([11, 6.5, 3.6], [13, 8.8, 4], STEEL),
      box([5, 9.6, 7.5], [6, 11, 8.5], STEEL), box([10, 9.6, 7.5], [11, 11, 8.5], STEEL),
      box([5, 11, 7.4], [11, 12, 8.6], RUBBER),
      box([7.2, 2.2, 3.7], [8.8, 6.8, 4], CERAMIC), box([5.7, 3.7, 3.7], [10.3, 5.3, 4], CERAMIC),
      box([13, 2, 3.7], [13.6, 2.6, 3.9], G_GREEN)]
for x0, x1 in ((0.7, 1.8), (14.2, 15.3)):
    for z0, z1 in ((3.7, 4.8), (11.2, 12.3)):
        rk.append(box([x0, 1, z0], [x1, 9.6, z1], STEEL))
M['ripperdoc_kit'] = rk

# ---------------------------------------------------------------- Cyber chip
c = [box([3, 0, 3], [13, 1, 13], PCB), box([6, 1, 6], [10, 1.8, 10], PANEL),
     box([6.5, 1.8, 6.5], [9.5, 2.2, 9.5], CHROME), box([7.3, 2.2, 7.3], [8.7, 2.4, 8.7], G_CYAN),
     box([4, 1, 4], [5, 1.8, 5], CHROME), box([11, 1, 11], [12, 1.8, 12], CHROME),
     box([11, 1, 4], [12, 1.5, 5.5], RUBBER)]
for i in (4.5, 6.5, 8.5, 10.5):
    c += [box([i, 0, 1.5], [i + 1, 0.6, 3], GOLD), box([i, 0, 13], [i + 1, 0.6, 14.5], GOLD),
          box([1.5, 0, i], [3, 0.6, i + 1], GOLD), box([13, 0, i], [14.5, 0.6, i + 1], GOLD)]
M['cyber_chip'] = c

# ---------------------------------------------------------------- Ripperdoc chair (block, seat faces north)
CHAIR = [
    box([5, 0, 5], [11, 2, 11], PANEL), box([4.8, 1, 4.8], [11.2, 1.5, 11.2], G_CYAN),
    box([6.5, 2, 6.5], [9.5, 6, 9.5], CHROME), box([6, 4.5, 6], [10, 5, 10], HAZARD),
    box([2, 6, 1], [14, 8, 12], STEEL), box([2.5, 8, 1.5], [13.5, 9, 11.5], RUBBER),
    box([1.9, 6.5, 1], [2, 7.5, 12], G_CYAN), box([14, 6.5, 1], [14.1, 7.5, 12], G_CYAN),
    rot(box([2.5, 8, 12], [13.5, 22, 14], STEEL), 22.5, (8, 8, 12)),
    rot(box([3, 9, 11.4], [13, 21, 12], RUBBER), 22.5, (8, 8, 12)),
    rot(box([5, 21, 12.3], [11, 24, 14.3], PANEL), 22.5, (8, 8, 12)),
    rot(box([7.5, 10, 14], [8.5, 20, 14.3], G_PINK), 22.5, (8, 8, 12)),
    box([1, 8, 3], [2.5, 10, 11], CHROME), box([13.5, 8, 3], [15, 10, 11], CHROME),
    box([1.2, 6, 9], [2.3, 8, 10], PANEL), box([13.7, 6, 9], [14.8, 8, 10], PANEL),
    box([14, 0, 13.5], [15.5, 21, 15], STEEL), box([13.8, 2, 13.3], [15.7, 3, 15.2], HAZARD),
    box([13.6, 20, 3], [15, 21.5, 15], CHROME),
    box([10, 17, 3], [15, 20, 7], PANEL), box([10.2, 17.5, 2.8], [14.8, 19.5, 3], CARBON),
    box([10.5, 16.5, 3.5], [11.5, 17, 4.5], G_PINK), box([12, 16.5, 3.5], [13, 17, 4.5], G_GREEN),
    box([13.5, 16.5, 3.5], [14.5, 17, 4.5], G_CYAN),
    box([11, 14.5, 5], [11.4, 17, 5.4], CHROME), box([13, 15, 5.5], [13.4, 17, 5.9], CHROME),
]


def display_for(elements):
    top = max(e['to'][1] for e in elements)
    gui_scale = round(0.625 * min(1.0, 16 / top), 3)
    return {
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, -25, 0], "translation": [1.5, 3, 1], "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [0, -25, 0], "translation": [1.5, 3, 1], "scale": [0.6, 0.6, 0.6]},
        "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"scale": [0.8, 0.8, 0.8]},
        "head": {"translation": [0, 13, 7], "scale": [0.8, 0.8, 0.8]},
        "gui": {"rotation": [25, 210, 0], "translation": [0, -(top - 16) * gui_scale / 2 if top > 16 else 0, 0],
                "scale": [gui_scale] * 3},
    }


def write(path, obj):
    with open(path, 'w') as f:
        json.dump(obj, f, indent=1)


if __name__ == '__main__':
    gen_materials.build(f'{ROOT}/textures/item/cyberware.png')
    os.makedirs(f'{ROOT}/models/item/3d', exist_ok=True)
    for name, elements in M.items():
        for e in elements:  # authored facing -Z; mirror so details face the viewer (+Z)
            e['from'][2], e['to'][2] = 16 - e['to'][2], 16 - e['from'][2]
            if 'rotation' in e:
                e['rotation']['origin'][2] = 16 - e['rotation']['origin'][2]
                e['rotation']['angle'] = -e['rotation']['angle']
        disp = display_for(elements)
        if name == 'cyber_chip':
            for k in ('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand'):
                disp[k]['rotation'] = [-70, 0, 0]
            disp['gui']['rotation'] = [60, 210, 0]
        write(f'{ROOT}/models/item/3d/{name}.json',
              {"textures": {"atlas": "edgerunners:item/cyberware", "particle": f"edgerunners:item/{name}"},
               "elements": elements, "display": disp})
        # 3D everywhere, including the inventory
        write(f'{ROOT}/models/item/{name}.json', {"parent": f"edgerunners:item/3d/{name}"})

    os.makedirs(f'{ROOT}/models/block', exist_ok=True)
    os.makedirs(f'{ROOT}/blockstates', exist_ok=True)
    write(f'{ROOT}/models/block/ripperdoc_chair.json',
          {"parent": "minecraft:block/block",
           "textures": {"atlas": "edgerunners:item/cyberware", "particle": "edgerunners:item/cyberware"},
           "elements": CHAIR})
    write(f'{ROOT}/models/item/ripperdoc_chair.json', {"parent": "edgerunners:block/ripperdoc_chair"})
    write(f'{ROOT}/blockstates/ripperdoc_chair.json', {"variants": {
        f"facing={d}": {"model": "edgerunners:block/ripperdoc_chair", **({"y": y} if y else {})}
        for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    print('generated', len(M), 'item models + ripperdoc chair')
