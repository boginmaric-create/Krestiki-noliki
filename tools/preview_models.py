"""Renders textured previews of the generated 3D models (for docs). Usage: python3 tools/preview_models.py out.png [chair]"""
import json, glob, math, sys
import numpy as np
from PIL import Image, ImageDraw

ATLAS = Image.open('src/main/resources/assets/edgerunners/textures/item/cyberware.png').convert('RGBA')
S = 300

def coeffs(dst, src):
    A, B = [], []
    for (x, y), (u, v) in zip(dst, src):
        A += [[x, y, 1, 0, 0, 0, -u * x, -u * y], [0, 0, 0, x, y, 1, -v * x, -v * y]]
        B += [u, v]
    return np.linalg.solve(np.array(A, float), np.array(B, float)).tolist()

def rotate_pts(pts, R):
    if not R: return pts
    o = R['origin']; a = math.radians(R['angle']); ax = R['axis']; out = []
    for x, y, z in pts:
        x, y, z = x - o[0], y - o[1], z - o[2]
        c, s = math.cos(a), math.sin(a)
        if ax == 'x': y, z = y * c - z * s, y * s + z * c
        elif ax == 'y': x, z = x * c + z * s, -x * s + z * c
        else: x, y = x * c - y * s, x * s + y * c
        out.append((x + o[0], y + o[1], z + o[2]))
    return out

def render(path, yaw=-35, pitch=25, sc=None):
    m = json.load(open(path)); els = m['elements']
    lo = [min(e['from'][i] for e in els) for i in range(3)]; hi = [max(e['to'][i] for e in els) for i in range(3)]
    cx, cy, cz = [(lo[i] + hi[i]) / 2 for i in range(3)]
    sc = sc or S * 0.62 / max(hi[i] - lo[i] for i in range(3))
    ya, pa = math.radians(yaw), math.radians(pitch)
    def P(x, y, z):
        x, y, z = x - cx, y - cy, z - cz
        X = x * math.cos(ya) + z * math.sin(ya); Z = -x * math.sin(ya) + z * math.cos(ya)
        Y = y * math.cos(pa) - Z * math.sin(pa); D = y * math.sin(pa) + Z * math.cos(pa)
        return (S / 2 + X * sc, S / 2 - Y * sc, D)
    light = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
    faces = []
    for e in els:
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        quads = {'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
                 'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
                 'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
                 'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
                 'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
                 'down': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]}
        for d, q in quads.items():
            q = rotate_pts(q, e.get('rotation'))
            pts = [P(*p) for p in q]
            faces.append((sum(p[2] for p in pts) / 4, pts, e['faces'][d]['uv'], 1.0 if e.get('shade') is False else light[d]))
    img = Image.new('RGBA', (S, S), (30, 31, 40, 255))
    k = ATLAS.width / 16
    for _, pts, uv, sh in sorted(faces, key=lambda f: f[0]):
        u0, v0, u1, v1 = [c * k for c in uv]
        src = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
        dst = [(p[0], p[1]) for p in pts]
        try: cf = coeffs(dst, src)
        except np.linalg.LinAlgError: continue
        tex = ATLAS.transform((S, S), Image.PERSPECTIVE, cf, Image.BILINEAR)
        if sh < 1: tex = Image.blend(Image.new('RGBA', (S, S), (0, 0, 0, 255)), tex, sh)
        mask = Image.new('L', (S, S)); ImageDraw.Draw(mask).polygon(dst, fill=255)
        img.paste(tex, (0, 0), mask)
    return img

if __name__ == '__main__':
    out = sys.argv[1]
    if len(sys.argv) > 2 and sys.argv[2] == 'chair':
        files = ['src/main/resources/assets/edgerunners/models/block/ripperdoc_chair.json']; yaw = -140
    else:
        files = sorted(glob.glob('src/main/resources/assets/edgerunners/models/item/3d/*.json')); yaw = -30
    sheet = Image.new('RGB', (S * min(5, len(files)), S * math.ceil(len(files) / 5)), (30, 31, 40))
    for i, f in enumerate(files):
        sheet.paste(render(f, yaw).convert('RGB'), ((i % 5) * S, (i // 5) * S))
    sheet.save(out)
