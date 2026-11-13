"""
The archer golem's hit rate against what a beta skeleton standing in its place would manage, by the re-run of beta's
arrow code in tools/skeleton_arrows.py (Still Faithful's, for its Skeleton Arrows). A golem's arrow starts at its own
eye (1.62, less 0.1), not 1.4 over a skeleton's head; aimed beta's way from there it climbs or drops more steeply than
a skeleton's, so BowJob aims it to cross the target's distance where the skeleton's arrow would and scales its spread
by how much quicker or slower across it is (SkeletonAim's way, with the golem's own lift).

  python library/golems/tools/archer_arrows.py [--n N]

Targets stand on the ground (a server's player's box, as the tool has it; a zombie's is the same size), `rise` below
the golem's feet: 0 level, -3, -4 and -5 from a perch at its edge.
"""
import argparse
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "tools"))
import skeleton_arrows as sa  # noqa: E402

GOLEM_EYE = float(sa.np.float32(1.62))
# Where a skeleton standing there starts its arrow, over where the golem starts its own.
GOLEM_LIFT = (sa.SKEL_EYE - 0.1 + sa.LIFT) - (GOLEM_EYE - 0.1)
# The re-run starts arrows at the skeleton's eye less 0.1 plus a lift: the golem's start is its eye's 0.09 higher.
EYE_OVER = GOLEM_EYE - sa.SKEL_EYE


def matched_aim(vy, across, speed=0.6, spread=12.0):
    """BowJob's aim: SkeletonAim.aim with the golem's lift in place of 1.4."""
    beta_vy = vy - GOLEM_LIFT
    want = GOLEM_LIFT + sa.rise(across, beta_vy, speed)
    if across < 0.5 or not math.isfinite(want):
        return vy, spread
    at = lambda angle: sa.rise(across, across * math.tan(angle), speed)
    lo, hi = -math.radians(80), math.radians(80)
    for _ in range(40):
        a, b = lo + (hi - lo) / 3, hi - (hi - lo) / 3
        if at(a) < at(b):
            lo = a
        else:
            hi = b
    angle = (lo + hi) / 2
    if at(angle) > want:
        lo, hi = -math.radians(80), angle
        for _ in range(40):
            mid = (lo + hi) / 2
            if at(mid) < want:
                lo = mid
            else:
                hi = mid
        angle = (lo + hi) / 2
    aimed = across * math.tan(angle)
    return aimed, spread * math.hypot(across, beta_vy) / math.hypot(across, aimed)


WAYS = (('beta skeleton', sa.LIFT, sa.beta_aim), ('golem, beta aim', EYE_OVER, sa.beta_aim), ('golem, matched', EYE_OVER, matched_aim))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--n", type=int, default=20000)
    a = ap.parse_args()
    print('across                                  ' + ' '.join(f'{h:3d}' for h in sa.RANGES))
    for r in (0, -3, -4, -5):
        kind = 'level' if r == 0 else 'cliff'
        for way in WAYS:
            sa.row(f'{kind} {r:+d} {way[0]}', lambda h: sa.rate(a.n, h, r, way, True, kind=kind))


if __name__ == "__main__":
    main()
