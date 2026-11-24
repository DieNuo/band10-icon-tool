#!/usr/bin/env python3
"""生成手环端「关于」页开发者头像（LVGL v9 I4 索引色图片 blob，32×32）。

blob = 12B 头(0x19, cf=I4=0x09, flags=0, w=32, h=32, stride=16, rsv=0)
     + 16×4B 调色板(B,G,R,A 16级灰度) + 32×32 像素(半字节/像素, MSB first)
     总长 12+64+512 = 588B
"""
import struct
from PIL import Image, ImageDraw

W = H = 20
DIEFUO_SRC = "/tmp/rpk/v34/common/images/avatar_dienuo.png"
QQ_SRC = "/tmp/av1_qq.jpg"
OUT_DIR = "/workspace/band10-icon-tool/scripts/about_assets"


def load_32(src: str) -> Image.Image:
    im = Image.open(src).convert("RGBA")
    w, h = im.size
    scale = max(W / w, H / h)
    im = im.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    left = (im.width - W) // 2
    top = (im.height - H) // 2
    im = im.crop((left, top, left + W, top + H))
    bg = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    im = Image.alpha_composite(bg, im).convert("L")
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).ellipse((-1, -1, W, H), fill=255)
    im = Image.composite(im, Image.new("L", (W, H), 0), mask)
    return im


def to_blob(im: Image.Image) -> bytes:
    head = struct.pack("<BBHHHHH", 0x19, 0x09, 0, W, H, (W + 1) // 2, 0)
    assert len(head) == 12
    pal = b"".join(struct.pack("<BBBB", i * 17, i * 17, i * 17, 255) for i in range(16))
    px = bytearray()
    for y in range(H):
        row = [min(15, (im.getpixel((x, y)) + 8) // 17) for x in range(W)]
        for x in range(0, W, 2):
            px.append((row[x] << 4) | row[x + 1])
    blob = head + pal + bytes(px)
    assert len(blob) == 276, len(blob)
    return blob


def main():
    import os
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, src in (("av_dienuo", DIEFUO_SRC), ("av_qq", QQ_SRC)):
        blob = to_blob(load_32(src))
        # 直接存原始二进制；用 8 个 = 的长定界符包裹，确保 blob 内无 ]========]
        DELIM = b"]" + b"=" * 8 + b"]"
        assert DELIM not in blob, name
        with open(f"{OUT_DIR}/{name}.i4", "wb") as f:
            f.write(blob)
        print(name, len(blob), "bytes")
        prev = Image.new("L", (W, H))
        for y in range(H):
            for x in range(W):
                b = blob[12 + 64 + y * ((W + 1) // 2) + x // 2]
                prev.putpixel((x, y), min(255, ((b >> 4) if x % 2 == 0 else (b & 15)) * 17))
        prev.resize((192, 192), Image.NEAREST).save(f"{OUT_DIR}/{name}_preview.png")


if __name__ == "__main__":
    main()
