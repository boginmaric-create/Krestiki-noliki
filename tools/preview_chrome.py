"""Renders a textured preview of every external chrome set on a player figure. Usage: python3 tools/preview_chrome.py out.png"""
import json, math, sys
import numpy as np
from PIL import Image, ImageDraw
from preview_models import coeffs

R = 'src/main/resources/assets/edgerunners/'
ATL = {'b': Image.open(R + 'textures/item/cyberware.png').convert('RGBA'), 'c': Image.open(R + 'textures/item/chrome.png').convert('RGBA')}
ATN = {k: np.asarray(v, float) for k, v in ATL.items()}
S = 340
PARTS = {'head': ((0, 0, 0), (-4, -8, -4, 4, 0, 4), (205, 160, 130)),
         'body': ((0, 0, 0), (-4, 0, -2, 4, 12, 2), (38, 40, 52)),
         'right_arm': ((-5, 2, 0), (-3, -2, -2, 1, 10, 2), (38, 40, 52)),
         'left_arm': ((5, 2, 0), (-1, -2, -2, 3, 10, 2), (38, 40, 52)),
         'right_leg': ((-1.9, 12, 0), (-2, 0, -2, 2, 12, 2), (28, 30, 38)),
         'left_leg': ((1.9, 12, 0), (-2, 0, -2, 2, 12, 2), (28, 30, 38))}

def rmat(x, y, z):
    x, y, z = map(math.radians, (x, y, z))
    Rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    Ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    Rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return Rz @ Ry @ Rx

def signal(name, sig, a):
    v = {'deploy': sig['deploy'], 'sprint': sig['sprint'], 'swing': sig['swing'], 'sway': sig['sway'],
         'breath': math.sin(sig['age'] * 0.08 + a.get('phase', 0)), 'age': sig['age']}[name]
    if name == 'sway': v = math.sin(sig['age'] * 0.3 + a.get('phase', 0)) * sig['sway']
    return v

def group_tf(g, sig):
    r = list(g.get('rot', [0, 0, 0])); mv = [0, 0, 0]
    for a in g.get('anims', []):
        i = 'xyz'.index(a['axis']); s = signal(a['signal'], sig, a)
        if 'from' in a: val = a['from'] + (a['to'] - a['from']) * s
        elif a['signal'] == 'age': val = s * a.get('speed', 0)
        else: val = a['amp'] * s
        if a['type'] == 'rot': r[i] += val
        else: mv[i] += val
    return rmat(*r), np.array(mv), np.array(g['pivot'])

def collect(groups, sig, tf, out):
    for g in groups:
        Rg, mv, pv = group_tf(g, sig)
        f = (lambda p, Rg=Rg, mv=mv, pv=pv, tf=tf: tf(pv + Rg @ (np.array(p) - pv) + mv))
        for c in g['cubes']: out.append((c, f))
        collect(g.get('children', []), sig, f, out)

def box_faces(x0, y0, z0, x1, y1, z1):
    return [[(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], [(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)],
            [(x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1)], [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)],
            [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]]
SHADE = [0.85, 0.7, 0.6, 0.6, 1.0, 0.45]

def render(set_id, yaw, sig, pose):
    spec = json.load(open(R + f'chrome/{set_id}.json'))['parts']
    faces = []
    ya, pa = math.radians(yaw), math.radians(12)
    def proj(p):
        x, y, z = p[0], -p[1] + 12, -p[2]
        X = x * math.cos(ya) + z * math.sin(ya); Z = -x * math.sin(ya) + z * math.cos(ya)
        Y = y * math.cos(pa) - Z * math.sin(pa); D = y * math.sin(pa) + Z * math.cos(pa)
        return (S / 2 + X * 8.6, S / 2 - Y * 8.6, D)
    for name, (piv, bx, col) in PARTS.items():
        Rp = rmat(*pose.get(name, (0, 0, 0))); piv = np.array(piv)
        tf = lambda p, Rp=Rp, piv=piv: piv + Rp @ np.array(p)
        for i, q in enumerate(box_faces(*bx)):
            pts = [proj(tf(p)) for p in q]
            faces.append((0, pts, None, tuple(int(c * SHADE[i]) for c in col), 1))
        items = []
        collect(spec.get(name, []), sig, tf, items)
        for c, f in items:
            (x0, y0, z0), (x1, y1, z1) = c['from'], c['to']
            tile = c['mat']; u0, v0 = (tile % 4) * 64, (tile // 4) * 64
            dims = [(x1 - x0, y1 - y0), (x1 - x0, y1 - y0), (z1 - z0, y1 - y0), (z1 - z0, y1 - y0), (x1 - x0, z1 - z0), (x1 - x0, z1 - z0)]
            for i, q in enumerate(box_faces(x0, y0, z0, x1, y1, z1)):
                w, h = dims[i]; us, vs = 64 * min(1, w / 4), 64 * min(1, h / 4)
                pts = [proj(f(p)) for p in q]
                faces.append((sum(p[2] for p in pts) / 4, pts, (c['atlas'], [(u0, v0), (u0 + us, v0), (u0 + us, v0 + vs), (u0, v0 + vs)]),
                              None, 1.0 if c.get('glow') else SHADE[i]))
    return raster(faces)


def raster(faces, ss=2):
    W = S * ss
    col = np.zeros((W, W, 3)) + (26, 27, 36)
    dep = np.full((W, W), -1e9)
    for _, pts, tex, flat, sh in faces:
        P = np.array([(p[0] * ss, p[1] * ss, p[2]) for p in pts])
        uv = np.array(tex[1], float) if tex else None
        arr = ATN[tex[0]] if tex else None
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = P[list(tri)]
            x0, x1 = int(max(0, np.floor(min(a[0], b[0], c[0])))), int(min(W - 1, np.ceil(max(a[0], b[0], c[0]))))
            y0, y1 = int(max(0, np.floor(min(a[1], b[1], c[1])))), int(min(W - 1, np.ceil(max(a[1], b[1], c[1]))))
            if x1 < x0 or y1 < y0: continue
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9: continue
            yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1] + 0.5
            w0 = ((b[1] - c[1]) * (xx - c[0]) + (c[0] - b[0]) * (yy - c[1])) / den
            w1 = ((c[1] - a[1]) * (xx - c[0]) + (a[0] - c[0]) * (yy - c[1])) / den
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any(): continue
            z = w0 * a[2] + w1 * b[2] + w2 * c[2]
            sub = dep[y0:y1 + 1, x0:x1 + 1]
            win = inside & (z > sub)
            if not win.any(): continue
            sub[win] = z[win]
            if tex:
                ta, tb, tc = uv[list(tri)]
                u = w0 * ta[0] + w1 * tb[0] + w2 * tc[0]; v = w0 * ta[1] + w1 * tb[1] + w2 * tc[1]
                px = arr[np.clip(v.astype(int), 0, 255), np.clip(u.astype(int), 0, 255), :3] * sh
            else:
                px = np.broadcast_to(np.array(flat, float), (*win.shape, 3))
            col[y0:y1 + 1, x0:x1 + 1][win] = px[win]
    return Image.fromarray(np.clip(col, 0, 255).astype('uint8')).resize((S, S), Image.LANCZOS)

if __name__ == '__main__':
    combat = {'right_arm': (-70, 10, 0), 'left_arm': (-35, -15, 0)}
    shots = [('street_samurai', {'deploy': 1, 'sprint': 0, 'swing': 0, 'sway': 0, 'age': 10}, combat),
             ('netrunner_rig', {'deploy': 0, 'sprint': 0, 'swing': 0, 'sway': 1, 'age': 7}, {}),
             ('heavy_borg', {'deploy': 0, 'sprint': 1, 'swing': 1, 'sway': 0, 'age': 20}, {'right_arm': (-20, 0, 0)}),
             ('merc_arms', {'deploy': 0, 'sprint': 0, 'swing': 1, 'sway': 0, 'age': 5}, {'right_arm': (-80, 0, 0)})]
    sheet = Image.new('RGB', (S * 4, S * 2), (26, 27, 36))
    for i, (sid, sig, pose) in enumerate(shots):
        sheet.paste(render(sid, 28, sig, pose), (i * S, 0))
        sheet.paste(render(sid, 180 + 28, sig, pose), (i * S, S))
    sheet.save(sys.argv[1])
