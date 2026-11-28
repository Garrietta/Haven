#!/usr/bin/env python3
"""
Draws Haven's own icon glyphs and writes them to
app/src/main/java/com/garrietta/haven/ui/components/HavenGlyphs.kt.

They are the things in a house Material's icon set has no picture of (a
fan, a heater, a garage door, a sprinkler...), drawn the way Material's
filled icons are: solid shapes on a 24-unit grid, a hole cut by drawing
it the other way round, under the nonzero rule.

    python3 tools/glyphs.py            # rewrite HavenGlyphs.kt
    python3 tools/glyphs.py --preview  # also write build/glyphs.html to look at
"""
import math, os, sys

def f(v): 
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s == "-0" else s
def circle(cx, cy, r, ccw=False):
    sw = 0 if ccw else 1
    return f"M{f(cx-r)} {f(cy)}a{f(r)} {f(r)} 0 1 {sw} {f(2*r)} 0a{f(r)} {f(r)} 0 1 {sw} {f(-2*r)} 0Z"
def rrect(x0, y0, x1, y1, r, ccw=False):
    r = min(r, (x1-x0)/2, (y1-y0)/2)
    if r <= 0:
        return (f"M{f(x0)} {f(y0)}V{f(y1)}H{f(x1)}V{f(y0)}Z" if ccw else f"M{f(x0)} {f(y0)}H{f(x1)}V{f(y1)}H{f(x0)}Z")
    if not ccw:
        return (f"M{f(x0+r)} {f(y0)}H{f(x1-r)}A{f(r)} {f(r)} 0 0 1 {f(x1)} {f(y0+r)}V{f(y1-r)}"
                f"A{f(r)} {f(r)} 0 0 1 {f(x1-r)} {f(y1)}H{f(x0+r)}A{f(r)} {f(r)} 0 0 1 {f(x0)} {f(y1-r)}"
                f"V{f(y0+r)}A{f(r)} {f(r)} 0 0 1 {f(x0+r)} {f(y0)}Z")
    return (f"M{f(x0+r)} {f(y0)}A{f(r)} {f(r)} 0 0 0 {f(x0)} {f(y0+r)}V{f(y1-r)}A{f(r)} {f(r)} 0 0 0 {f(x0+r)} {f(y1)}"
            f"H{f(x1-r)}A{f(r)} {f(r)} 0 0 0 {f(x1)} {f(y1-r)}V{f(y0+r)}A{f(r)} {f(r)} 0 0 0 {f(x1-r)} {f(y0)}Z")
def rot(pts, deg, cx=12, cy=12):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return [(cx + (x-cx)*c - (y-cy)*s, cy + (x-cx)*s + (y-cy)*c) for x, y in pts]
def cubic_path(start, segs, deg=0):
    # segs: list of (c1, c2, p)
    pts = [start] + [q for seg in segs for q in seg]
    pts = rot(pts, deg)
    out = f"M{f(pts[0][0])} {f(pts[0][1])}"
    for i in range(1, len(pts), 3):
        a, b, c = pts[i], pts[i+1], pts[i+2]
        out += f"C{f(a[0])} {f(a[1])} {f(b[0])} {f(b[1])} {f(c[0])} {f(c[1])}"
    return out + "Z"

G = {}
# Fan: three swept blades round a hub, the hub cut out to a ring.
blade_start = (11.0, 9.9)
blade = [((9.2, 8.4), (8.6, 4.6), (10.6, 2.9)), ((12.6, 1.2), (16.2, 2.0), (16.3, 4.8)), ((16.4, 7.2), (14.4, 8.7), (13.4, 10.2))]
G["fan"] = ("".join(cubic_path(blade_start, blade, d) for d in (0, 120, 240)) + circle(12, 12, 2.6) + circle(12, 12, 1.1, ccw=True), "nonzero")

# Heater: a radiator of four fins on a rail, two feet, heat rising above.
fins = "".join(rrect(x, 7.5, x + 3.3, 19, 1.65) for x in (3, 7.9, 12.8, 17.7))
rails = rrect(4, 10.2, 20, 11.4, 0.4) + rrect(4, 15.4, 20, 16.6, 0.4)
feet = rrect(4.2, 18.5, 5.8, 21, 0.6) + rrect(18.2, 18.5, 19.8, 21, 0.6)
def wave(x):
    return (f"M{f(x)} 6.3C{f(x-1.1)} 5.3 {f(x+0.9)} 4.2 {f(x-0.2)} 2.4C{f(x-0.4)} 2.1 {f(x)} 1.8 {f(x+0.3)} 2.0"
            f"C{f(x+1.6)} 3.6 {f(x-0.2)} 4.7 {f(x+0.9)} 6.0C{f(x+1.1)} 6.4 {f(x+0.6)} 6.8 {f(x+0.2)} 6.6Z")
G["heater"] = (fins + rails + feet + wave(8.2) + wave(12) + wave(15.8), "nonzero")

# Garage door: a roofed frame over a door of three slats.
frame = "M2 10.2L12 3l10 7.2V21h-2.6V11.8H4.6V21H2Z"
slats = rrect(6.2, 13.2, 17.8, 15.2, 0.4) + rrect(6.2, 16.1, 17.8, 18.1, 0.4) + rrect(6.2, 19, 17.8, 21, 0.4)
G["garage_door"] = (frame + slats, "nonzero")

# Sprinkler: a head on its riser, spraying jets three ways, on the ground.
def capsule(x1, y1, x2, y2, w):
    dx, dy = x2 - x1, y2 - y1
    L = math.hypot(dx, dy); nx, ny = -dy / L * w, dx / L * w
    return (f"M{f(x1+nx)} {f(y1+ny)}L{f(x2+nx)} {f(y2+ny)}A{f(w)} {f(w)} 0 0 0 {f(x2-nx)} {f(y2-ny)}"
            f"L{f(x1-nx)} {f(y1-ny)}A{f(w)} {f(w)} 0 0 0 {f(x1+nx)} {f(y1+ny)}Z")
head = rrect(9.3, 11, 14.7, 13.6, 1.2) + rrect(11.1, 13.4, 12.9, 19.4, 0.5) + rrect(3, 19.4, 21, 21.2, 0.9)
jets = ""
for ang in (-62, -31, 0, 31, 62):
    a = math.radians(ang)
    def at(d): return 12 + d * math.sin(a), 11.4 - d * math.cos(a)
    for d0, d1 in ((3.4, 5.6), (7.0, 9.2)):
        (x1, y1), (x2, y2) = at(d0), at(d1)
        jets += capsule(x1, y1, x2, y2, 0.75)
G["sprinkler"] = (head + jets, "nonzero")

# Valve: a pipe through a valve body, its stem and handwheel.
G["valve"] = (rrect(2, 13.2, 22, 18, 1) + circle(12, 15.6, 4.2) + rrect(11, 6.5, 13, 12, 0.5) + rrect(6.5, 4.5, 17.5, 7, 1.25), "nonzero")

# Humidifier: a rounded body and neck, a drop of mist above, a slot cut in.
drop = "M12 1.8C13.4 3.7 14.4 5 14.4 6.2A2.4 2.4 0 0 1 9.6 6.2C9.6 5 10.6 3.7 12 1.8Z"
G["humidifier"] = (drop + rrect(6, 11.2, 18, 21.5, 3.2) + rrect(9, 9.6, 15, 11.6, 0.6) + rrect(9, 16.8, 15, 18.2, 0.7, ccw=True), "nonzero")

# Air purifier: a tall body with a round intake and grille slots cut in.
G["air_purifier"] = (rrect(5.5, 2, 18.5, 22, 3.6) + circle(12, 8.2, 3.3, ccw=True) + circle(12, 8.2, 1.2)
                     + "".join(rrect(8.8, y, 15.2, y + 1.3, 0.65, ccw=True) for y in (13.4, 15.9, 18.4)), "nonzero")

# Water heater: a tank on legs with a flame cut in.
flame = ("M12 8.4C14.6 10.6 15.6 12.4 15.2 14.4C14.8 16.2 13.5 17.2 12 17.2C10.5 17.2 9.2 16.2 8.8 14.4"
         "C8.5 12.8 9.4 11.6 10.4 10.9C10.4 12.3 11 13 11.8 13.2C11.6 11.4 11.4 9.9 12 8.4Z")
# the flame is drawn counter to the tank so it cuts out
def reverse_flame():
    return ("M12 8.4C11.4 9.9 11.6 11.4 11.8 13.2C11 13 10.4 12.3 10.4 10.9C9.4 11.6 8.5 12.8 8.8 14.4"
            "C9.2 16.2 10.5 17.2 12 17.2C13.5 17.2 14.8 16.2 15.2 14.4C15.6 12.4 14.6 10.6 12 8.4Z")
G["water_heater"] = (rrect(5.5, 2, 18.5, 19.6, 3.4) + reverse_flame() + rrect(7, 19, 9, 22, 0.6) + rrect(15, 19, 17, 22, 0.6), "nonzero")

# Gate: two posts, and a gate of rails and pickets between them.
posts = rrect(2, 3.5, 4.6, 21, 0.8) + rrect(19.4, 3.5, 22, 21, 0.8)
rails2 = rrect(4.6, 7, 19.4, 8.6, 0.3) + rrect(4.6, 16, 19.4, 17.6, 0.3)
pickets = "".join(rrect(x, 5.2, x + 1.7, 19.6, 0.85) for x in (7.2, 11.15, 15.1))
G["gate"] = (posts + rails2 + pickets, "nonzero")

# Dishwasher: a cabinet with its control strip, two knobs and a handle cut in.
G["dishwasher"] = (rrect(4, 2, 20, 22, 2.2) + rrect(4, 6.6, 20, 7.8, 0, ccw=True)
                   + circle(7.4, 4.3, 0.95, ccw=True) + circle(10.4, 4.3, 0.95, ccw=True)
                   + rrect(9, 9.8, 15, 11.2, 0.7, ccw=True), "nonzero")

# Oven: a cabinet with three knobs and a window cut in.
G["oven"] = (rrect(3, 2.5, 21, 21.5, 2.6) + "".join(circle(x, 6.3, 1.05, ccw=True) for x in (7, 12, 17))
             + rrect(6, 9.8, 18, 18.6, 1.6, ccw=True), "nonzero")

# Lamp: a shade, a stem and a base.
G["lamp"] = ("M7.6 2.5H16.4L20 11.2H4Z" + rrect(11.2, 11, 12.8, 18.6, 0.4) + rrect(7, 18.4, 17, 21, 1.3), "nonzero")


NAMES = {
    "fan": ("Fan", "A fan: three swept blades round a hub."),
    "heater": ("Heater", "A heater: a radiator's four fins on their rails, heat rising above."),
    "garage_door": ("GarageDoor", "A garage door: a roofed frame over a door of three slats."),
    "sprinkler": ("Sprinkler", "A sprinkler: a head on its riser, spraying jets three ways, on the ground."),
    "valve": ("Valve", "A valve: a pipe through a valve body, its stem and handwheel."),
    "humidifier": ("Humidifier", "A humidifier: a body and neck with a drop of mist above."),
    "air_purifier": ("AirPurifier", "An air purifier: a tall body with a round intake and grille slots."),
    "water_heater": ("WaterHeater", "A water heater: a tank on legs with a flame in it."),
    "gate": ("Gate", "A gate: two posts, and rails and pickets between them."),
    "dishwasher": ("Dishwasher", "A dishwasher: a cabinet with its control strip, knobs and handle."),
    "oven": ("Oven", "An oven: a cabinet with three knobs and a window."),
    "lamp": ("Lamp", "A table lamp: a shade, a stem and a base."),
}

KOTLIN_HEAD = "package com.garrietta.haven.ui.components\n\nimport androidx.compose.material.icons.materialIcon\nimport androidx.compose.material.icons.materialPath\nimport androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.graphics.vector.PathBuilder\nimport androidx.compose.ui.graphics.vector.PathNode\nimport androidx.compose.ui.graphics.vector.PathParser\n\n/*\n * Glyphs for things in a house that Material's icon set has no picture\n * of: a fan, a heater, a garage door, a sprinkler and the rest. They are\n * drawn the way Material's filled icons are, on the same 24-unit grid,\n * solid shapes with holes cut by drawing the hole the other way round,\n * so they sit beside the stock icons without looking borrowed.\n *\n * Each one is SVG path data, replayed into Material's own icon builder\n * (materialIcon / materialPath), which supplies the fill an Icon then\n * tints.\n *\n * Generated by tools/glyphs.py, which draws the shapes and writes this\n * file; change a glyph there (`python3 tools/glyphs.py --preview` draws\n * them all to look at), never here.\n */\n"
KOTLIN_TAIL = '}\n\nprivate fun glyph(name: String, pathData: String): ImageVector {\n    val nodes = PathParser().parsePathString(pathData).toNodes()\n    return materialIcon(name) { materialPath { replay(nodes) } }\n}\n\n/** Plays parsed path nodes back into a builder, command for command. */\nprivate fun PathBuilder.replay(nodes: List<PathNode>) {\n    for (node in nodes) {\n        when (node) {\n            is PathNode.Close -> close()\n            is PathNode.MoveTo -> moveTo(node.x, node.y)\n            is PathNode.RelativeMoveTo -> moveToRelative(node.dx, node.dy)\n            is PathNode.LineTo -> lineTo(node.x, node.y)\n            is PathNode.RelativeLineTo -> lineToRelative(node.dx, node.dy)\n            is PathNode.HorizontalTo -> horizontalLineTo(node.x)\n            is PathNode.RelativeHorizontalTo -> horizontalLineToRelative(node.dx)\n            is PathNode.VerticalTo -> verticalLineTo(node.y)\n            is PathNode.RelativeVerticalTo -> verticalLineToRelative(node.dy)\n            is PathNode.CurveTo -> curveTo(node.x1, node.y1, node.x2, node.y2, node.x3, node.y3)\n            is PathNode.RelativeCurveTo -> curveToRelative(node.dx1, node.dy1, node.dx2, node.dy2, node.dx3, node.dy3)\n            is PathNode.ReflectiveCurveTo -> reflectiveCurveTo(node.x1, node.y1, node.x2, node.y2)\n            is PathNode.RelativeReflectiveCurveTo -> reflectiveCurveToRelative(node.dx1, node.dy1, node.dx2, node.dy2)\n            is PathNode.QuadTo -> quadTo(node.x1, node.y1, node.x2, node.y2)\n            is PathNode.RelativeQuadTo -> quadToRelative(node.dx1, node.dy1, node.dx2, node.dy2)\n            is PathNode.ReflectiveQuadTo -> reflectiveQuadTo(node.x, node.y)\n            is PathNode.RelativeReflectiveQuadTo -> reflectiveQuadToRelative(node.dx, node.dy)\n            is PathNode.ArcTo -> arcTo(\n                node.horizontalEllipseRadius, node.verticalEllipseRadius, node.theta,\n                node.isMoreThanHalf, node.isPositiveArc, node.arcStartX, node.arcStartY,\n            )\n            is PathNode.RelativeArcTo -> arcToRelative(\n                node.horizontalEllipseRadius, node.verticalEllipseRadius, node.theta,\n                node.isMoreThanHalf, node.isPositiveArc, node.arcStartDx, node.arcStartDy,\n            )\n        }\n    }\n}\n'


def kotlin():
    body = "".join(
        f"    /** {doc} */\n    val {name}: ImageVector by lazy {{ glyph(\"Haven.{name}\", \"{G[key][0]}\") }}\n"
        for key, (name, doc) in NAMES.items()
    )
    return KOTLIN_HEAD + "object HavenGlyphs {\n" + body + KOTLIN_TAIL


def preview():
    cells = "".join(
        f"<div style='text-align:center;font:13px sans-serif'><svg width='144' height='144' viewBox='0 0 24 24'"
        f" style='background:#eef3e6;border-radius:24px'><path d='{d}'/></svg><br>"
        f"<svg width='24' height='24' viewBox='0 0 24 24'><path d='{d}'/></svg><br>{key}</div>"
        for key, (d, _) in G.items()
    )
    return f"<html><body><div style='display:flex;flex-wrap:wrap;gap:18px;padding:18px'>{cells}</div></body></html>"


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out = os.path.join(root, "app/src/main/java/com/garrietta/haven/ui/components/HavenGlyphs.kt")
    with open(out, "w") as fh:
        fh.write(kotlin())
    print("wrote", os.path.relpath(out, root))
    if "--preview" in sys.argv:
        os.makedirs(os.path.join(root, "build"), exist_ok=True)
        with open(os.path.join(root, "build/glyphs.html"), "w") as fh:
            fh.write(preview())
        print("wrote build/glyphs.html")
