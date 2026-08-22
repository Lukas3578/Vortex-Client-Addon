from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path('/home/ubuntu/Vortex-Client-Addon/src/client/resources/assets/vortexplus/textures/cosmetics')
OUT.mkdir(parents=True, exist_ok=True)

W = H = 64

def checker(img, a, b, step=4):
    d = ImageDraw.Draw(img)
    for y in range(0, H, step):
        for x in range(0, W, step):
            d.rectangle((x, y, x + step - 1, y + step - 1), fill=a if (x // step + y // step) % 2 == 0 else b)
    return d

def edge_grid(d, color, bright):
    for p in range(0, 64, 8):
        d.line((p, 0, p, 63), fill=color, width=1)
        d.line((0, p, 63, p), fill=color, width=1)
    for x, y in [(7, 8), (33, 6), (49, 22), (14, 44), (57, 52)]:
        d.point((x, y), fill=bright)
        d.point((x + 1, y), fill=bright)
        d.point((x, y + 1), fill=bright)

def make_cap():
    img = Image.new('RGBA', (W, H), '#06142a')
    d = checker(img, '#071a36', '#0a2041')
    edge_grid(d, '#12355c', '#6beeff')
    d.rectangle((0, 0, 31, 15), fill='#0d3f7a')
    d.rectangle((2, 2, 29, 5), fill='#35cfff')
    d.rectangle((2, 6, 29, 12), fill='#11326c')
    d.rectangle((24, 0, 31, 15), fill='#0b2855')
    d.rectangle((0, 16, 39, 23), fill='#117dca')
    d.rectangle((2, 17, 37, 19), fill='#74f7ff')
    d.rectangle((18, 16, 25, 23), fill='#09203f')
    d.rectangle((20, 17, 23, 20), fill='#e0ffff')
    d.rectangle((32, 16, 47, 23), fill='#173c72')
    d.rectangle((48, 16, 63, 23), fill='#0c244c')
    return img

def make_halo():
    img = Image.new('RGBA', (W, H), '#071228')
    d = checker(img, '#081b35', '#0a2442')
    edge_grid(d, '#0f3a60', '#85ffff')
    for y in (0, 8, 16, 24):
        d.rectangle((0, y, 47, y + 3), fill='#0a6285')
        d.rectangle((1, y, 46, y), fill='#b6ffff')
        d.rectangle((10, y + 1, 17, y + 2), fill='#39e5ff')
        d.rectangle((31, y + 1, 38, y + 2), fill='#39e5ff')
    d.rectangle((32, 0, 47, 7), fill='#0b2a56')
    d.rectangle((36, 2, 43, 5), fill='#d4ffff')
    return img

def make_crown():
    img = Image.new('RGBA', (W, H), '#12091f')
    d = checker(img, '#160c28', '#22113a')
    edge_grid(d, '#412169', '#c96bff')
    d.rectangle((0, 0, 31, 15), fill='#241042')
    d.rectangle((1, 1, 30, 4), fill='#a33cff')
    d.rectangle((3, 5, 8, 14), fill='#4b1b86')
    d.rectangle((13, 3, 18, 14), fill='#7f2bd0')
    d.rectangle((23, 5, 28, 14), fill='#4b1b86')
    d.rectangle((0, 16, 43, 23), fill='#29114c')
    d.rectangle((1, 17, 42, 19), fill='#b346ff')
    d.rectangle((44, 0, 55, 7), fill='#51218d')
    d.rectangle((47, 2, 52, 5), fill='#f5baff')
    return img

def make_headphones():
    img = Image.new('RGBA', (W, H), '#07151f')
    d = checker(img, '#0a202d', '#0c2938')
    edge_grid(d, '#104154', '#62f4d6')
    d.rectangle((0, 0, 31, 15), fill='#123342')
    d.rectangle((2, 1, 29, 4), fill='#36d7c2')
    d.rectangle((3, 5, 28, 14), fill='#0f2736')
    d.rectangle((0, 16, 17, 31), fill='#143b48')
    d.rectangle((2, 18, 7, 29), fill='#4cefd6')
    d.rectangle((18, 16, 35, 31), fill='#0d2633')
    d.rectangle((36, 16, 47, 23), fill='#1a4a55')
    d.rectangle((39, 18, 44, 21), fill='#fff094')
    return img

def make_antenna():
    img = Image.new('RGBA', (W, H), '#071d1b')
    d = checker(img, '#082823', '#0b342d')
    edge_grid(d, '#14554a', '#72ffd2')
    d.rectangle((0, 0, 31, 15), fill='#126b58')
    d.rectangle((2, 2, 29, 5), fill='#62ffd0')
    d.rectangle((2, 6, 29, 14), fill='#0c4e42')
    d.rectangle((0, 16, 15, 31), fill='#127b61')
    d.rectangle((2, 17, 7, 29), fill='#7effc0')
    d.rectangle((16, 16, 31, 31), fill='#0e6b55')
    d.rectangle((18, 18, 27, 27), fill='#a8ff9b')
    d.rectangle((32, 16, 47, 31), fill='#0d443b')
    d.rectangle((36, 19, 43, 27), fill='#3ffff0')
    return img

textures = {
    'hat_vortex_cap.png': make_cap(),
    'hat_neon_halo.png': make_halo(),
    'hat_void_crown.png': make_crown(),
    'hat_cyber_headphones.png': make_headphones(),
    'hat_slime_antenna.png': make_antenna(),
}

for name, image in textures.items():
    image.save(OUT / name, optimize=True)
    print(f'{name}: {image.size}, opaque={sum(1 for px in image.getdata() if px[3] == 255)}')
