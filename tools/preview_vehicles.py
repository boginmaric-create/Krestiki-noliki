"""Textured z-buffered preview of the vehicle models. Usage: python3 tools/preview_vehicles.py out.png"""
import json, math, sys
import numpy as np
from PIL import Image
import preview_chrome as pc

R = 'src/main/resources/assets/edgerunners/'
PAINT = {'speedster': (250, 200, 30), 'cyber_bike': (235, 60, 140)}

def collect(groups, sig, tf, out):
    for g in groups:
        r = list(g.get('rot', [0, 0, 0]))
        for a in g.get('anims', []):
            r['xyz'.index(a['axis'])] += a['amp'] * sig.get(a['signal'], 0)
        Rg = pc.rmat(*r); pv = np.array(g['pivot'])
        f = (lambda p, Rg=Rg, pv=pv, tf=tf: tf(pv + Rg @ (np.array(p) - pv)))
        for c in g['cubes']: out.append((c, f))
        collect(g.get('children', []), sig, f, out)

def render(vid, yaw, sig, S=420, scale=None):
    groups = json.load(open(R + f'vehicle/{vid}.json'))['parts']['root']
    items = []; collect(groups, sig, lambda p: np.array(p), items)
    ya, pa = math.radians(yaw), math.radians(22)
    sc = scale or (S * 0.85 / 70)
    def proj(p):
        x, y, z = p[0], p[1] - 8, p[2]
        X = x * math.cos(ya) + z * math.sin(ya); Z = -x * math.sin(ya) + z * math.cos(ya)
        Y = y * math.cos(pa) - Z * math.sin(pa); D = y * math.sin(pa) + Z * math.cos(pa)
        return (S / 2 + X * sc, S / 2 - Y * sc, D)
    faces = []
    for c, f in items:
        (x0, y0, z0), (x1, y1, z1) = c['from'], c['to']
        tile = c['mat']; u0, v0 = (tile % 4) * 64, (tile // 4) * 64
        dims = [(x1 - x0, y1 - y0)] * 2 + [(z1 - z0, y1 - y0)] * 2 + [(x1 - x0, z1 - z0)] * 2
        for i, q in enumerate(pc.box_faces(x0, y0, z0, x1, y1, z1)):
            w, h = dims[i]; us, vs = 64 * min(1, w / 4), 64 * min(1, h / 4)
            sh = 1.0 if c.get('glow') else [0.8, 0.8, 0.62, 0.62, 0.45, 1.0][i]
            atlas = c['atlas'] + ('p' if c.get('paint') else '')
            faces.append((0, [proj(f(p)) for p in q], (atlas, [(u0, v0), (u0 + us, v0), (u0 + us, v0 + vs), (u0, v0 + vs)]), None, sh))
    pc.ATN['bp'] = pc.ATN['b'] * np.array(list(PAINT[vid]) + [255]) / 255.0
    old = pc.S; pc.S = S
    try: return pc.raster(faces)
    finally: pc.S = old

if __name__ == '__main__':
    S = 420
    sheet = Image.new('RGB', (S * 2, S * 2), (26, 27, 36))
    sheet.paste(render('speedster', 35, {'wheel': 20, 'steer': 18}), (0, 0))
    sheet.paste(render('speedster', 180 + 40, {'wheel': 20, 'steer': 0}), (S, 0))
    sheet.paste(render('cyber_bike', 55, {'wheel': 10, 'steer': 15}, scale=S * 0.85 / 44), (0, S))
    sheet.paste(render('cyber_bike', 180 + 60, {'wheel': 10, 'steer': 0}, scale=S * 0.85 / 44), (S, S))
    sheet.save(sys.argv[1])
