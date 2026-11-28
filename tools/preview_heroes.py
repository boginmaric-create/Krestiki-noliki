"""Textured z-buffered preview of the hero personas. Usage: python3 tools/preview_heroes.py out.png"""
import json, math, sys
import numpy as np
from PIL import Image
import preview_chrome as pc

R = 'src/main/resources/assets/edgerunners/'
IDS = ['runner_kid', 'netrunner_girl', 'gunner_girl', 'veteran_borg', 'chrome_tyrant']

def render(hid, yaw, sig, pose, S=360):
    m = json.load(open(R + f'hero/{hid}.json'))
    pc.ATN['h'] = np.asarray(Image.open(R + f'textures/hero/{hid}.png').convert('RGBA'), float)
    scale = m['scale']
    ya, pa = math.radians(yaw), math.radians(10)
    def proj(p):
        x, y, z = p[0] * scale, (-p[1] + 12) * scale, -p[2] * scale
        X = x * math.cos(ya) + z * math.sin(ya); Z = -x * math.sin(ya) + z * math.cos(ya)
        Y = y * math.cos(pa) - Z * math.sin(pa); D = y * math.sin(pa) + Z * math.cos(pa)
        return (S / 2 + X * 7.2, S * 0.62 - Y * 7.2, D)
    faces = []
    for name, (piv, bx, col) in pc.PARTS.items():
        Rp = pc.rmat(*pose.get(name, (0, 0, 0))); pv = np.array(piv)
        tf = lambda p, Rp=Rp, pv=pv: pv + Rp @ np.array(p)
        items = []
        pc.collect(m['parts'].get(name, []), sig, tf, items)
        for c, f in items:
            (x0, y0, z0), (x1, y1, z1) = c['from'], c['to']
            tile = c['mat']; u0, v0 = (tile % 4) * 64, (tile // 4) * 64
            dims = [(x1 - x0, y1 - y0)] * 2 + [(z1 - z0, y1 - y0)] * 2 + [(x1 - x0, z1 - z0)] * 2
            for i, q in enumerate(pc.box_faces(x0, y0, z0, x1, y1, z1)):
                w, h = dims[i]; us, vs = 64 * min(1, w / 4), 64 * min(1, h / 4)
                sh = 1.0 if c.get('glow') else pc.SHADE[i]
                faces.append((0, [proj(f(p)) for p in q], (c['atlas'], [(u0, v0), (u0 + us, v0), (u0 + us, v0 + vs), (u0, v0 + vs)]), None, sh))
    old = pc.S; pc.S = S
    try: return pc.raster(faces)
    finally: pc.S = old

if __name__ == '__main__':
    S = 360
    sig = {'deploy': 0, 'sprint': 0, 'swing': 0, 'sway': 0.6, 'age': 12}
    pose = {'right_arm': (-15, 0, 5), 'left_arm': (10, 0, -5), 'right_leg': (12, 0, 0), 'left_leg': (-12, 0, 0)}
    sheet = Image.new('RGB', (S * len(IDS), S * 2), (24, 25, 34))
    for i, h in enumerate(IDS):
        sheet.paste(render(h, 22, sig, pose, S), (i * S, 0))
        sheet.paste(render(h, 180 + 30, sig, pose, S), (i * S, S))
    sheet.save(sys.argv[1])
