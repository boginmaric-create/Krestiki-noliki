"""External chrome sets: animated cyberware drawn on the player model.

Writes, for every set:
  assets/edgerunners/chrome/<id>.json      runtime model (groups, cubes, animations) read by ChromeRenderer
  assets/edgerunners/models/item/...       a 3D item model built from one body part of the set

Coordinates are Minecraft player-model pixels relative to each body part (Y points DOWN, the face is -Z):
  head (-4,-8,-4)..(4,0,4)  body (-4,0,-2)..(4,12,2)  right_arm (-3,-2,-2)..(1,10,2)
  left_arm (-1,-2,-2)..(3,10,2)  legs (-2,0,-2)..(2,12,2)

Animation signals (computed per player on the client):
  age (ticks), deploy (0..1, out in combat), sprint (0..1), swing (attack arc 0..1..0),
  sway (walk cycle -1..1 scaled by walk speed), breath (slow sine)
Glow modes scale emissive brightness: pulse, breath, heat (sprint), deploy, flicker, steady
Run from the repository root: python3 tools/gen_chrome.py"""
import json, os, copy
import gen_models as gm

ROOT = 'src/main/resources/assets/edgerunners'

# chrome atlas materials ('c')
ARMOR, GUNMETAL, TEAL, PURPLE, WHITE, COPPER, G_ORANGE, G_PURPLE, G_ICE, VENT, CABLE, RED_ARMOR, SERVO, DARKCHROME, CIRCUIT, VISOR = range(16)
C_GLOWS = {G_ORANGE, G_PURPLE, G_ICE, VENT, CIRCUIT}
# base atlas materials ('b')
B = gm


def cube(fr, to, mat, atlas='c', glow=None, cyl=None):
    """A textured box; with cyl='x'|'y'|'z' it renders as a 16-sided cylinder inscribed in the box."""
    if glow is None:
        glow = 'steady' if (atlas == 'c' and mat in C_GLOWS) or (atlas == 'b' and mat in gm.GLOWS) else None
    c = {"from": list(fr), "to": list(to), "mat": mat, "atlas": atlas}
    if glow:
        c["glow"] = glow
    if cyl:
        c["cyl"] = cyl
    return c


def cyl(fr, to, axis, mat, atlas='c', glow=None):
    return cube(fr, to, mat, atlas, glow=glow, cyl=axis)


def group(pivot, cubes, anims=None, children=None, rot=None):
    g = {"pivot": list(pivot), "cubes": cubes}
    if rot: g["rot"] = list(rot)
    if anims: g["anims"] = anims
    if children: g["children"] = children
    return g


def rot_anim(axis, signal, amp=0.0, speed=0.0, phase=0.0, frm=None, to=None):
    a = {"type": "rot", "axis": axis, "signal": signal, "amp": amp}
    if speed: a["speed"] = speed
    if phase: a["phase"] = phase
    if frm is not None: a["from"] = frm; a["to"] = to
    return a


def move_anim(axis, signal, amp, speed=0.0, phase=0.0):
    a = {"type": "move", "axis": axis, "signal": signal, "amp": amp}
    if speed: a["speed"] = speed
    if phase: a["phase"] = phase
    return a


def mirror_x(groups):
    """Right arm -> left arm: x -> -x (part spaces of the two arms are mirror images)."""
    out = copy.deepcopy(groups)

    def m(g):
        g["pivot"][0] = -g["pivot"][0]
        for c in g["cubes"]:
            c["from"][0], c["to"][0] = -c["to"][0], -c["from"][0]
        for a in g.get("anims", []):
            if a["axis"] in ("y", "z"):
                a["amp"] = -a["amp"]
                if "from" in a: a["from"], a["to"] = -a["from"], -a["to"]
        if "rot" in g:
            g["rot"][1], g["rot"][2] = -g["rot"][1], -g["rot"][2]
        for ch in g.get("children", []): m(ch)
    for g in out: m(g)
    return out


SETS = {}

# ================================================================ Street samurai
blade = group((-4.2, 9.0, 0), [
    cyl((-4.6, 8.2, -0.8), (-3.8, 9.8, 0.8), 'x', SERVO),
    cube((-4.45, 4.4, -0.95), (-4.15, 8.6, 0.8), gm.STEEL, 'b'),
    cube((-4.43, 1.7, -0.75), (-4.17, 4.4, 0.6), gm.STEEL, 'b'),
    cube((-4.4, 0.2, -0.45), (-4.2, 1.7, 0.3), gm.CHROME, 'b'),
    cube((-4.38, 0.5, -1.1), (-4.22, 8.4, -0.95), G_ORANGE, glow='deploy'),
    cube((-4.5, 5.0, 0.8), (-4.1, 8.0, 1.05), GUNMETAL),
], anims=[rot_anim("x", "deploy", frm=0, to=165)])
samurai_arm = [group((-1, 4, 0), [
    cube((-3.4, 4.2, -2.4), (1.4, 9.6, 2.4), ARMOR),
    cube((-3.45, 5.0, -2.45), (1.45, 5.4, 2.45), G_ORANGE, glow='pulse'),
    cube((-3.3, 9.6, -2.3), (1.3, 10.3, 2.3), GUNMETAL),
    cube((-4.0, 4.5, -1.2), (-3.4, 9.4, 1.2), DARKCHROME),
    cube((-4.05, 5.0, -0.2), (-4.0, 9.0, 0.2), G_ORANGE, glow='deploy'),
    cube((-3.6, -2.4, -2.5), (1.6, 0.2, 2.5), ARMOR),
    cube((-3.7, -0.2, -2.6), (1.7, 0.3, 2.6), GUNMETAL),
], children=[blade])]
samurai_body = [group((0, 0, 0), [c for i in range(4) for c in (
    cube((-1.6, 1 + i * 2.6, 2.0), (1.6, 2.8 + i * 2.6, 2.9), ARMOR),
    cube((-0.5, 2.8 + i * 2.6, 2.2), (0.5, 3.6 + i * 2.6, 2.6), G_ORANGE, glow='pulse'))] + [
    cube((-4.2, 0.2, -2.3), (4.2, 1.2, 2.3), GUNMETAL)])]
samurai_head = [group((0, 0, 0), [
    cube((-1.6, -3.0, 4.0), (1.6, -0.4, 4.6), GUNMETAL),
    cube((-1.1, -2.5, 4.6), (-0.4, -1.8, 4.7), G_ORANGE, glow='pulse'),
    cube((0.4, -2.5, 4.6), (1.1, -1.8, 4.7), G_ORANGE, glow='pulse'),
    cube((-4.05, -4.2, -3.2), (-4.0, -3.8, -0.8), G_ORANGE, glow='steady'),
])]
SETS['street_samurai'] = {"item_part": "right_arm", "load": 25, "parts": {
    "right_arm": samurai_arm, "left_arm": mirror_x(samurai_arm), "body": samurai_body, "head": samurai_head}}

# ================================================================ Netrunner
def cable_chain(x, n=4):
    child = None
    for k in reversed(range(n)):
        y0 = -1.5 + k * 3.0
        cubes = [cyl((x - 0.45, y0, 4.05 + k * 0.15), (x + 0.45, y0 + 3.1, 4.95 + k * 0.15), 'y', CABLE)]
        if k == n - 1:
            cubes.append(cyl((x - 0.6, y0 + 2.6, 3.9 + k * 0.15), (x + 0.6, y0 + 3.4, 5.1 + k * 0.15), 'y', gm.GOLD, 'b'))
        if k == 0:
            cubes.append(cyl((x - 0.65, y0 - 0.4, 3.85), (x + 0.65, y0 + 0.4, 4.95), 'y', DARKCHROME))
        g = group((x, y0, 4.5 + k * 0.15), cubes,
                  anims=[rot_anim("x", "sway", amp=6 + 5 * k, phase=k * 0.7),
                         rot_anim("x", "breath", amp=2 + k, phase=k * 0.5),
                         rot_anim("z", "sway", amp=2 + k, phase=1.3 + k * 0.4)],
                  children=[child] if child else None)
        child = g
    return child

net_head = [group((0, 0, 0), [
    cube((-4.35, -6.0, -4.35), (4.35, -5.3, -4.0), GUNMETAL),
    cube((-4.35, -6.0, -4.0), (-4.0, -5.3, 4.35), GUNMETAL), cube((4.0, -6.0, -4.0), (4.35, -5.3, 4.35), GUNMETAL),
    cube((-4.0, -6.0, 4.0), (4.0, -5.3, 4.35), GUNMETAL),
    cube((-3.0, -5.85, -4.42), (3.0, -5.45, -4.35), G_PURPLE, glow='pulse'),
    cyl((-4.7, -4.3, -1.6), (-4.0, -2.1, 0.6), 'x', DARKCHROME), cyl((4.0, -4.3, -1.6), (4.7, -2.1, 0.6), 'x', DARKCHROME),
    cube((-4.66, -3.6, -1.0), (-4.6, -2.8, 0.0), G_PURPLE, glow='pulse'), cube((4.6, -3.6, -1.0), (4.66, -2.8, 0.0), G_PURPLE, glow='pulse'),
    cube((-3.0, -2.9, -4.06), (-1.2, -2.7, -4.02), G_ICE, glow='flicker'), cube((1.2, -2.9, -4.06), (3.0, -2.7, -4.02), G_ICE, glow='flicker'),
    cube((-0.15, -1.8, -4.06), (0.15, -0.4, -4.02), G_ICE, glow='flicker'),
    cube((-3.2, -4.4, -4.08), (-0.8, -4.25, -4.02), G_PURPLE, glow='pulse'),
], children=[cable_chain(-1.3), cable_chain(1.3)])]
net_body = [group((0, 0, 0), [
    cube((-3.0, 6.5, 2.0), (3.0, 10.5, 3.6), PURPLE),
    cube((-2.4, 7.1, 3.6), (2.4, 9.7, 3.7), CIRCUIT, glow='flicker'),
    cube((-3.2, 7.0, 2.2), (-3.0, 10.0, 3.4), SERVO), cube((3.0, 7.0, 2.2), (3.2, 10.0, 3.4), SERVO),
    cube((-1.9, 5.9, 2.3), (-0.7, 6.5, 3.3), gm.GOLD, 'b'), cube((0.7, 5.9, 2.3), (1.9, 6.5, 3.3), gm.GOLD, 'b'),
    cube((-3.2, 10.5, 2.0), (3.2, 11.2, 3.8), GUNMETAL),
])]
net_arm = [group((-1, 8, 0), [
    cyl((-3.6, 8.3, -2.6), (1.6, 9.3, 2.6), 'y', DARKCHROME),
    cyl((-3.7, 8.65, -2.7), (1.7, 8.95, 2.7), 'y', G_PURPLE, glow='pulse'),
    cube((-3.06, 3.0, -0.5), (-3.0, 8.2, 0.5), CIRCUIT, glow='flicker'),
    cube((-2.2, 9.3, -2.35), (0.2, 9.9, -2.05), G_ICE, glow='pulse'),
])]
SETS['netrunner_rig'] = {"item_part": "head", "load": 20, "parts": {
    "head": net_head, "body": net_body, "right_arm": net_arm, "left_arm": mirror_x(net_arm)}}

# ================================================================ Heavy borg
borg_body = [group((0, 0, 0), [
    cube((-4.5, 0.3, -2.9), (4.5, 7.5, -2.0), RED_ARMOR),
    cube((-4.3, 7.5, -2.6), (4.3, 10.2, -2.0), GUNMETAL),
    cyl((-1.7, 2.3, -3.2), (1.7, 5.7, -2.9), 'z', DARKCHROME),
    cyl((-1.1, 2.9, -3.35), (1.1, 5.1, -3.1), 'z', G_ORANGE, glow='breath'),
    cube((-4.6, -0.8, -2.7), (4.6, 0.6, 2.7), GUNMETAL),
    cube((-3.8, 1.0, 2.0), (-0.6, 7.0, 4.2), GUNMETAL), cube((0.6, 1.0, 2.0), (3.8, 7.0, 4.2), GUNMETAL),
    cube((-3.4, 1.6, 4.2), (-1.0, 6.4, 4.3), VENT, glow='heat'), cube((1.0, 1.6, 4.2), (3.4, 6.4, 4.3), VENT, glow='heat'),
    cyl((-0.6, 0.5, 2.0), (0.6, 11.0, 3.2), 'y', SERVO),
], children=[
    group((-2.2, 1.0, 4.3), [cube((-3.6, 1.0, 4.3), (-0.8, 6.6, 4.8), RED_ARMOR)],
          anims=[rot_anim("x", "sprint", amp=-55), rot_anim("x", "breath", amp=-6)]),
    group((2.2, 1.0, 4.3), [cube((0.8, 1.0, 4.3), (3.6, 6.6, 4.8), RED_ARMOR)],
          anims=[rot_anim("x", "sprint", amp=-55), rot_anim("x", "breath", amp=-6, phase=1.5)]),
])]
borg_arm = [group((-1, 0, 0), [
    cube((-4.6, -2.8, -3.0), (1.4, 1.2, 3.0), RED_ARMOR),
    cube((-4.8, -1.2, -3.2), (0.6, 0.4, 3.2), ARMOR),
    cube((-4.85, 0.0, -3.25), (0.4, 0.3, 3.25), G_ORANGE, glow='breath'),
    cube((-3.3, 1.2, -2.3), (1.3, 4.0, 2.3), GUNMETAL),
    cyl((-3.7, 3.4, -1.5), (1.7, 5.0, 1.5), 'x', SERVO),
    cube((-4.0, 4.6, -3.0), (1.8, 10.6, 3.0), RED_ARMOR),
    cube((-3.8, 10.2, -3.15), (1.6, 11.0, -2.2), DARKCHROME),
    cube((-4.05, 6.0, -2.0), (-4.0, 9.0, 2.0), VENT, glow='heat'),
    cyl((-1.7, 2.0, 2.3), (-0.3, 5.0, 3.5), 'y', GUNMETAL),
] + [cube((-3.4 + k * 1.7, 10.4, -3.25), (-2.6 + k * 1.7, 10.8, -3.15), G_ORANGE, glow='pulse') for k in range(3)],
    children=[group((-1, 5, 2.8), [cyl((-1.35, 5.0, 2.55), (-0.65, 8.5, 3.25), 'y', gm.CHROME, 'b')],
                    anims=[move_anim("y", "swing", 1.6)])])]
borg_head = [group((0, 0, 0), [
    cube((-3.4, -3.0, -4.5), (3.4, 0.2, -3.9), GUNMETAL),
    cube((-2.5, -1.5, -4.56), (2.5, -0.8, -4.5), VENT, glow='heat'),
    cube((-4.3, -2.5, -1.0), (-4.0, -1.0, 1.0), DARKCHROME), cube((4.0, -2.5, -1.0), (4.3, -1.0, 1.0), DARKCHROME),
    cube((1.2, -4.0, -4.15), (2.8, -3.0, -4.02), gm.G_RED, 'b', glow='pulse'),
    cube((-4.2, -8.3, -2.0), (4.2, -7.9, 3.0), ARMOR),
])]
borg_leg = [group((0, 0, 0), [
    cube((-2.5, 3.5, -2.6), (2.5, 6.5, -2.0), RED_ARMOR),
    cube((-2.3, 6.5, -2.4), (2.3, 11.0, -2.0), GUNMETAL),
    cyl((-0.45, 5.0, 2.0), (0.45, 10.0, 2.9), 'y', gm.CHROME, 'b'),
    cube((-2.4, 11.0, -2.5), (2.4, 12.2, 2.5), ARMOR),
])]
SETS['heavy_borg'] = {"item_part": "right_arm", "load": 35, "parts": {
    "body": borg_body, "right_arm": borg_arm, "left_arm": mirror_x(borg_arm), "head": borg_head,
    "right_leg": borg_leg, "left_leg": borg_leg}}

# ================================================================ Mercenary cyberarms
rotor = group((-1, 8.9, 0), [
    cyl((-3.7, 8.6, -2.7), (1.7, 9.2, 2.7), 'y', DARKCHROME),
    cube((-3.5, 8.75, -2.5), (-2.9, 9.05, -1.9), gm.G_PINK, 'b', glow='pulse'),
    cube((0.9, 8.75, 1.9), (1.5, 9.05, 2.5), gm.G_PINK, 'b', glow='pulse'),
], anims=[rot_anim("y", "age", amp=0, speed=8.0)])
fingers = group((-1, 10.2, -2.0), [
    cube((-3.1, 10.2, -2.3), (1.1, 11.4, -1.3), TEAL),
    cube((-3.15, 10.5, -2.35), (1.15, 10.7, -1.25), SERVO),
], anims=[rot_anim("x", "swing", amp=-55)])
merc_arm = [group((-1, 0, 0), [
    cube((-3.45, -2.45, -2.45), (1.45, 0.5, 2.45), TEAL),
    cube((-3.3, 0.5, -2.3), (1.3, 3.6, 2.3), WHITE),
    cyl((-3.6, 3.3, -1.8), (1.6, 4.9, 1.8), 'x', SERVO),
    cyl((-3.65, 3.7, -1.4), (1.65, 4.5, 1.4), 'x', gm.G_PINK, 'b', glow='pulse'),
    cube((-3.3, 4.6, -2.3), (1.3, 8.6, 2.3), TEAL),
    cyl((-3.8, 5.0, -0.5), (-3.1, 8.2, 0.5), 'y', gm.CHROME, 'b'),
    cube((-3.2, 9.2, -2.2), (1.2, 10.3, 2.2), TEAL),
    cube((-1.6, 10.3, -0.6), (-0.4, 10.36, 0.6), gm.G_PINK, 'b', glow='pulse'),
    cube((-3.36, 0.8, -0.15), (-3.3, 8.5, 0.15), gm.G_PINK, 'b', glow='pulse'),
    cube((1.3, 0.8, -0.15), (1.36, 8.5, 0.15), gm.G_PINK, 'b', glow='pulse'),
], children=[rotor, fingers])]
merc_body = [group((0, 0, 0), [
    cube((-1.2, 0.5, 2.0), (1.2, 2.7, 2.7), TEAL),
    cube((-0.5, 1.1, 2.7), (0.5, 2.1, 2.8), gm.G_PINK, 'b', glow='pulse'),
    cyl((-4.4, -0.3, -1.4), (-3.6, 2.5, 1.4), 'x', SERVO), cyl((3.6, -0.3, -1.4), (4.4, 2.5, 1.4), 'x', SERVO),
])]
SETS['merc_arms'] = {"item_part": "right_arm", "load": 25, "parts": {
    "right_arm": merc_arm, "left_arm": mirror_x(merc_arm), "body": merc_body}}


# ---------------------------------------------------------------- item model export
def flatten(groups):
    for g in groups:
        yield from g["cubes"]
        yield from flatten(g.get("children", []))


def item_model(set_id, spec):
    cubes = list(flatten(spec["parts"][spec["item_part"]]))
    # part space (y down, face -Z) -> model space (y up, face +Z): rotate 180 deg about X
    conv = [((c["from"][0], -c["to"][1], -c["to"][2]), (c["to"][0], -c["from"][1], -c["from"][2]), c) for c in cubes]
    lo = [min(f[i] for f, _, _ in conv) for i in range(3)]
    hi = [max(t[i] for _, t, _ in conv) for i in range(3)]
    size = max(hi[i] - lo[i] for i in range(3))
    s = min(1.35, 14.0 / size)
    ctr = [(lo[i] + hi[i]) / 2 for i in range(3)]
    elements = []
    for f, t, c in conv:
        fr = [round(8 + (f[i] - ctr[i]) * s, 3) for i in range(3)]
        to = [round(8 + (t[i] - ctr[i]) * s, 3) for i in range(3)]
        e = gm.box(fr, to, c["mat"], glow=bool(c.get("glow")))
        tex = "#chrome" if c["atlas"] == "c" else "#atlas"
        for face in e["faces"].values(): face["texture"] = tex
        elements.append(e)
    disp = gm.display_for(elements)
    gm.write(f'{ROOT}/models/item/3d/{set_id}.json', {
        "textures": {"atlas": "edgerunners:item/cyberware", "chrome": "edgerunners:item/chrome",
                     "particle": "edgerunners:item/chrome"},
        "elements": elements, "display": disp})
    gm.write(f'{ROOT}/models/item/{set_id}.json', {"parent": f"edgerunners:item/3d/{set_id}"})


if __name__ == '__main__':
    import gen_materials
    gen_materials.build_chrome(f'{ROOT}/textures/item/chrome.png')
    os.makedirs(f'{ROOT}/chrome', exist_ok=True)
    for sid, spec in SETS.items():
        gm.write(f'{ROOT}/chrome/{sid}.json', {"parts": spec["parts"]})
        item_model(sid, spec)
    print('chrome sets:', ', '.join(SETS))
