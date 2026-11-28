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


_PLANE = {'x': (1, 2), 'y': (0, 2), 'z': (0, 1)}
_AX = {'x': 0, 'y': 1, 'z': 2}


def _tex(els, tex):
    if tex:
        for e in els:
            for f in e['faces'].values(): f['texture'] = tex
    return els


def cyl16(axis, c, r, length, mat, glow=None, tex=None, sides=16):
    """Round prism along `axis` centred at c: 8 thin slabs at 22.5 degree steps (16 sides), or 4 slabs for sides=8.
    Only uses rotations block models allow (0, +-22.5, +-45)."""
    i, j = _PLANE[axis]
    k = _AX[axis]
    s = 2 * r * math.tan(math.pi / sides) * 1.08
    variants = [(False, 0), (False, 45), (True, 0), (True, 45)] if sides == 8 else \
               [(False, 0), (False, 22.5), (False, 45), (False, -22.5), (True, 0), (True, 22.5), (True, 45), (True, -22.5)]
    els = []
    for swapped, ang in variants:
        da, db = (s / 2, r) if swapped else (r, s / 2)
        fr, to = [0, 0, 0], [0, 0, 0]
        fr[k], to[k] = c[k] - length / 2, c[k] + length / 2
        fr[i], to[i] = c[i] - da, c[i] + da
        fr[j], to[j] = c[j] - db, c[j] + db
        e = box([round(v, 3) for v in fr], [round(v, 3) for v in to], mat, glow)
        if ang:
            e['rotation'] = {"origin": [round(v, 3) for v in c], "axis": axis, "angle": ang}
        els.append(e)
    return _tex(els, tex)


def rbox(fr, to, axis, rad, mat, glow=None, tex=None):
    """Box with rounded edges running along `axis` (a rounded-rectangle prism)."""
    i, j = _PLANE[axis]
    k = _AX[axis]
    rad = min(rad, (to[i] - fr[i]) / 2 - 0.01, (to[j] - fr[j]) / 2 - 0.01)
    b1 = [list(fr), list(to)]; b1[0][i] += rad; b1[1][i] -= rad
    b2 = [list(fr), list(to)]; b2[0][j] += rad; b2[1][j] -= rad
    els = [box(b1[0], b1[1], mat, glow), box(b2[0], b2[1], mat, glow)]
    for ci in (fr[i] + rad, to[i] - rad):
        for cj in (fr[j] + rad, to[j] - rad):
            c = [0, 0, 0]; c[k] = (fr[k] + to[k]) / 2; c[i] = ci; c[j] = cj
            els += cyl16(axis, c, rad, to[k] - fr[k], mat, glow, sides=8)
    return _tex(els, tex)


def cross(fr, to, axis, inset, mat, glow=None, tex=None, rotation=None):
    """Cheap bevel: two overlapping boxes (works together with a rotation, unlike rbox)."""
    i, j = _PLANE[axis]
    b1 = [list(fr), list(to)]; b1[0][i] += inset; b1[1][i] -= inset
    b2 = [list(fr), list(to)]; b2[0][j] += inset; b2[1][j] -= inset
    els = [box(b1[0], b1[1], mat, glow), box(b2[0], b2[1], mat, glow)]
    if rotation:
        for e in els: e['rotation'] = {"origin": list(rotation[0]), "axis": rotation[1], "angle": rotation[2]}
    return _tex(els, tex)


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
mb = cyl16('y', (8, 2.5, 8), 2.6, 5, PANEL) + cyl16('y', (8, 1.5, 8), 2.75, 1, HAZARD) + \
     cyl16('x', (8, 6, 8), 1.1, 4.4, CHROME) + cyl16('x', (8, 6, 8), 0.5, 4.8, STEEL) + \
     cyl16('y', (8, 7.0, 9.9), 0.45, 4.5, CHROME, sides=8) + [
    box([7.5, 7, 6], [8.5, 12, 10], STEEL),
    box([7.55, 12, 6.3], [8.45, 17, 10], STEEL),
    box([7.6, 17, 6.8], [8.4, 21, 10], CHROME),
    rot(box([7.62, 20, 7.4], [8.38, 23.5, 9.6], CHROME), 22.5, (8, 21, 8.5)),
    box([7.8, 7.5, 10], [8.2, 21, 10.25], CHROME),
    box([7.85, 8, 10.25], [8.15, 21, 10.6], G_PINK),
    rot(box([7.85, 20.5, 9.5], [8.15, 23, 9.8], G_PINK), 22.5, (8, 21, 8.5)),
    box([7.45, 7, 5.6], [8.55, 19, 6.1], PANEL),
] + cyl16('x', (8, 12, 8.3), 0.6, 1.3, G_CYAN, sides=8) + cyl16('x', (8, 17, 8.4), 0.55, 1.2, G_CYAN, sides=8)
for y in range(8, 19, 2):
    mb.append(box([7.55, y, 5.2], [8.45, y + 0.8, 5.6], STEEL))
M['mantis_blade'] = mb

# ---------------------------------------------------------------- Sandevistan
s = cyl16('y', (8, 8, 8), 0.8, 16, CHROME) + cyl16('y', (6.4, 8, 10.9), 0.45, 16, RUBBER, sides=8) + \
    cyl16('y', (9.6, 8, 10.9), 0.45, 16, RUBBER, sides=8) + cyl16('y', (8, 8, 11.8), 0.3, 16, G_GREEN, sides=8) + \
    cyl16('y', (8, 16.1, 8), 1.6, 1, GOLD)
for i in range(6):
    y = 0.4 + i * 2.6
    nodes = G_GREEN if i % 2 == 0 else G_CYAN
    s += rbox([5, y, 6], [11, y + 1.6, 10], 'y', 1.2, STEEL)
    s += [box([5.3, y + 1.6, 6.3], [10.7, y + 1.85, 9.7], PANEL)]
    s += rbox([7.3, y + 0.2, 10], [8.7, y + 1.4, 12.4], 'y', 0.5, PANEL)
    s += cyl16('x', (4.25, y + 0.8, 8), 0.45, 1.5, STEEL, sides=8) + cyl16('x', (11.75, y + 0.8, 8), 0.45, 1.5, STEEL, sides=8)
    s += cyl16('x', (3.25, y + 0.8, 8), 0.5, 0.5, nodes, sides=8) + cyl16('x', (12.75, y + 0.8, 8), 0.5, 0.5, nodes, sides=8)
    s += [box([7, y + 0.45, 5.6], [9, y + 1.15, 6], G_GREEN)]
M['sandevistan'] = s

# ---------------------------------------------------------------- Kiroshi optics (round cyber-eye)
M['kiroshi_optics'] = cyl16('z', (8, 8, 8.5), 4.0, 5, PANEL) + cyl16('z', (8, 8, 5.7), 3.7, 0.8, CHROME) + \
    cyl16('z', (8, 8, 5.2), 3.1, 0.4, RUBBER) + cyl16('z', (8, 8, 4.85), 2.4, 0.3, G_RED) + \
    cyl16('z', (8, 8, 4.6), 1.7, 0.3, LENS) + cyl16('z', (8, 8, 4.35), 0.8, 0.25, RUBBER) + \
    [box([8.8, 8.8, 4.15], [9.3, 9.3, 4.25], CERAMIC, glow=True)] + \
    cyl16('z', (8, 8, 12.6), 1.0, 3.2, RUBBER) + cyl16('z', (8, 8, 14.7), 1.4, 1.0, GOLD) + \
    cyl16('x', (3.6, 8, 8.5), 1.6, 0.6, CARBON) + cyl16('x', (12.4, 8, 8.5), 1.6, 0.6, CARBON) + \
    cyl16('z', (8, 8, 7.2), 4.2, 0.4, CERAMIC)

# ---------------------------------------------------------------- Monowire
mw = cyl16('y', (8, 2.5, 8), 4.0, 5, PANEL) + cyl16('y', (8, 1.9, 8), 4.2, 0.8, HAZARD) + \
     cyl16('y', (8, 4.0, 8), 4.1, 0.4, CARBON) + cyl16('y', (8, 5.6, 8), 1.5, 1.2, CHROME) + \
     cyl16('y', (8, 5.65, 8), 1.8, 0.3, G_PINK) + cyl16('y', (8, 6.6, 8), 0.6, 0.8, STEEL, sides=8)
mw += path([(8, 7, 8), (8, 9, 9.5), (8, 11.5, 9.6), (8, 13.5, 8), (8.4, 14.6, 5.5), (9, 14, 3),
            (9.4, 12.5, 2), (9.6, 11, 2.4)], 0.35, G_PINK, steps=5)
mw += cyl16('y', (9.6, 10.4, 2.4), 0.6, 1.2, CHROME, sides=8) + cyl16('y', (9.6, 9.65, 2.4), 0.4, 0.3, G_PINK, sides=8)
M['monowire'] = mw

# ---------------------------------------------------------------- Reinforced tendons
t = rbox([4, 13.5, 6], [12, 15.5, 10], 'y', 1.2, STEEL) + rbox([4, 0.5, 6], [12, 2.5, 10], 'y', 1.2, STEEL) + \
    rbox([4.4, 7.3, 6.4], [11.6, 8.5, 9.6], 'y', 1.0, PANEL) + [box([7.5, 7.6, 6.1], [8.5, 8.2, 6.4], G_CYAN)] + \
    cyl16('y', (8, 5.25, 10.1), 0.9, 5.5, PANEL) + cyl16('y', (8, 10.75, 10.1), 0.45, 5.5, CHROME, sides=8)
for i, x in enumerate([5.5, 6.8, 8.1, 9.4, 10.7]):
    t += cyl16('y', (x, 8, 7.6 + (i % 2) * 0.3), 0.62, 11, MUSCLE, sides=8)
for x in (5.1, 10.9):
    for y in (15.1, 0.6):
        t += cyl16('z', (x, y, 5.9), 0.3, 0.2, GOLD, sides=8)
M['reinforced_tendons'] = t

# ---------------------------------------------------------------- Gorilla arms (rounded fist)
g = cyl16('y', (8, 2, 8), 3.0, 4, PANEL) + cyl16('y', (8, 3.6, 8), 3.15, 0.8, HAZARD) + \
    cyl16('y', (4.3, 4, 6.3), 0.35, 4, CHROME, sides=8) + cyl16('y', (11.7, 4, 9.7), 0.35, 4, CHROME, sides=8) + \
    rbox([3.8, 4, 4.6], [12.2, 10, 11.4], 'y', 1.4, STEEL) + rbox([4, 9, 4.2], [12, 10.4, 5], 'y', 0.35, PANEL) + \
    [box([4.4, 5, 11.4], [11.6, 9.5, 11.8], CARBON)] + rbox([12.2, 5.5, 4.8], [13.8, 9.5, 8.5], 'y', 0.7, STEEL) + \
    cyl16('x', (12.5, 9.25, 4.75), 0.75, 2, CHROME, sides=8)
for i in range(4):
    x = 4 + i * 2.1
    g += rbox([x, 10, 4.6], [x + 1.9, 13, 9.5], 'z', 0.8, STEEL)
    g += rbox([x + 0.1, 10.4, 3.6], [x + 1.8, 12.8, 4.6], 'z', 0.7, PANEL)
    g += cyl16('z', (x + 0.95, 11.6, 3.5), 0.4, 0.2, G_CYAN, sides=8)
    g += cyl16('x', (x + 0.95, 13.2, 7.25), 0.3, 1.5, CHROME, sides=8)
M['gorilla_arms'] = g

# ---------------------------------------------------------------- Immunosuppressant (round auto-injector)
M['immunosuppressant'] = cyl16('y', (8, 7, 8), 1.5, 8, CERAMIC) + cyl16('y', (8, 7, 8), 1.58, 4.6, G_CYAN) + \
    cyl16('y', (8, 10, 8), 1.62, 0.8, REDPL) + cyl16('y', (8, 2.5, 8), 2.0, 1, STEEL) + cyl16('y', (8, 11.5, 8), 2.0, 1, PANEL) + \
    rbox([4.5, 11, 7.5], [11.5, 11.6, 8.5], 'x', 0.25, PANEL) + cyl16('y', (8, 13.25, 8), 0.5, 2.5, CHROME, sides=8) + \
    cyl16('y', (8, 14.9, 8), 1.2, 0.8, REDPL) + cyl16('y', (8, 1.7, 8), 1.0, 0.6, STEEL) + cyl16('y', (8, 0.7, 8), 0.2, 1.4, CHROME, sides=8)

# ---------------------------------------------------------------- Ripperdoc kit (rounded case)
rk = rbox([1, 1, 4], [15, 7.8, 12], 'z', 1.2, REDPL) + rbox([1, 8.1, 4], [15, 9.6, 12], 'z', 0.7, REDPL) + \
     [box([1.1, 7.8, 4.1], [14.9, 8.1, 11.9], PANEL)] + \
     rbox([2, 0, 5], [4, 1, 11], 'z', 0.4, RUBBER) + rbox([12, 0, 5], [14, 1, 11], 'z', 0.4, RUBBER) + \
     rbox([3, 6.5, 3.6], [5, 8.8, 4], 'z', 0.15, STEEL) + rbox([11, 6.5, 3.6], [13, 8.8, 4], 'z', 0.15, STEEL) + \
     cyl16('y', (5.5, 10.3, 8), 0.5, 1.4, STEEL, sides=8) + cyl16('y', (10.5, 10.3, 8), 0.5, 1.4, STEEL, sides=8) + \
     cyl16('x', (8, 11.5, 8), 0.7, 6.2, RUBBER) + \
     [box([7.2, 2.2, 3.7], [8.8, 6.8, 4], CERAMIC), box([5.7, 3.7, 3.7], [10.3, 5.3, 4], CERAMIC), box([13, 2, 3.7], [13.6, 2.6, 3.9], G_GREEN)]
M['ripperdoc_kit'] = rk

# ---------------------------------------------------------------- Cyber chip
c = [box([3, 0, 3], [13, 1, 13], PCB)] + rbox([6, 1, 6], [10, 1.8, 10], 'y', 0.6, PANEL) + \
    cyl16('y', (8, 2.0, 8), 1.6, 0.4, CHROME) + cyl16('y', (8, 2.3, 8), 0.8, 0.2, G_CYAN) + \
    cyl16('y', (4.5, 1.4, 4.5), 0.5, 0.8, CHROME, sides=8) + cyl16('y', (11.5, 1.4, 11.5), 0.5, 0.8, CHROME, sides=8) + \
    [box([11, 1, 4], [12, 1.5, 5.5], RUBBER)]
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
    # mount for the animated surgical arm (drawn by the block entity renderer)
    box([13.3, 0, 12.8], [16, 1.5, 15.7], PANEL), box([13.6, 1.5, 13.1], [15.9, 2.2, 15.4], HAZARD),
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
                if e['rotation']['axis'] != 'z':
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
