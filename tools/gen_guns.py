"""Gun item models (barrel along -Z), the micro-missile model and the projectile-launcher implant item.
Run from the repository root: python3 tools/gen_guns.py"""
import gen_models as gm
from gen_chrome import cube, cyl, group, ARMOR, GUNMETAL, WHITE, DARKCHROME, VISOR, RED_ARMOR, G_ORANGE, CIRCUIT, SERVO

ROOT = 'src/main/resources/assets/edgerunners'


def e(fr, to, mat, tex='b', glow=None, rot=None):
    el = gm.box(list(fr), list(to), mat, glow=glow if glow is not None else (tex == 'b' and mat in gm.GLOWS) or (tex == 'c' and mat in (G_ORANGE, CIRCUIT)))
    for f in el['faces'].values(): f['texture'] = '#atlas' if tex == 'b' else '#chrome'
    if rot: el['rotation'] = {"origin": list(rot[0]), "axis": rot[1], "angle": rot[2]}
    return el


B, C = '#atlas', '#chrome'
cyl16, rbox, cross = gm.cyl16, gm.rbox, gm.cross

GUNS = {
    'smart_pistol':
        rbox((7, 9.5, 3), (9, 11.3, 12.5), 'z', 0.55, gm.PANEL, tex=B) +
        rbox((7.6, 11.2, 3.2), (8.4, 11.6, 12.2), 'z', 0.18, gm.CHROME, tex=B) +
        rbox((7.1, 8, 4), (8.9, 9.6, 12), 'z', 0.45, gm.STEEL, tex=B) +
        cyl16('z', (8, 10.3, 2.4), 0.55, 1.6, gm.CHROME, tex=B) + cyl16('z', (8, 10.3, 1.55), 0.38, 0.12, gm.G_CYAN, tex=B) +
        [e((6.95, 10, 5), (7.0, 10.8, 11), gm.G_CYAN), e((9.0, 10, 5), (9.05, 10.8, 11), gm.G_CYAN)] +
        cross((7.15, 2.5, 9.5), (8.85, 8, 12), 'y', 0.35, gm.RUBBER, tex=B, rotation=((8, 8, 11), 'x', -22.5)) +
        cyl16('x', (8, 7.4, 8.3), 0.25, 1.2, DARKCHROME, tex=C, sides=8) + [e((7.4, 7, 7), (8.6, 7.3, 9.6), DARKCHROME, 'c'),
         e((7.9, 7.4, 8.2), (8.1, 8.2, 8.6), gm.CHROME)] +
        rbox((7.6, 11.4, 12), (8.4, 12, 12.6), 'z', 0.2, DARKCHROME, tex=C) + rbox((7.6, 11.4, 3.4), (8.4, 11.9, 3.9), 'z', 0.2, DARKCHROME, tex=C) +
        cyl16('z', (8, 10.4, 12.75), 0.8, 0.5, SERVO, tex=C),
    'smg':
        rbox((6.8, 8.5, 4), (9.2, 11, 13), 'z', 0.6, gm.PANEL, tex=B) +
        [e((6.75, 9.2, 5), (6.8, 10.4, 12), gm.HAZARD), e((9.2, 9.2, 5), (9.25, 10.4, 12), gm.HAZARD)] +
        cyl16('z', (8, 9.8, 2.25), 0.85, 3.5, gm.CARBON, tex=B) + cyl16('z', (8, 9.8, 0.25), 0.62, 0.5, DARKCHROME, tex=C) +
        cyl16('z', (8, 9.8, 0.0), 0.35, 0.1, gm.G_PINK, tex=B, sides=8) +
        cross((7.3, 3, 6), (8.7, 8.5, 8), 'y', 0.3, GUNMETAL, tex=C, rotation=((8, 8.5, 7), 'x', 22.5)) +
        cross((7.2, 3.5, 10), (8.8, 8.5, 12), 'y', 0.35, gm.RUBBER, tex=B, rotation=((8, 8.5, 11), 'x', -22.5)) +
        cyl16('z', (8, 9.2, 14.5), 0.4, 3, gm.STEEL, tex=B, sides=8) + rbox((7.4, 6.5, 15.2), (8.6, 9.6, 16), 'y', 0.35, gm.STEEL, tex=B) +
        rbox((7.2, 11, 7), (8.8, 12.6, 10), 'z', 0.4, DARKCHROME, tex=C) + [e((7.45, 11.35, 6.95), (8.55, 12.35, 7.0), VISOR, 'c'),
         e((7.9, 11.7, 6.9), (8.1, 11.9, 6.95), gm.G_PINK)] + cyl16('x', (8, 8.15, 8.75), 0.35, 2, DARKCHROME, tex=C, sides=8),
    'shotgun':
        cyl16('z', (8, 10.5, 6), 0.72, 12, DARKCHROME, tex=C) + cyl16('z', (8, 10.5, 0.1), 0.5, 0.25, gm.RUBBER, tex=B, sides=8) +
        cyl16('z', (8, 9.0, 5.75), 0.6, 8.5, GUNMETAL, tex=C) +
        cyl16('z', (8, 9.0, 4.5), 0.95, 4, gm.RUBBER, tex=B) +
        [x for z in (3.0, 3.8, 4.6, 5.4, 6.2) for x in cyl16('z', (8, 9.0, z), 1.0, 0.25, gm.CARBON, tex=B, sides=8)] +
        rbox((6.9, 8.2, 9), (9.1, 11.4, 13.5), 'z', 0.6, gm.PANEL, tex=B) + [e((6.85, 10.6, 9.5), (6.9, 11.0, 13), gm.HAZARD)] +
        rbox((7.2, 6.5, 13.5), (8.8, 10.5, 16), 'z', 0.5, gm.CARBON, tex=B) +
        cross((7.3, 5.0, 11), (8.7, 8.4, 13), 'y', 0.3, gm.RUBBER, tex=B, rotation=((8, 8.2, 12), 'x', -22.5)) +
        cyl16('y', (8, 11.45, 0.85), 0.3, 0.5, gm.CHROME, tex=B, sides=8) + [e((9.1, 9.0, 10), (9.15, 9.6, 12.5), gm.G_RED)] +
        rbox((7.0, 11.4, 9.5), (9.0, 11.8, 13), 'z', 0.18, DARKCHROME, tex=C),
    'tech_rifle':
        rbox((6.9, 8.8, 2), (9.1, 11.2, 15), 'z', 0.7, WHITE, tex=C) +
        [e((6.85, 9.4, 4), (6.9, 10.6, 13), CIRCUIT, 'c'), e((9.1, 9.4, 4), (9.15, 10.6, 13), CIRCUIT, 'c')] +
        cyl16('z', (8, 11.4, 4.5), 0.35, 9, DARKCHROME, tex=C, sides=8) + cyl16('z', (8, 8.6, 4.5), 0.35, 9, DARKCHROME, tex=C, sides=8) +
        cyl16('z', (8, 10.0, 1.0), 0.55, 2, gm.CHROME, tex=B) +
        [x for z in (0.85, 2.85, 4.85, 6.85) for x in cyl16('z', (8, 10.0, z), 1.75, 0.5, gm.G_CYAN, tex=B)] +
        cyl16('z', (8, 12.2, 8.5), 0.65, 5, DARKCHROME, tex=C) + cyl16('z', (8, 12.2, 5.95), 0.5, 0.1, gm.LENS, tex=B) +
        cyl16('y', (8, 11.75, 7.0), 0.25, 0.6, DARKCHROME, tex=C, sides=8) + cyl16('y', (8, 11.75, 10.0), 0.25, 0.6, DARKCHROME, tex=C, sides=8) +
        rbox((7.3, 7.0, 13), (8.7, 10.8, 16), 'z', 0.5, WHITE, tex=C) +
        cross((7.4, 4.8, 11.2), (8.6, 8.8, 13), 'y', 0.3, gm.RUBBER, tex=B, rotation=((8, 8.8, 12), 'x', -22.5)) +
        rbox((7.4, 5.8, 8.5), (8.6, 8.8, 10.5), 'y', 0.35, GUNMETAL, tex=C) + [e((8.6, 6.4, 9.0), (8.65, 8.2, 10.0), gm.G_CYAN)],
}


def gun_display(name):
    return {
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1.0, -1.5], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1.0, -1.5], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1.5, -1.0], "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1.5, -1.0], "scale": [0.6, 0.6, 0.6]},
        "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 90, 0], "scale": [0.8, 0.8, 0.8]},
        "head": {"translation": [0, 13, 7], "scale": [0.8, 0.8, 0.8]},
        "gui": {"rotation": [15, -105, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    }


def write_item(name, elements, display):
    gm.write(f'{ROOT}/models/item/3d/{name}.json', {
        "textures": {"atlas": "edgerunners:item/cyberware", "chrome": "edgerunners:item/chrome", "particle": "edgerunners:item/chrome"},
        "elements": elements, "display": display})
    gm.write(f'{ROOT}/models/item/{name}.json', {"parent": f"edgerunners:item/3d/{name}"})


# ammo box
AMMO = rbox((4, 0, 5), (12, 6, 11), 'y', 0.8, GUNMETAL, tex=C) + rbox((3.8, 4, 4.8), (12.2, 5, 11.2), 'y', 0.9, gm.HAZARD, tex=B) + \
       [x for i in range(4) for x in cyl16('y', (5.5 + i * 1.6, 7.25, 7), 0.55, 2.5, gm.GOLD, tex=B, sides=8)] + \
       [x for i in range(4) for x in cyl16('y', (5.5 + i * 1.6, 8.8, 7), 0.42, 0.6, gm.CHROME, tex=B, sides=8)]

# projectile launcher implant (item)
PLS = rbox((4, 1, 3), (12, 7, 13), 'z', 1.2, gm.PANEL, tex=B) + rbox((3.8, 2, 2.8), (12.2, 3, 13.2), 'z', 1.25, gm.HAZARD, tex=B) + \
      [x for cx in (5.7, 10.3) for cy in (8.1, 10.7) for x in cyl16('z', (cx, cy, 7), 1.15, 12, DARKCHROME, tex=C)] + \
      [x for cx in (5.7, 10.3) for cy in (8.1, 10.7) for x in cyl16('z', (cx, cy, 0.95), 0.6, 0.1, gm.G_RED, tex=B, sides=8)] + \
      cyl16('z', (8, 9.4, 8), 1.5, 6, SERVO, tex=C)

# micro-missile runtime model (entity space: Y up, nose toward +Z)
MISSILE = {"parts": {"root": [group((0, 0, 0), [
    cyl((-0.8, -0.8, -3), (0.8, 0.8, 3), 'z', WHITE), cyl((-0.85, -0.85, 0.5), (0.85, 0.85, 1.2), 'z', RED_ARMOR),
    cyl((-0.65, -0.65, 3), (0.65, 0.65, 4.0), 'z', RED_ARMOR), cyl((-0.4, -0.4, 4.0), (0.4, 0.4, 4.6), 'z', RED_ARMOR),
    cyl((-0.18, -0.18, 4.6), (0.18, 0.18, 5.0), 'z', DARKCHROME),
    cube((-2.0, -0.1, -3), (2.0, 0.1, -1.2), GUNMETAL), cube((-0.1, -2.0, -3), (0.1, 2.0, -1.2), GUNMETAL),
    cyl((-0.6, -0.6, -3.5), (0.6, 0.6, -3.0), 'z', G_ORANGE, glow='steady'),
])]}}

if __name__ == '__main__':
    for name, els in GUNS.items():
        write_item(name, els, gun_display(name))
    d = gm.display_for(AMMO); write_item('ammo', AMMO, d)
    d = gm.display_for(PLS); write_item('projectile_launcher', PLS, d)
    import os
    os.makedirs(f'{ROOT}/machine', exist_ok=True)
    gm.write(f'{ROOT}/machine/missile.json', MISSILE)
    print('guns:', ', '.join(GUNS))
