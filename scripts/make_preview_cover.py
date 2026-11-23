#!/usr/bin/env python3
"""生成环10换装工具的表盘预览封面（切换表盘时的封面图）。

face 容器尾部有一块 234x270 ARGB8888 LVGL-RLE 预览图（原版内容是 fontswap
的蓝色圆圈），布局：
  +0  u32 0x400            (保留)
  +4  u16 w=234
  +6  u16 h=270
  +8  u32 lenfield=7738    (保留，前提是 RLE 数据等长 7730B)
  +12 u32 magic 0x5AA521E0 (保留)
  +16 u32 (uncompressed<<4)|blksize = 0x3DB304  (保留)
  +20 RLE 流: ctrl<0x80=重复run(ctrl次,1像素4B)；ctrl>=0x80=字面run(ctrl&0x7F像素)
  结尾 2B 终止符 00 00
整块 7752B。等长替换内容，文件其余字节零改动。

设计：应用图标风格——黑/棕渐变全幅延伸背景 + 四圆点（3白1橙）+ 「换装工具」。
"""
import struct, sys
from PIL import Image, ImageDraw, ImageFont

W, H = 234, 270
TARGET_DATA = 7730          # RLE 数据必须等长
BLK = 4

# ---- 取色（来自应用图标 720x720）----
ORANGE = (255, 122, 69, 255)     # #FF7A45
WHITE  = (255, 255, 255, 255)
TXT    = (168, 152, 142, 255)    # 暖灰

def build_image() -> Image.Image:
    im = Image.new("RGBA", (W, H))
    p = im.load()
    # 1) 垂直渐变：顶部近黑 -> 底部深棕（图标方块色 #20110E 方向延伸）
    top, bot = (13, 9, 8), (42, 26, 18)
    for y in range(H):
        t = y / (H - 1)
        c = tuple(round(top[i] + (bot[i] - top[i]) * t) for i in range(3)) + (255,)
        for x in range(W):
            p[x, y] = c
    d = ImageDraw.Draw(im)
    # 2) 四圆点 2x2（右上=橙，其余白），硬边无抗锯齿
    D = 54          # 直径
    cx1, cx2, cy1, cy2 = 77, 157, 88, 168
    for (cx, cy, col) in ((cx1, cy1, WHITE), (cx2, cy1, ORANGE),
                          (cx1, cy2, WHITE), (cx2, cy2, WHITE)):
        r = D / 2
        for yy in range(int(cy - r), int(cy - r) + D + 1):
            dx2 = r * r - (yy - cy) * (yy - cy)
            if dx2 <= 0:
                continue
            half = int(dx2 ** 0.5)
            d.line((cx - half, yy, cx + half, yy), fill=col)
    # 3) 文字「换装工具」：1-bit 硬边渲染
    try:
        f = ImageFont.truetype("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc", 24)
    except Exception:
        f = ImageFont.load_default()
    mask = Image.new("L", (W, H), 0)
    md = ImageDraw.Draw(mask)
    bb = md.textbbox((0, 0), "换装工具", font=f)
    tw, th = bb[2] - bb[0], bb[3] - bb[1]
    md.text(((W - tw) // 2 - bb[0], 224 - bb[1]), "换装工具", font=f, fill=255)
    mp = mask.load()
    for yy in range(H):
        for xx in range(W):
            if mp[xx, yy] >= 128:
                p[xx, yy] = TXT
    return im

def rle_encode(px: bytes) -> bytes:
    """LVGL RLE：重复 run(ctrl<0x80) / 字面 run(ctrl>=0x80)。"""
    out = bytearray()
    n = len(px) // BLK
    i = 0
    lits = bytearray()
    def flush_lit():
        nonlocal lits
        while lits:
            npix = min(127, len(lits) // BLK)
            chunk = lits[:npix * BLK]
            out.append(0x80 | npix)
            out.extend(chunk)
            lits = lits[npix * BLK:]
    while i < n:
        blk = px[i * BLK:(i + 1) * BLK]
        j = i
        while j < n and px[j * BLK:(j + 1) * BLK] == blk:
            j += 1
        run = j - i
        if run >= 4:
            flush_lit()
            while run > 0:
                c = min(run, 127)
                out.append(c); out.extend(blk)
                run -= c
        else:
            lits.extend(blk * run)
            if len(lits) >= 127 * BLK:
                flush_lit()
        i = j
    flush_lit()
    return bytes(out)

def pad_to(data: bytes, target: int) -> bytes:
    """把 RLE 流等像素扩到 target 字节：拆字面 run(+1B/次)、拆重复 run(+5B/次)。"""
    runs = []   # (kind, payload) kind: 'r'=repeat(count,color) 'l'=literal(pixels)
    i = 0
    while i < len(data):
        c = data[i]
        if c < 0x80:
            runs.append(['r', c, data[i + 1:i + 5]]); i += 5
        else:
            npx = c & 0x7F
            runs.append(['l', npx, data[i + 1:i + 1 + 4 * npx]]); i += 1 + 4 * npx
    size = lambda rs: sum(5 if r[0] == 'r' else 1 + 4 * r[1] for r in rs)
    while size(runs) < target:
        # 优先拆最长的字面 run（+1B）
        best = None
        for k, r in enumerate(runs):
            if r[0] == 'l' and r[1] >= 2:
                if best is None or r[1] > runs[best][1]:
                    best = k
        if best is not None:
            r = runs[best]
            half = r[1] // 2
            runs[best:best + 1] = [['l', half, r[2][:4 * half]],
                                   ['l', r[1] - half, r[2][4 * half:]]]
            continue
        # 拆重复 run（+5B）
        for k, r in enumerate(runs):
            if r[0] == 'r' and r[1] >= 2:
                runs[k:k + 1] = [['r', r[1] - 1, r[2]], ['r', 1, r[2]]]
                break
        else:
            raise SystemExit("无法再扩，设计颜色太少")
    out = bytearray()
    for r in runs:
        if r[0] == 'r':
            out.append(r[1]); out.extend(r[2])
        else:
            out.append(0x80 | r[1]); out.extend(r[2])
    assert len(out) == target, (len(out), target)
    return bytes(out)

def make_block() -> bytes:
    im = build_image()
    px = im.tobytes()                     # RGBA
    assert len(px) == W * H * 4
    enc = rle_encode(px)
    print(f"原始 RLE {len(enc)}B -> 目标 {TARGET_DATA}B")
    enc = pad_to(enc, TARGET_DATA)
    # 自校验：解码回读必须与原始像素逐字节一致
    back = bytearray()
    i = 0
    while i < len(enc):
        c = enc[i]
        if c == 0:
            break
        if c < 0x80:
            back.extend(enc[i + 1:i + 5] * c); i += 5
        else:
            n = c & 0x7F
            back.extend(enc[i + 1:i + 1 + 4 * n]); i += 1 + 4 * n
    assert bytes(back) == px, "RLE 回读不一致！"
    head = bytearray(20)
    struct.pack_into('<I', head, 0, 0x400)
    struct.pack_into('<HH', head, 4, W, H)
    struct.pack_into('<I', head, 8, TARGET_DATA + 8)   # 7738，与原版一致
    struct.pack_into('<I', head, 12, 0x5AA521E0)
    struct.pack_into('<I', head, 16, (W * H * 4 << 4) | BLK)
    return bytes(head) + enc + b'\x00\x00'

def patch(path: str):
    data = bytearray(open(path, 'rb').read())
    # 定位预览块：magic 0x5AA521E0 在 +12，块起点 = magic位置-12
    mi = bytes(data).find(struct.pack('<I', 0x5AA521E0))
    assert mi > 0, "找不到预览块"
    B = mi - 12
    w, h = struct.unpack('<HH', bytes(data)[B + 4:B + 8])
    assert (w, h) == (W, H), (w, h)
    old_len = struct.unpack('<I', bytes(data)[B + 8:B + 12])[0]
    blk = make_block()
    assert len(blk) == len(data) - B, (len(blk), len(data) - B)
    data[B:B + len(blk)] = blk
    open(path, 'wb').write(bytes(data))
    print(f"{path}: 预览块 @ {B} 已替换 (lenfield {old_len} -> {TARGET_DATA + 8}, 块长 {len(blk)}B 不变)")

if __name__ == '__main__':
    blk = make_block()
    # 保存设计稿预览
    build_image().resize((W * 2, H * 2), Image.NEAREST).save('/tmp/cover_design_big.png')
    build_image().save('/workspace/新表盘封面预览_234x270.png')
    for p in sys.argv[1:]:
        patch(p)
