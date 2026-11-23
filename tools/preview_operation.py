"""Preview of the ripperdoc chair during an operation (arm deployed over a seated player)."""
import json, math, sys
import numpy as np
from PIL import Image
import preview_chrome as pc
import preview_vehicles as pv

R = 'src/main/resources/assets/edgerunners/'
SEATED = [((4, 9, 4.5, 12, 21, 8.5), (38, 40, 52)), ((4, 21, 2.5, 12, 29, 10.5), (205, 160, 130)),
          ((4, 7, -5.5, 8, 11, 6.5), (28, 30, 38)), ((8, 7, -5.5, 12, 11, 6.5), (28, 30, 38)),
          ((0, 9, 4.5, 4, 21, 8.5), (38, 40, 52)), ((12, 9, 4.5, 16, 21, 8.5), (38, 40, 52))]

def element_faces(e, proj):
    (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
    rot = e.get('rotation')
    def tf(p):
        if not rot: return np.array(p)
        o = np.array(rot['origin']); a = rot['angle']
        Rm = pc.rmat(*(a if ax == rot['axis'] else 0 for ax in 'xyz'))
        return o + Rm @ (np.array(p) - o)
    out = []
    for i, q in enumerate(pc.box_faces(x0, y0, z0, x1, y1, z1)):
        d = ['north', 'south', 'west', 'east', 'down', 'up'][i]
        uv = e['faces'][d]['uv']; k = 16
        u0, v0, u1, v1 = [c * k for c in uv]
        sh = 1.0 if e.get('shade') is False else [0.8, 0.8, 0.62, 0.62, 0.45, 1.0][i]
        out.append((0, [proj(tf(p)) for p in q], ('b', [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]), None, sh))
    return out

def render(sig, yaw=-150, S=520, lasers=()):
    ya, pa = math.radians(yaw), math.radians(18); sc = S * 0.8 / 48
    def proj(p):
        x, y, z = p[0] - 8, p[1] - 20, p[2] - 8
        X = x * math.cos(ya) + z * math.sin(ya); Z = -x * math.sin(ya) + z * math.cos(ya)
        Y = y * math.cos(pa) - Z * math.sin(pa); D = y * math.sin(pa) + Z * math.cos(pa)
        return (S / 2 + X * sc, S / 2 - Y * sc, D)
    faces = []
    for e in json.load(open(R + 'models/block/ripperdoc_chair.json'))['elements']: faces += element_faces(e, proj)
    items = []; pv.collect(json.load(open(R + 'machine/chair_arm.json'))['parts']['root'], sig, lambda p: np.array(p), items)
    for c, f in items:
        (x0, y0, z0), (x1, y1, z1) = c['from'], c['to']
        tile = c['mat']; u0, v0 = (tile % 4) * 64, (tile // 4) * 64
        dims = [(x1 - x0, y1 - y0)] * 2 + [(z1 - z0, y1 - y0)] * 2 + [(x1 - x0, z1 - z0)] * 2
        for i, q in enumerate(pc.box_faces(x0, y0, z0, x1, y1, z1)):
            w, h = dims[i]; us, vs = 64 * min(1, w / 4), 64 * min(1, h / 4)
            sh = 1.0 if c.get('glow') else [0.8, 0.8, 0.62, 0.62, 0.45, 1.0][i]
            faces.append((0, [proj(f(p)) for p in q], (c['atlas'], [(u0, v0), (u0 + us, v0), (u0 + us, v0 + vs), (u0, v0 + vs)]), None, sh))
    if sig.get('player'):
        for (b, col) in SEATED:
            for i, q in enumerate(pc.box_faces(*b)):
                faces.append((0, [proj(p) for p in q], None, tuple(int(c * [0.85, 0.7, 0.6, 0.6, 0.45, 1][i]) for c in col), 1))
    old = pc.S; pc.S = S
    try: img = pc.raster(faces)
    finally: pc.S = old
    if lasers:
        from PIL import ImageDraw
        d = ImageDraw.Draw(img)
        for a, b, col in lasers:
            pa_, pb_ = proj(a), proj(b)
            d.line([pa_[:2], pb_[:2]], fill=col, width=3); d.line([pa_[:2], pb_[:2]], fill=(255, 255, 255), width=1)
            d.ellipse([pb_[0] - 5, pb_[1] - 5, pb_[0] + 5, pb_[1] + 5], fill=(255, 240, 200))
    return img

def emitters(sig):
    out = []
    for (x, y, z, col) in ((13.9, 33.35, 6.0, (255, 40, 60)), (15.6, 33.35, 6.0, (255, 40, 60)), (14.75, 33.35, 8.1, (40, 230, 255))):
        dx, dy, dz = x - 14.75, y - sig['drop'] - 39, z - sig['reach'] - 14.25
        a = math.radians(sig['swing'])
        rx, rz = dx * math.cos(a) + dz * math.sin(a), -dx * math.sin(a) + dz * math.cos(a)
        out.append(((14.75 + rx, 39 + dy, 14.25 + rz), col))
    return out

if __name__ == '__main__':
    S = 420
    frames = [{'swing': 20, 'reach': 1.4, 'drop': 0, 'spin': 0, 'player': True},
              {'swing': 41, 'reach': 2.9, 'drop': 1.2, 'spin': 0, 'player': True},
              {'swing': 41, 'reach': 2.9, 'drop': 2.5, 'spin': 40, 'player': True},
              {'swing': 41, 'reach': 2.9, 'drop': 2.5, 'spin': 80, 'player': True}]
    targets = [None, None, (8, 25, 2.3), (14, 16, 6.5)]
    sheet = Image.new('RGB', (S * 4, S), (26, 27, 36))
    for i, (sig, tg) in enumerate(zip(frames, targets)):
        las = [(a, tg, col) for a, col in emitters(sig)] if tg else []
        sheet.paste(render(sig, yaw=145, S=S, lasers=las), (i * S, 0))
    sheet.save(sys.argv[1])
