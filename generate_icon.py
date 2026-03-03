"""
Renders the MPG Calculator Play Store icon (512x512 PNG).
Matches the adaptive icon vector: deep navy background, white gauge arc,
white ticks, red needle, white pivot, white fuel drop.
Renders at 2048x2048 then downscales to 512x512 for smooth anti-aliasing.
"""
import math
from PIL import Image, ImageDraw

RENDER = 2048
FINAL  = 512
# The vector is 108x108; we render at RENDER px
S = RENDER / 108.0

def s(v):
    return v * S

def line(draw, x1, y1, x2, y2, w, color):
    draw.line([(s(x1), s(y1)), (s(x2), s(y2))], fill=color, width=max(1, int(w * S)))

img  = Image.new('RGB', (RENDER, RENDER), '#0D2B55')
draw = ImageDraw.Draw(img)

WHITE = '#FFFFFF'
RED   = '#FF5252'

# ── Gauge arc: centre (54,68) radius 26, upper semicircle ────────────────
cx, cy, r = s(54), s(68), s(26)
bbox = [cx - r, cy - r, cx + r, cy + r]
draw.arc(bbox, start=180, end=360, fill=WHITE, width=max(1, int(5.5 * S)))

# ── Baseline ──────────────────────────────────────────────────────────────
line(draw, 28, 68, 80, 68,   3, WHITE)

# ── Tick marks ────────────────────────────────────────────────────────────
line(draw, 28, 68, 35, 68,   3, WHITE)   # 180° left
line(draw, 36, 50, 41, 55,   3, WHITE)   # 135°
line(draw, 54, 42, 54, 49,   3, WHITE)   # 90°  top
line(draw, 72, 50, 67, 55,   3, WHITE)   # 45°
line(draw, 80, 68, 73, 68,   3, WHITE)   # 0°   right

# ── Needle: tail (51,73) → tip (65,49) ───────────────────────────────────
line(draw, 51, 73, 65, 49,   4, RED)

# ── Centre pivot circle: (54,68) r=4 ─────────────────────────────────────
pr = s(4)
draw.ellipse([cx - pr, cy - pr, cx + pr, cy + pr], fill=WHITE)

# ── Fuel drop: pointed top, rounded bottom ───────────────────────────────
# Triangle: tip (54,76), left (49,84), right (59,84)
drop_poly = [(s(54), s(76)), (s(49), s(84)), (s(59), s(84))]
draw.polygon(drop_poly, fill=WHITE)
# Bottom semicircle: centre (54,84) r=5
dr = s(5)
dcx, dcy = s(54), s(84)
draw.ellipse([dcx - dr, dcy - dr, dcx + dr, dcy + dr], fill=WHITE)

# ── Downscale to 512x512 ─────────────────────────────────────────────────
out = img.resize((FINAL, FINAL), Image.LANCZOS)
out.save('play_store_icon.png')
print("Saved play_store_icon.png (512x512)")
