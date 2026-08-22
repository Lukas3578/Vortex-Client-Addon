from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path('/home/ubuntu/Vortex-Client-Addon/src/client/resources/assets/vortexplus/textures/cosmetics')
OUT.mkdir(parents=True, exist_ok=True)
W = H = 64


def make_base(shadow, mid, seam, spark):
    """Opaque pixel-metal base for every cube face, including less-visible UV regions."""
    img = Image.new('RGBA', (W, H), shadow)
    d = ImageDraw.Draw(img)
    for y in range(0, H, 4):
        for x in range(0, W, 4):
            color = mid if (x // 4 + y // 4) % 3 else shadow
            d.rectangle((x, y, x + 3, y + 3), fill=color)
    for p in range(0, 64, 8):
        d.line((p, 0, p, 63), fill=seam)
        d.line((0, p, 63, p), fill=seam)
    for x, y in ((5, 7), (21, 11), (45, 5), (57, 18), (13, 37), (39, 48), (55, 57)):
        d.point((x, y), fill=spark)
        d.point((x + 1, y), fill=spark)
    return img, d


def panel(d, box, dark, mid, light, edge=None):
    x0, y0, x1, y1 = box
    d.rectangle(box, fill=dark)
    d.rectangle((x0 + 1, y0 + 1, x1 - 1, y0 + 2), fill=light)
    d.rectangle((x0 + 1, y0 + 3, x1 - 2, y1 - 2), fill=mid)
    d.line((x0, y0, x0, y1), fill=edge or dark)
    d.line((x0, y1, x1, y1), fill=dark)
    for x in range(x0 + 3, x1 - 1, 4):
        d.line((x, y0 + 3, x, y1 - 2), fill=dark)


def gem(d, cx, cy, dark, mid, bright, core):
    d.polygon(((cx, cy - 5), (cx + 5, cy), (cx, cy + 5), (cx - 5, cy)), fill=dark)
    d.polygon(((cx, cy - 4), (cx + 4, cy), (cx, cy + 4), (cx - 4, cy)), fill=mid)
    d.polygon(((cx, cy - 2), (cx + 2, cy), (cx, cy + 2), (cx - 2, cy)), fill=bright)
    d.rectangle((cx - 1, cy - 1, cx + 1, cy + 1), fill=core)


def stripe(d, box, dark, light):
    x0, y0, x1, y1 = box
    d.rectangle(box, fill=dark)
    for x in range(x0 + 1, x1, 3):
        d.line((x, y0 + 1, x, y1 - 1), fill=light)


def make_cap():
    img, d = make_base('#06142a', '#0a2345', '#11365d', '#4ef3ff')
    navy, blue, cyan, pale = '#071b39', '#124779', '#21c7f3', '#c8ffff'
    panel(d, (0, 0, 31, 11), navy, blue, cyan, '#072147')
    panel(d, (0, 12, 31, 21), '#06182f', '#0e3b6b', '#59eaff')
    panel(d, (0, 22, 23, 29), '#06172e', '#12365d', '#35d9ff')
    stripe(d, (0, 30, 25, 35), '#092247', cyan)
    panel(d, (24, 0, 39, 11), '#06162d', '#0b3159', '#2faee0')
    panel(d, (40, 0, 55, 11), '#07182e', '#164266', '#55daff')
    panel(d, (24, 12, 39, 21), '#07172c', '#0d335d', '#2abce9')
    panel(d, (40, 12, 55, 21), '#07172c', '#0d335d', '#2abce9')
    panel(d, (32, 22, 39, 29), '#132034', '#425168', '#93a8bf')
    panel(d, (40, 22, 47, 29), '#132034', '#425168', '#93a8bf')
    panel(d, (48, 22, 63, 29), '#06162d', '#0c315c', '#23bce7')
    gem(d, 44, 41, '#07345b', '#087db4', cyan, pale)
    stripe(d, (0, 42, 20, 47), '#06172d', cyan)
    return img


def make_halo():
    img, d = make_base('#061527', '#092642', '#0d3a5c', '#82ffff')
    dark, blue, cyan, pale = '#073152', '#0b6c94', '#24d7ef', '#d2ffff'
    for y in (0, 8, 16):
        panel(d, (0, y, 23, y + 6), dark, blue, cyan)
        panel(d, (24, y, 47, y + 6), dark, blue, cyan)
    for x in (0, 8, 16, 24, 32, 40):
        panel(d, (x, 24, x + 7, 31), '#07213f', '#095875', '#36ddef')
    gem(d, 55, 7, '#083a59', '#0d83aa', cyan, pale)
    gem(d, 55, 22, '#083a59', '#0d83aa', cyan, pale)
    gem(d, 55, 38, '#083a59', '#0d83aa', cyan, pale)
    stripe(d, (0, 40, 31, 45), '#073052', cyan)
    return img


def make_crown():
    img, d = make_base('#14091f', '#211035', '#3e1e63', '#c76cff')
    black, obsidian, violet, pale = '#170a29', '#31134e', '#9b35e7', '#f2c7ff'
    panel(d, (0, 0, 31, 10), black, obsidian, violet)
    stripe(d, (0, 11, 31, 15), '#260d42', '#ce52ff')
    for x, h in ((0, 10), (7, 14), (15, 18), (23, 14), (30, 10)):
        panel(d, (x, 16, x + 5, 16 + h), '#190a2c', '#44206b', '#b44bff')
        d.rectangle((x + 2, 17, x + 3, 16 + h - 2), fill='#7024b7')
    panel(d, (36, 0, 43, 9), '#1a1b29', '#4d5167', '#a4a9bb')
    panel(d, (44, 0, 51, 9), '#1a1b29', '#4d5167', '#a4a9bb')
    gem(d, 57, 8, '#391367', '#8529c9', '#db72ff', pale)
    gem(d, 57, 22, '#391367', '#8529c9', '#db72ff', pale)
    stripe(d, (0, 38, 27, 43), '#260d42', '#b84fff')
    return img


def make_headphones():
    img, d = make_base('#07131c', '#102635', '#174252', '#65f5dd')
    graphite, metal, teal, pale = '#0b1c29', '#1b4151', '#28cdb8', '#c5fff5'
    panel(d, (0, 0, 31, 10), graphite, metal, teal)
    panel(d, (0, 11, 23, 17), '#0b1c29', '#173947', '#57e7d1')
    panel(d, (0, 18, 5, 31), '#0a2230', '#1c4a58', '#63f5da')
    panel(d, (6, 18, 11, 31), '#0a2230', '#1c4a58', '#63f5da')
    panel(d, (12, 18, 23, 31), '#071822', '#123747', '#35cdbd')
    panel(d, (24, 18, 35, 31), '#071822', '#123747', '#35cdbd')
    panel(d, (36, 18, 47, 31), '#071822', '#123747', '#35cdbd')
    gem(d, 53, 7, '#0a4d56', '#118c92', teal, pale)
    gem(d, 53, 22, '#0a4d56', '#118c92', teal, pale)
    stripe(d, (0, 38, 22, 43), '#0c2733', teal)
    d.rectangle((31, 39, 39, 43), fill='#132f3d')
    for y in (39, 41, 43):
        d.line((32, y, 38, y), fill='#70ffe3')
    return img


def make_antenna():
    img, d = make_base('#061d1b', '#0b322a', '#14594a', '#75ffd2')
    green, lime, pale = '#0b8b62', '#49df8d', '#ddffbc'
    panel(d, (0, 0, 31, 10), '#07513d', green, lime)
    panel(d, (0, 11, 31, 18), '#06412f', '#0b7555', '#5df8a1')
    panel(d, (0, 19, 8, 27), '#07382e', '#0a7955', '#55ee9b')
    panel(d, (9, 19, 17, 27), '#07382e', '#0a7955', '#55ee9b')
    for x in (18, 25, 32, 39):
        panel(d, (x, 19, x + 5, 27), '#063a2e', '#0a8b61', '#6cffb4')
    gem(d, 7, 38, '#0a6147', '#20ba68', '#80ff97', pale)
    gem(d, 21, 38, '#0a6147', '#20ba68', '#80ff97', pale)
    gem(d, 35, 38, '#0a6147', '#20ba68', '#80ff97', pale)
    panel(d, (46, 19, 55, 27), '#075039', '#0a9864', '#64ffc1')
    gem(d, 56, 39, '#075039', '#0a9864', '#64ffc1', '#e4ffff')
    stripe(d, (0, 48, 24, 53), '#07513d', '#63ffad')
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
    opaque = sum(1 for px in image.getdata() if px[3] == 255)
    print(f'{name}: {image.size}, opaque={opaque}/{W * H}')
