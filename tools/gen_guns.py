"""Gun item models (barrel along -Z), the micro-missile model and the projectile-launcher implant item.
Run from the repository root: python3 tools/gen_guns.py"""
import gen_models as gm
from gen_chrome import cube, group, ARMOR, GUNMETAL, WHITE, DARKCHROME, VISOR, RED_ARMOR, G_ORANGE, CIRCUIT, SERVO

ROOT = 'src/main/resources/assets/edgerunners'


def e(fr, to, mat, tex='b', glow=None, rot=None):
    el = gm.box(list(fr), list(to), mat, glow=glow if glow is not None else (tex == 'b' and mat in gm.GLOWS) or (tex == 'c' and mat in (G_ORANGE, CIRCUIT)))
    for f in el['faces'].values(): f['texture'] = '#atlas' if tex == 'b' else '#chrome'
    if rot: el['rotation'] = {"origin": list(rot[0]), "axis": rot[1], "angle": rot[2]}
    return el


GUNS = {
    'smart_pistol': [
        e((7, 9.5, 3), (9, 11.3, 12.5), gm.PANEL), e((7.6, 11.3, 3.2), (8.4, 11.6, 12.2), gm.CHROME),
        e((7.1, 8, 4), (8.9, 9.5, 12), gm.STEEL), e((7.5, 9.8, 2), (8.5, 10.8, 3), gm.CHROME),
        e((6.95, 10, 5), (7.0, 10.8, 11), gm.G_CYAN), e((9.0, 10, 5), (9.05, 10.8, 11), gm.G_CYAN),
        e((7.6, 10.05, 1.95), (8.4, 10.55, 2.0), gm.G_CYAN),
        e((7.15, 2.5, 9.5), (8.85, 8, 12), gm.RUBBER, rot=((8, 8, 11), 'x', -22.5)),
        e((7.4, 7, 7), (8.6, 7.4, 9.6), DARKCHROME, 'c'), e((7.9, 7.4, 8.2), (8.1, 8.2, 8.6), gm.CHROME),
        e((7.6, 11.3, 12), (8.4, 12, 12.6), DARKCHROME, 'c'), e((7.6, 11.3, 3.4), (8.4, 11.9, 3.9), DARKCHROME, 'c'),
        e((7.2, 9.6, 12.5), (8.8, 11.2, 13.0), SERVO, 'c'),
    ],
    'smg': [
        e((6.8, 8.5, 4), (9.2, 11, 13), gm.PANEL), e((6.75, 9.2, 5), (6.8, 10.4, 12), gm.HAZARD), e((9.2, 9.2, 5), (9.25, 10.4, 12), gm.HAZARD),
        e((7.2, 9, 0.5), (8.8, 10.6, 4), gm.CARBON), e((7.4, 9.2, 0), (8.6, 10.4, 0.5), DARKCHROME, 'c'),
        e((7.3, 3, 6), (8.7, 8.5, 8), GUNMETAL, 'c', rot=((8, 8.5, 7), 'x', 22.5)),
        e((7.2, 3.5, 10), (8.8, 8.5, 12), gm.RUBBER, rot=((8, 8.5, 11), 'x', -22.5)),
        e((7.4, 8.8, 13), (8.6, 9.6, 16), gm.STEEL), e((7.4, 6.5, 15.2), (8.6, 9.6, 16), gm.STEEL),
        e((7.2, 11, 7), (8.8, 12.6, 10), DARKCHROME, 'c'), e((7.4, 11.3, 7.0), (8.6, 12.4, 7.05), VISOR, 'c'),
        e((7.9, 11.7, 7.6), (8.1, 11.9, 7.65), gm.G_PINK), e((7.0, 7.8, 8.5), (9.0, 8.5, 9.0), DARKCHROME, 'c'),
    ],
    'shotgun': [
        e((7.3, 9.8, 0), (8.7, 11.2, 12), DARKCHROME, 'c'), e((7.4, 8.4, 1.5), (8.6, 9.6, 10), GUNMETAL, 'c'),
        e((7.1, 8.2, 2.5), (8.9, 9.8, 6.5), gm.RUBBER), e((7.05, 8.6, 3), (8.95, 8.9, 6), gm.CARBON),
        e((6.9, 8.2, 9), (9.1, 11.4, 13.5), gm.PANEL), e((6.85, 10.6, 9.5), (6.9, 11.0, 13), gm.HAZARD),
        e((7.2, 6.5, 13.5), (8.8, 10.5, 16), gm.CARBON), e((7.3, 5.0, 11), (8.7, 8.4, 13), gm.RUBBER, rot=((8, 8.2, 12), 'x', -22.5)),
        e((7.6, 11.2, 0.5), (8.4, 11.7, 1.2), gm.CHROME), e((9.1, 9.0, 10), (9.15, 9.6, 12.5), gm.G_RED),
        e((7.0, 11.4, 9.5), (9.0, 11.8, 13), DARKCHROME, 'c'),
    ],
    'tech_rifle': [
        e((6.9, 8.8, 2), (9.1, 11.2, 15), WHITE, 'c'), e((6.85, 9.4, 4), (6.9, 10.6, 13), CIRCUIT, 'c'),
        e((9.1, 9.4, 4), (9.15, 10.6, 13), CIRCUIT, 'c'),
        e((7.0, 11.2, 0), (9.0, 11.6, 9), DARKCHROME, 'c'), e((7.0, 8.4, 0), (9.0, 8.8, 9), DARKCHROME, 'c'),
        e((7.5, 9.5, 0), (8.5, 10.5, 2), gm.CHROME),
        *[e((6.75, 8.3, z), (9.25, 11.7, z + 0.5), gm.G_CYAN) for z in (0.6, 2.6, 4.6, 6.6)],
        e((7.3, 11.6, 6), (8.7, 12.8, 11), DARKCHROME, 'c'), e((7.45, 11.75, 5.95), (8.55, 12.65, 6.0), gm.LENS),
        e((7.3, 7.0, 13), (8.7, 10.8, 16), WHITE, 'c'), e((7.4, 4.8, 11.2), (8.6, 8.8, 13), gm.RUBBER, rot=((8, 8.8, 12), 'x', -22.5)),
        e((7.4, 5.8, 8.5), (8.6, 8.8, 10.5), GUNMETAL, 'c'), e((8.6, 6.4, 9.0), (8.65, 8.2, 10.0), gm.G_CYAN),
    ],
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
AMMO = [e((4, 0, 5), (12, 6, 11), GUNMETAL, 'c'), e((3.8, 4, 4.8), (12.2, 5, 11.2), gm.HAZARD),
        *[e((5 + i * 1.6, 6, 6.5), (6 + i * 1.6, 8.5, 7.5), gm.GOLD) for i in range(4)],
        *[e((5 + i * 1.6, 8.5, 6.6), (6 + i * 1.6, 9.2, 7.4), gm.CHROME) for i in range(4)]]

# projectile launcher implant (item)
PLS = [e((4, 1, 3), (12, 7, 13), gm.PANEL), e((3.8, 2, 2.8), (12.2, 3, 13.2), gm.HAZARD),
       *[e((x, y, 1), (x + 2.2, y + 2.2, 13), DARKCHROME, 'c') for x in (4.6, 9.2) for y in (7, 9.6)],
       *[e((x + 0.5, y + 0.5, 0.9), (x + 1.7, y + 1.7, 1.0), gm.G_RED) for x in (4.6, 9.2) for y in (7, 9.6)],
       e((7, 7, 5), (9, 12, 11), SERVO, 'c')]

# micro-missile runtime model (entity space: Y up, nose toward +Z)
MISSILE = {"parts": {"root": [group((0, 0, 0), [
    cube((-0.8, -0.8, -3), (0.8, 0.8, 3), WHITE), cube((-0.85, -0.85, 0.5), (0.85, 0.85, 1.2), RED_ARMOR),
    cube((-0.6, -0.6, 3), (0.6, 0.6, 4.2), RED_ARMOR), cube((-0.3, -0.3, 4.2), (0.3, 0.3, 4.8), DARKCHROME),
    cube((-2.0, -0.1, -3), (2.0, 0.1, -1.2), GUNMETAL), cube((-0.1, -2.0, -3), (0.1, 2.0, -1.2), GUNMETAL),
    cube((-0.55, -0.55, -3.5), (0.55, 0.55, -3.0), G_ORANGE, glow='steady'),
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
