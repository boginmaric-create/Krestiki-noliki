"""Vehicle models (same group/cube/animation format as the chrome sets, single part "root").

Vehicle space: pixels, Y up, origin at ground centre, the front points to +Z.
Signals: wheel (wheel spin, degrees), steer (degrees), plus glow modes throttle / brake / boost / pulse / steady.
Cubes with "paint": true are tinted with the vehicle's dye colour.
Run from the repository root: python3 tools/gen_vehicles.py"""
import math, os
import gen_models as gm
from gen_chrome import cube, cyl, group, rot_anim, ARMOR, GUNMETAL, DARKCHROME, SERVO, VENT, VISOR, CIRCUIT, G_ORANGE, G_ICE, CABLE

ROOT = 'src/main/resources/assets/edgerunners'


def paint(fr, to):
    c = cube(fr, to, gm.CERAMIC, 'b')
    c['paint'] = True
    return c


def mirror(cubes):
    out = []
    for c in cubes:
        m = dict(c)
        m['from'] = [-c['to'][0], c['from'][1], c['from'][2]]
        m['to'] = [-c['from'][0], c['to'][1], c['to'][2]]
        out.append(m)
    return out


def ring(cx, cy, cz, r_out, thick, width, mat, atlas, segs=16, glow=None):
    """Circle in the YZ plane (an axle along X) made of tangent segments."""
    groups = []
    seg_len = 2 * r_out * math.tan(math.pi / segs) + 0.15
    for k in range(segs):
        ang = 360.0 * k / segs
        c = cube((cx - width / 2, cy + r_out - thick, cz - seg_len / 2), (cx + width / 2, cy + r_out, cz + seg_len / 2), mat, atlas, glow=glow)
        groups.append(group((cx, cy, cz), [c], rot=[ang, 0, 0]))
    return groups


def disc(cx, cy, cz, r, width, mat, atlas='b'):
    s = r * 1.55
    return [group((cx, cy, cz), [cube((cx - width / 2, cy - s / 2, cz - s / 2), (cx + width / 2, cy + s / 2, cz + s / 2), mat, atlas)], rot=[a, 0, 0])
            for a in (0, 30, 60)]


def car_wheel(x, y, z, r=5.5, w=4.0, front=False):
    outer = 1 if x > 0 else -1
    spin = group((x, y, z), [], anims=[rot_anim("x", "wheel", amp=1)], children=
                 ring(x, y, z, r, 1.9, w, gm.RUBBER, 'b') +
                 ring(x, y, z, r - 1.9, 0.5, w * 0.6, gm.G_CYAN, 'b', glow='pulse') +
                 disc(x, y, z, r - 2.2, w * 0.5, DARKCHROME, 'c') +
                 [group((x, y, z), [cube((x + outer * w * 0.25 - 0.3, y - (r - 2.3), z - 0.5), (x + outer * w * 0.25 + 0.3, y + (r - 2.3), z + 0.5), gm.CHROME, 'b')], rot=[a, 0, 0])
                  for a in (0, 60, 120)] +
                 [group((x, y, z), [cube((x + outer * w * 0.3 - 0.4, y - 1, z - 1), (x + outer * w * 0.3 + 0.4, y + 1, z + 1), gm.GOLD, 'b')])])
    caliper = cube((x - outer * 0.2 - 0.6, y + 1.0, z - 2.2), (x - outer * 0.2 + 0.6, y + 3.6, z + 0.2), gm.REDPL, 'b')
    if front:
        return group((x, y, z), [caliper], anims=[rot_anim("y", "steer", amp=1)], children=[spin])
    return group((x, y, z), [caliper], children=[spin])


# ================================================================ Neon speedster (car)
half = [
    # chassis & sills
    cube((0, 2.5, -30), (13, 4, 30), gm.CARBON, 'b'),
    paint((13, 3, -13.5), (15, 8.5, 13.5)),
    cube((13.5, 2.1, -24), (14.5, 2.5, 24), gm.G_CYAN, 'b', glow='pulse'),
    # nose
    paint((0, 4, 26), (13, 8, 32)),
    cube((0, 2.5, 29), (14, 3.5, 33.2), gm.CARBON, 'b'),
    cube((5, 7.0, 31.95), (12, 7.8, 32.35), G_ICE, glow='steady'),
    cube((10, 5.2, 32.0), (12.5, 6.6, 32.3), G_ICE, glow='steady'),
    cube((0, 4.5, 32.0), (4, 6.5, 32.2), gm.PANEL, 'b'),
    # front wheel bay
    paint((0, 4, 14), (10, 9, 26)),
    paint((9.5, 11, 13.6), (15, 12.4, 26.4)),
    paint((14, 7.8, 13.6), (15.2, 11.2, 15.2)), paint((14, 7.8, 24.8), (15.2, 11.2, 26.4)),
    cube((9.9, 4, 14), (10, 11, 26), gm.RUBBER, 'b'),
    # cabin
    paint((0, 4, -14), (15, 9, 14)),
    cube((15.0, 5, -12), (15.1, 8, -6), VENT, glow='throttle'),
    cube((15.05, 8.5, -12), (15.15, 8.8, 12), gm.G_PINK, 'b', glow='pulse'),
    # rear wheel bay
    paint((0, 4, -26), (10, 9, -14)),
    paint((9.5, 11, -26.4), (15, 12.6, -13.6)),
    paint((14, 7.8, -26.4), (15.2, 11.2, -24.8)), paint((14, 7.8, -15.2), (15.2, 11.2, -13.6)),
    cube((9.9, 4, -26), (10, 11, -14), gm.RUBBER, 'b'),
    # tail
    paint((0, 4, -32), (14, 9, -26)),
    cube((0, 7, -32.3), (13, 8, -32.0), gm.G_RED, 'b', glow='brake'),
    cube((0, 2.5, -33), (12, 4.5, -30), gm.CARBON, 'b'),
    cyl((1, 4, -32.8), (3, 5.8, -31.8), 'z', DARKCHROME), cyl((1.4, 4.4, -32.9), (2.6, 5.4, -32.8), 'z', G_ORANGE, glow='throttle'),
    # rear deck, engine vents, headrest fairings
    paint((0, 9, -30), (13, 10.5, -11)),
    cube((0, 10.5, -26), (8, 10.7, -16), VENT, glow='throttle'),
    paint((3, 10.5, -12), (8, 14, -10)),
    # dashboard
    cube((0, 9, 2), (12, 11, 6), gm.PANEL, 'b'),
    # seats
    cube((2, 9, -8), (9, 11, -1), gm.RUBBER, 'b'),
    # spoiler
    cyl((7.9, 10.5, -29.1), (9.1, 15, -27.9), 'y', GUNMETAL),
    cube((0, 15, -31), (14, 15.8, -26), gm.CARBON, 'b'),
    paint((14, 13.5, -31.5), (14.4, 16.5, -25.5)),
    cube((0, 15.8, -31.1), (14, 16.0, -30.8), gm.G_PINK, 'b', glow='pulse'),
    # mirrors
    paint((15, 10, 8), (16.5, 11.5, 9.5)), cube((14.5, 9.5, 8.4), (15.2, 10.2, 9.0), GUNMETAL),
]
car_cubes = half + mirror(half)
car_groups = [
    group((0, 0, 0), car_cubes),
    # sloped hood
    group((0, 9, 14), [paint((-13, 8.2, 14), (13, 9.6, 30)),
                       cube((-2, 9.6, 18), (2, 9.75, 28), gm.CARBON, 'b')], rot=[7, 0, 0]),
    # windscreen + pillars
    group((0, 9.5, 12), [cube((-11, 9.5, 4), (11, 10.1, 12), VISOR),
                         cube((-11.6, 9.4, 4), (-11, 10.3, 12), gm.CHROME, 'b'), cube((11, 9.4, 4), (11.6, 10.3, 12), gm.CHROME, 'b'),
                         cube((-11.6, 9.4, 3.6), (11.6, 10.3, 4.2), gm.CHROME, 'b')], rot=[35, 0, 0]),
    # dashboard screen
    group((0, 0, 0), [cube((-3, 10.95, 2.5), (3, 11.05, 5.5), CIRCUIT, glow='pulse')]),
    # seat backs
    group((5.5, 10, -8), [cube((2, 10, -10), (9, 17, -8), gm.RUBBER, 'b'), cube((4.5, 15, -10.2), (6.5, 16, -10), gm.G_PINK, 'b', glow='pulse')], rot=[-12, 0, 0]),
    group((-5.5, 10, -8), [cube((-9, 10, -10), (-2, 17, -8), gm.RUBBER, 'b'), cube((-6.5, 15, -10.2), (-4.5, 16, -10), gm.G_PINK, 'b', glow='pulse')], rot=[-12, 0, 0]),
    # steering wheel (driver on +X)
    group((5.5, 12.5, 4.0), [cyl((3.4, 10.4, 3.7), (7.6, 14.6, 4.3), 'z', DARKCHROME), cube((5.0, 12.2, 3.6), (6.0, 12.8, 3.7), G_ICE, glow='pulse')],
          anims=[rot_anim("z", "steer", amp=-4)], rot=[-25, 0, 0]),
    car_wheel(12.5, 5.5, 20, front=True), car_wheel(-12.5, 5.5, 20, front=True),
    car_wheel(12.5, 5.5, -20), car_wheel(-12.5, 5.5, -20),
]

# ================================================================ Cyber bike
def bike_wheel(z):
    y, r, w = 6.0, 6.0, 3.4
    return group((0, y, z), [], anims=[rot_anim("x", "wheel", amp=1)], children=
                 ring(0, y, z, r, 1.6, w, gm.RUBBER, 'b') +
                 ring(0, y, z, r - 1.6, 0.6, w * 0.8, DARKCHROME, 'c') +
                 ring(0, y, z, r - 2.3, 0.45, w * 0.55, gm.G_PINK, 'b', segs=16, glow='pulse'))

bike_half = [
    cube((0, 4, -5), (3.5, 9, 5), DARKCHROME),
    cube((3.5, 5, -2), (3.6, 8, 2), gm.G_CYAN, 'b', glow='throttle'),
    cube((3.5, 4.5, -4.5), (3.7, 8.5, -2.5), SERVO),
    cube((0, 5, -13), (1.0, 7, -4), GUNMETAL),
    paint((0, 8, -10), (3, 12, 10)),
    cyl((1.9, 4.5, -15), (3.5, 6.3, -6), 'z', gm.CHROME, 'b'),
    cyl((2.15, 4.75, -15.3), (3.25, 6.05, -15), 'z', G_ORANGE, glow='throttle'),
    cube((0, 2.6, -8), (1.6, 3.0, 8), gm.G_CYAN, 'b', glow='pulse'),
]
bike_groups = [
    group((0, 0, 0), bike_half + mirror(bike_half) + [
        cube((-2.5, 12, -10), (2.5, 13.2, 0), gm.RUBBER, 'b'),
        cube((-0.4, 13.2, -9), (0.4, 13.3, -1), gm.G_PINK, 'b', glow='pulse'),
    ]),
    group((0, 11.5, 0), [paint((-3.5, 11, 0), (3.5, 13.5, 9)), cube((-0.8, 13.5, 1), (0.8, 13.6, 8), gm.CARBON, 'b')], rot=[-6, 0, 0]),
    group((0, 11, 10), [paint((-2.6, 9, 10), (2.6, 13, 17)),
                        cube((-1.5, 10.6, 17), (1.5, 11.8, 17.4), G_ICE, glow='steady'),
                        cube((-2.7, 9.2, 13), (-2.6, 10, 16.5), gm.G_PINK, 'b', glow='pulse'),
                        cube((2.6, 9.2, 13), (2.7, 10, 16.5), gm.G_PINK, 'b', glow='pulse')], rot=[-18, 0, 0]),
    group((0, 13, 10), [cube((-2.2, 13, 10), (2.2, 13.4, 15), VISOR)], rot=[-40, 0, 0]),
    group((0, 11.5, -10), [paint((-2.5, 10, -18), (2.5, 13, -10)),
                           cube((-2, 11.5, -18.3), (2, 12.5, -18), gm.G_RED, 'b', glow='brake')], rot=[10, 0, 0]),
    # steering: fork, bars and front wheel turn together
    group((0, 12.5, 11), [
        cyl((-5, 13.45, 9.2), (5, 14.25, 10.0), 'x', GUNMETAL),
        cyl((-5.8, 13.2, 9.0), (-4.2, 14.5, 10.2), 'x', gm.RUBBER, 'b'), cyl((4.2, 13.2, 9.0), (5.8, 14.5, 10.2), 'x', gm.RUBBER, 'b'),
        cube((-1.2, 14.2, 9.4), (1.2, 14.4, 10.6), CIRCUIT, glow='pulse'),
    ], anims=[rot_anim("y", "steer", amp=1)], children=[
        group((0, 13, 10.5), [cyl((-2.5, 5, 10.0), (-1.7, 13.2, 11.0), 'y', gm.CHROME, 'b'), cyl((1.7, 5, 10.0), (2.5, 13.2, 11.0), 'y', gm.CHROME, 'b')], rot=[22, 0, 0]),
        bike_wheel(13.5),
    ]),
    bike_wheel(-13.5),
]

VEHICLES = {'speedster': car_groups, 'cyber_bike': bike_groups}


def item_model(vid, groups):
    """Small static 3D model for the inventory, built from the top-level boxes (wheels as simple blocks)."""
    cubes = [c for g in groups if not g.get('rot') and not g.get('anims') for c in g['cubes']]
    # simple octagonal wheels (block models can't hold the rotated rim segments)
    def find_wheels(gs, out):
        for g in gs:
            if any(a['signal'] == 'wheel' for a in g.get('anims', [])): out.append(g['pivot'])
            find_wheels(g.get('children', []), out)
        return out
    r, w = (5.5, 4.0) if vid == 'speedster' else (6.0, 3.4)
    for (x, y, z) in find_wheels(groups, []):
        for (ry, rz) in ((0.92, 0.4), (0.4, 0.92), (0.72, 0.72)):
            cubes.append(cube((x - w / 2, y - r * ry, z - r * rz), (x + w / 2, y + r * ry, z + r * rz), gm.RUBBER, 'b'))
        cubes.append(cube((x - w / 2 - 0.05, y - r * 0.45, z - r * 0.45), (x + w / 2 + 0.05, y + r * 0.45, z + r * 0.45), gm.CHROME, 'b'))
    lo = [min(c['from'][i] for c in cubes) for i in range(3)]
    hi = [max(c['to'][i] for c in cubes) for i in range(3)]
    s = 15.0 / max(hi[i] - lo[i] for i in range(3))
    ctr = [(lo[i] + hi[i]) / 2 for i in range(3)]
    els = []
    for c in cubes:
        fr = [round(8 + (c['from'][i] - ctr[i]) * s, 3) for i in range(3)]
        to = [round(8 + (c['to'][i] - ctr[i]) * s, 3) for i in range(3)]
        e = gm.box(fr, to, c['mat'], glow=bool(c.get('glow')))
        tex = '#chrome' if c['atlas'] == 'c' else '#atlas'
        for f in e['faces'].values():
            f['texture'] = tex
            if c.get('paint'): f['tintindex'] = 0
        els.append(e)
    disp = gm.display_for(els)
    disp['gui'] = {"rotation": [25, 135, 0], "scale": [0.85, 0.85, 0.85]}
    gm.write(f'{ROOT}/models/item/3d/{vid}.json', {
        "textures": {"atlas": "edgerunners:item/cyberware", "chrome": "edgerunners:item/chrome", "particle": "edgerunners:item/cyberware"},
        "elements": els, "display": disp})
    gm.write(f'{ROOT}/models/item/{vid}.json', {"parent": f"edgerunners:item/3d/{vid}"})


if __name__ == '__main__':
    os.makedirs(f'{ROOT}/vehicle', exist_ok=True)
    for vid, groups in VEHICLES.items():
        gm.write(f'{ROOT}/vehicle/{vid}.json', {"parts": {"root": groups}})
        item_model(vid, groups)
        n = 0
        def count(gs):
            global n
            for g in gs:
                n += len(g['cubes']); count(g.get('children', []))
        count(groups)
        print(vid, 'cubes:', n)
