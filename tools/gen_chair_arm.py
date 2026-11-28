"""Animated surgical arm of the ripperdoc chair (drawn by the block entity renderer).

Block space in pixels, Y up, chair facing north (-Z); same group/cube format as the chrome sets.
Signals: swing (deg, boom yaw toward the patient), reach (px along the boom), drop (px down),
spin (deg, tool rotation). Glow modes: laser (on during surgery), pulse, steady.
Run from the repository root: python3 tools/gen_chair_arm.py"""
import os
import gen_models as gm
from gen_chrome import cube, cyl, group, rot_anim, move_anim, SERVO, CABLE, DARKCHROME, CIRCUIT, GUNMETAL

ROOT = 'src/main/resources/assets/edgerunners'
PIVOT = (14.75, 39.0, 14.25)

tool = group((14.75, 33.0, 6.85), [
    cyl((14.55, 29.4, 6.65), (14.95, 33.2, 7.05), 'y', gm.CHROME, 'b'),
    cube((14.62, 29.0, 6.72), (14.88, 29.4, 6.98), gm.G_CYAN, 'b', glow='laser'),
    cube((13.5, 30.6, 6.7), (13.8, 33.2, 7.0), GUNMETAL), cube((15.7, 30.6, 6.7), (16.0, 33.2, 7.0), GUNMETAL),
    cube((14.6, 30.6, 5.6), (14.9, 33.2, 5.9), GUNMETAL), cube((14.6, 30.6, 7.8), (14.9, 33.2, 8.1), GUNMETAL),
], anims=[rot_anim("y", "spin", amp=1)])

head = group((14.75, 35.5, 6.85), [
    cyl((14.3, 37.0, 6.4), (15.2, 45.0, 7.3), 'y', gm.CHROME, 'b'),
    cube((12.75, 34.0, 4.85), (16.75, 37.0, 8.85), gm.PANEL, 'b'),
    cube((12.6, 35.0, 4.7), (16.9, 35.5, 9.0), gm.HAZARD, 'b'),
    cyl((13.25, 33.5, 5.35), (16.25, 34.0, 8.35), 'y', DARKCHROME),
    cube((13.5, 33.2, 5.6), (14.3, 33.5, 6.4), gm.G_RED, 'b', glow='laser'),
    cube((15.2, 33.2, 5.6), (16.0, 33.5, 6.4), gm.G_RED, 'b', glow='laser'),
    cube((14.35, 33.2, 7.7), (15.15, 33.5, 8.5), gm.G_CYAN, 'b', glow='laser'),
    cube((16.75, 35.8, 5.5), (16.85, 36.6, 8.2), CIRCUIT, glow='pulse'),
    cube((12.65, 35.8, 5.5), (12.75, 36.6, 8.2), CIRCUIT, glow='pulse'),
], anims=[move_anim("y", "drop", -1)], children=[tool])

carriage = group((14.75, 38.5, 6.85), [
    cube((13.8, 37.6, 5.8), (15.7, 38.6, 7.9), SERVO),
], anims=[move_anim("z", "reach", -1)], children=[head])

boom = group(PIVOT, [
    cyl((14.0, 38.5, 3.0), (15.5, 40.0, 15.0), 'z', gm.CHROME, 'b'),
    cube((14.2, 40.0, 4.0), (15.3, 40.4, 14.0), CABLE),
    cube((14.05, 39.0, 2.6), (15.45, 39.6, 3.0), gm.G_PINK, 'b', glow='pulse'),
], anims=[rot_anim("y", "swing", amp=1)], children=[carriage])

post = group((0, 0, 0), [
    cyl((13.9, 2.2, 13.4), (15.6, 38.0, 15.1), 'y', gm.STEEL, 'b'),
    cyl((13.7, 4.0, 13.2), (15.8, 5.0, 15.3), 'y', gm.HAZARD, 'b'),
    cyl((13.4, 37.0, 12.9), (16.1, 40.0, 15.6), 'y', SERVO),
    cube((15.5, 22.0, 13.4), (15.65, 27.0, 15.1), CIRCUIT, glow='pulse'),
    cube((14.3, 6.0, 15.0), (15.2, 36.0, 15.6), CABLE),
])

if __name__ == '__main__':
    os.makedirs(f'{ROOT}/machine', exist_ok=True)
    gm.write(f'{ROOT}/machine/chair_arm.json', {"parts": {"root": [post, boom]}})
    print('chair arm ok')
