#!/usr/bin/env python3
"""Generates Haven's logo assets from one piece of geometry.

The mark is three shapes: a house, an arched doorway with the light on,
and the sun coming up behind the roof. The sun is Material 3's own
"Sunny" shape, the same polygon a light's badge blooms into inside the
app, so the logo is drawn from the app's shape language rather than
beside it.

    pip install shapely
    python3 tools/logo.py

Writes, all from the same shapes:

    res/drawable/ic_launcher_*.xml     adaptive icon layers and themed icon
    res/drawable*/haven_splash.xml     splash mark, day and night
    res/drawable/ic_stat_haven.xml     notification icon
    ui/theme/BrandGeometry.kt          path data for the in-app HavenMark
    docs/*.svg                         copies for the web

Nothing is rasterised: Android renders the vectors at any density.
"""
import math
import re
from pathlib import Path

from shapely import affinity, minimum_bounding_circle, minimum_bounding_radius
from shapely.geometry import Polygon

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
DOCS = ROOT / "docs"

# Brand colors (ui/theme/Palette.kt: Leaf.t40, Lime, Sun; the field is
# Cream.t96).
LEAF = "#346A22"
LIME = "#9BF67C"
SUN = "#FFB83D"
PAPER = "#F6F4E9"

# MaterialShapes.Sunny from androidx.compose.material3 1.5, as cubics on
# the unit square (dumped from the library, not redrawn).
SUNNY = (
    "M0.9969 0.5000 C0.9969 0.5179 0.9907 0.5359 0.9783 0.5504 C0.9530 0.5801 0.9277 0.6098 0.9024 "
    "0.6395 C0.8918 0.6519 0.8854 0.6674 0.8841 0.6837 C0.8810 0.7226 0.8779 0.7615 0.8748 0.8004 "
    "C0.8718 0.8384 0.8415 0.8687 0.8035 0.8717 C0.7646 0.8748 0.7257 0.8779 0.6868 0.8810 C0.6705 "
    "0.8823 0.6550 0.8887 0.6426 0.8993 C0.6129 0.9246 0.5832 0.9499 0.5535 0.9752 C0.5245 1.0000 "
    "0.4817 1.0000 0.4527 0.9752 C0.4230 0.9499 0.3933 0.9246 0.3636 0.8993 C0.3511 0.8887 0.3357 "
    "0.8823 0.3194 0.8810 C0.2805 0.8779 0.2416 0.8748 0.2027 0.8717 C0.1646 0.8687 0.1344 0.8384 "
    "0.1314 0.8004 C0.1283 0.7615 0.1252 0.7226 0.1221 0.6837 C0.1208 0.6674 0.1144 0.6519 0.1038 "
    "0.6395 C0.0785 0.6098 0.0532 0.5801 0.0279 0.5504 C0.0031 0.5214 0.0031 0.4786 0.0279 0.4496 "
    "C0.0532 0.4199 0.0785 0.3902 0.1038 0.3605 C0.1144 0.3481 0.1208 0.3326 0.1221 0.3163 C0.1252 "
    "0.2774 0.1283 0.2385 0.1314 0.1996 C0.1344 0.1616 0.1646 0.1313 0.2027 0.1283 C0.2416 0.1252 "
    "0.2805 0.1221 0.3194 0.1190 C0.3357 0.1177 0.3511 0.1113 0.3636 0.1007 C0.3933 0.0754 0.4230 "
    "0.0501 0.4527 0.0248 C0.4817 0.0000 0.5245 0.0000 0.5535 0.0248 C0.5832 0.0501 0.6129 0.0754 "
    "0.6426 0.1007 C0.6550 0.1113 0.6705 0.1177 0.6868 0.1190 C0.7257 0.1221 0.7646 0.1252 0.8035 "
    "0.1283 C0.8415 0.1313 0.8718 0.1616 0.8748 0.1996 C0.8779 0.2385 0.8810 0.2774 0.8841 0.3163 "
    "C0.8854 0.3326 0.8918 0.3481 0.9024 0.3605 C0.9277 0.3902 0.9530 0.4199 0.9783 0.4496 C0.9907 "
    "0.4641 0.9969 0.4821 0.9969 0.5000 Z "
)


def cubic(p0, p1, p2, p3, steps):
    for i in range(1, steps + 1):
        t = i / steps
        u = 1 - t
        yield (
            u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0],
            u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1],
        )


def sunny(cx, cy, size, rotate=0.0, steps=8):
    nums = [float(v) for v in re.findall(r"-?\d+\.\d+", SUNNY)]
    points = [(nums[0], nums[1])]
    for i in range(2, len(nums) - 5, 6):
        points += cubic(points[-1], (nums[i], nums[i + 1]), (nums[i + 2], nums[i + 3]), (nums[i + 4], nums[i + 5]), steps)
    a = math.radians(rotate)
    placed = []
    for x, y in points:
        x -= 0.5
        y -= 0.5
        placed.append((cx + (x * math.cos(a) - y * math.sin(a)) * size, cy + (x * math.sin(a) + y * math.cos(a)) * size))
    return Polygon(placed).buffer(0)


def SUN_SIZE_UNIT(placed_sun):
    """The side of the square the unit Sunny was scaled to, recovered from its area."""
    return math.sqrt(placed_sun.area / sunny(0, 0, 1.0).area)


def rounded(points, radius):
    """A convex polygon with every corner filleted."""
    return Polygon(points).buffer(-radius, join_style=1, quad_segs=14).buffer(radius, join_style=1, quad_segs=14)


def arch(cx, base, width, height, steps=28):
    r = width / 2
    points = [(cx - r, base), (cx + r, base), (cx + r, base - height + r)]
    points += [(cx + r * math.cos(math.pi * i / steps), base - height + r - r * math.sin(math.pi * i / steps)) for i in range(1, steps)]
    points.append((cx - r, base - height + r))
    return Polygon(points)


SUN_ROTATION = 11.0
# The clear band of field kept between the sun and the roof, so the two
# never touch at any size.
HALO = 3.4


def compose():
    """The mark in its own coordinates. Returns (sun, house, door, halo, full_sun)."""
    cx, base, eave, peak, half = 50.0, 80.0, 53.0, 31.0, 22.5
    house = rounded([(cx - half, base), (cx + half, base), (cx + half, eave), (cx, peak), (cx - half, eave)], 6.5)
    door = arch(cx, base, 15.5, 24.0)
    halo = house.buffer(HALO, join_style=1)
    full_sun = sunny(cx + 21.0, 37.5, 33.0, rotate=SUN_ROTATION)
    return full_sun.difference(halo), house.difference(door), door, halo, full_sun


def fit(parts, cx, cy, radius):
    """Scale and centre the parts so the drawn mark's bounding circle is (cx, cy, radius)."""
    whole = parts[0].union(parts[1]).union(parts[2])
    c = minimum_bounding_circle(whole).centroid
    scale = radius / minimum_bounding_radius(whole)
    out = []
    for part in parts:
        moved = affinity.translate(part, cx - c.x, cy - c.y)
        out.append(affinity.scale(moved, scale, scale, origin=(cx, cy)))
    return out


def path(geom, precision=2):
    """Path data. Points closer than the last printed digit can show are dropped."""
    tolerance = 2 * 10 ** -precision
    geoms = geom.geoms if hasattr(geom, "geoms") else [geom]
    out = []
    for g in geoms:
        for ring in [g.exterior, *g.interiors]:
            coords = list(ring.simplify(tolerance).coords)[:-1]
            out.append("M" + "L".join(f"{x:.{precision}f},{y:.{precision}f}" for x, y in coords) + "Z")
    return "".join(out)


def vector(size, layers, comment):
    body = "\n".join(
        f'    <path\n        android:fillColor="{color}"\n        android:fillType="evenOdd"\n        android:pathData="{path(g)}" />'
        for g, color in layers
    )
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"<!-- {comment} Generated by tools/logo.py: edit the geometry there. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
        f'    android:viewportWidth="{size}"\n    android:viewportHeight="{size}">\n'
        f"{body}\n</vector>\n"
    )


def svg(size, layers, background=None, corner=0):
    field = f'<rect width="{size}" height="{size}" rx="{corner}" fill="{background}"/>' if background else ""
    body = "".join(f'<path fill="{color}" fill-rule="evenodd" d="{path(g)}"/>' for g, color in layers)
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}">{field}{body}</svg>\n'


def main():
    drawable = RES / "drawable"
    drawable.mkdir(parents=True, exist_ok=True)

    # Launcher icon: 108dp canvas, and everything must sit inside the
    # 66dp circle every launcher mask keeps (radius 33); 31 leaves air.
    sun, house, door, _, _ = fit(list(compose()), 54, 54, 31)
    (drawable / "ic_launcher_foreground.xml").write_text(
        vector(108, [(sun, SUN), (house, LEAF), (door, LIME)], "Launcher icon foreground: the Haven mark.")
    )
    (drawable / "ic_launcher_monochrome.xml").write_text(
        vector(108, [(sun.union(house), "#000000")], "Themed icon: the mark as one silhouette, doorway cut out.")
    )
    (drawable / "ic_launcher_background.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Launcher icon field: paper. Generated by tools/logo.py. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        f'    <path\n        android:fillColor="{PAPER}"\n        android:pathData="M0,0h108v108h-108z" />\n</vector>\n'
    )

    # Splash screen: the launcher composition with no field. Android puts
    # it on the window background, which is paper by day and black by
    # night, so the night copy swaps to the lime house.
    (drawable / "haven_splash.xml").write_text(
        vector(108, [(sun, SUN), (house, LEAF), (door, LIME)], "Splash mark, day.")
    )
    night = RES / "drawable-night"
    night.mkdir(parents=True, exist_ok=True)
    (night / "haven_splash.xml").write_text(vector(108, [(sun, SUN), (house, LIME)], "Splash mark, night."))

    # Status bar icon for notifications: the silhouette, white, 24dp. The
    # system tints it; only its alpha matters.
    sun, house, door, _, _ = fit(list(compose()), 12, 12, 11)
    (drawable / "ic_stat_haven.xml").write_text(
        vector(24, [(sun.union(house), "#FFFFFF")], "Notification icon: the mark as a silhouette.")
    )

    # The mark for Compose, which draws it live (the sun turns): the same
    # geometry on a unit square, as path data and the sun's placement.
    sun, house, door, halo, full_sun = fit(list(compose()), 0.5, 0.5, 0.49)
    box = full_sun.bounds
    (ROOT / "app/src/main/java/com/garrietta/haven/ui/theme/BrandGeometry.kt").write_text(
        "package com.garrietta.haven.ui.theme\n\n"
        "/**\n"
        " * The Haven mark's geometry on a unit square, for HavenMark to draw.\n"
        " *\n"
        " * GENERATED by tools/logo.py from the same shapes as the launcher icon.\n"
        " * Do not edit by hand.\n"
        " */\n"
        "internal object BrandGeometry {\n"
        "    /** The house with the doorway cut out of it. */\n"
        f'    const val HOUSE = "{path(house, 4)}"\n\n'
        "    /** The doorway. */\n"
        f'    const val DOOR = "{path(door, 4)}"\n\n'
        "    /** The house grown by the clear band: the sun is never drawn inside this. */\n"
        f'    const val HALO = "{path(halo, 4)}"\n\n'
        "    /** Where MaterialShapes.Sunny sits: centre, diameter, resting rotation in degrees. */\n"
        f"    const val SUN_CX = {(box[0] + box[2]) / 2:.4f}f\n"
        f"    const val SUN_CY = {(box[1] + box[3]) / 2:.4f}f\n"
        f"    const val SUN_SIZE = {SUN_SIZE_UNIT(full_sun):.4f}f\n"
        f"    const val SUN_ROTATION = {SUN_ROTATION}f\n"
        "}\n"
    )

    DOCS.mkdir(exist_ok=True)
    sun, house, door, _, _ = fit(list(compose()), 54, 54, 31)
    (DOCS / "icon.svg").write_text(svg(108, [(sun, SUN), (house, LEAF), (door, LIME)], PAPER, corner=30))
    sun, house, door, _, _ = fit(list(compose()), 24, 24, 23.5)
    (DOCS / "mark.svg").write_text(svg(48, [(sun, SUN), (house, LEAF), (door, LIME)]))
    (DOCS / "mark-night.svg").write_text(svg(48, [(sun, SUN), (house, LIME)]))
    print("wrote the launcher layers, splash and notification icons, BrandGeometry.kt and docs SVGs")


if __name__ == "__main__":
    main()
