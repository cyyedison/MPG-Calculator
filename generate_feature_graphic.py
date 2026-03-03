"""
Renders the MPG Calculator Play Store feature graphic (1024x500 PNG).
Layout: navy background, gauge illustration left-centre, app name + tagline right.
Renders at 2x (2048x1000) then downscales for smooth anti-aliasing.
"""
import math
from PIL import Image, ImageDraw, ImageFont

RENDER_W, RENDER_H = 2048, 1000
FINAL_W,  FINAL_H  = 1024, 500

WHITE  = '#FFFFFF'
RED    = '#FF5252'
NAVY   = '#0D2B55'
NAVY_L = '#112F5E'   # slightly lighter for subtle gradient panel

img  = Image.new('RGB', (RENDER_W, RENDER_H), NAVY)
draw = ImageDraw.Draw(img)

# ── Subtle right-side lighter panel ──────────────────────────────────────
draw.rectangle([RENDER_W // 2, 0, RENDER_W, RENDER_H], fill=NAVY_L)

# ── Gauge — centred in the left half ─────────────────────────────────────
# Original viewport 108x108; scale to ~480px tall → S≈4.44, centred at (512, 500)
S   = 4.6
cx  = 512          # horizontal centre of gauge in render coords
cy  = 530          # vertical centre of gauge baseline

def gx(v): return cx + (v - 54) * S
def gy(v): return cy + (v - 68) * S

def line(x1, y1, x2, y2, w, color):
    draw.line([(gx(x1), gy(y1)), (gx(x2), gy(y2))],
              fill=color, width=max(1, int(w * S)))

r = 26 * S
# Gauge arc
bbox = [gx(54) - r, gy(68) - r, gx(54) + r, gy(68) + r]
draw.arc(bbox, start=180, end=360, fill=WHITE, width=max(1, int(5.5 * S)))

# Baseline + ticks
line(28, 68, 80, 68,  3.0, WHITE)
line(28, 68, 35, 68,  3.0, WHITE)
line(36, 50, 41, 55,  3.0, WHITE)
line(54, 42, 54, 49,  3.0, WHITE)
line(72, 50, 67, 55,  3.0, WHITE)
line(80, 68, 73, 68,  3.0, WHITE)

# Needle
line(51, 73, 65, 49,  4.0, RED)

# Centre pivot
pr = 4 * S
draw.ellipse([gx(54) - pr, gy(68) - pr, gx(54) + pr, gy(68) + pr], fill=WHITE)

# Fuel drop
drop_poly = [(gx(54), gy(76)), (gx(49), gy(84)), (gx(59), gy(84))]
draw.polygon(drop_poly, fill=WHITE)
dr = 5 * S
draw.ellipse([gx(54) - dr, gy(84) - dr, gx(54) + dr, gy(84) + dr], fill=WHITE)

# ── Text — right half ─────────────────────────────────────────────────────
font_bold    = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 108)
font_regular = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',  62)
font_small   = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',  52)

text_x  = RENDER_W // 2 + 90
text_max = RENDER_W - 60   # right boundary
title_y = 270

# App name — wrap if needed by measuring first
title = 'MPG Calculator'
bbox_t = draw.textbbox((0, 0), title, font=font_bold)
title_w = bbox_t[2] - bbox_t[0]
# Scale font down until it fits
while title_w > (text_max - text_x) and font_bold.size > 60:
    font_bold = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', font_bold.size - 4)
    bbox_t = draw.textbbox((0, 0), title, font=font_bold)
    title_w = bbox_t[2] - bbox_t[0]

draw.text((text_x, title_y), title, font=font_bold, fill=WHITE)

# Tagline — shrink font until it fits
tagline = 'Track your real-world fuel economy'
while True:
    bbox_tag = draw.textbbox((0, 0), tagline, font=font_regular)
    if (bbox_tag[2] - bbox_tag[0]) <= (text_max - text_x) or font_regular.size < 40:
        break
    font_regular = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', font_regular.size - 2)
draw.text((text_x, title_y + font_bold.size + 30),
          tagline, font=font_regular, fill='#AECBF0')

# Feature bullets
bullets = [
    '  Multi-car support',
    '  UK & US MPG · L/100km · km/L',
    '  Trend chart · Cost per mile',
]
by = title_y + font_bold.size + 30 + font_regular.size + 60
for b in bullets:
    # Draw a small filled circle manually then the text
    dot_r = 10
    dot_x = text_x + 6
    dot_y = by + font_small.size // 2
    draw.ellipse([dot_x - dot_r, dot_y - dot_r, dot_x + dot_r, dot_y + dot_r],
                 fill='#7BAFD4')
    draw.text((text_x + 36, by), b.strip(), font=font_small, fill='#7BAFD4')
    by += font_small.size + 26

# ── Thin accent line between panels ──────────────────────────────────────
draw.rectangle([RENDER_W // 2 - 3, 60, RENDER_W // 2 + 3, RENDER_H - 60],
               fill='#1E4A8A')

# ── Downscale to 1024x500 ─────────────────────────────────────────────────
out = img.resize((FINAL_W, FINAL_H), Image.LANCZOS)
out.save('play_store_feature_graphic.png')
print("Saved play_store_feature_graphic.png (1024x500)")
