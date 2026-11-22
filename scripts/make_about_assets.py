#!/usr/bin/env python3
"""生成手环端「关于」页开发者头像（LVGL v9 I4 索引色图片 blob）。

布局规范（与 patch_about_face.py 约定一致）：
  blob = 12B 头(0x19, cf=I4=0x09, flags=0, w=48, h=48, stride=24, rsv=0)
       + 16×4B 调色板(B,G,R,A) + 48×48 像素(半字节/像素，MSB first, 行优先)
  总长 1228 B。
"""
import struct
from PIL import Image, ImageDraw, ImageOps

W = H = 48
AV = 1228

DIEFUO_SRC = "/tmp/rpk/v34/common/images/avatar_dienuo.png"
QQ_SRC = "/tmp/av1_qq.jpg"
OUT_DIR = "/workspace/band10-icon-tool/scripts/about_assets"


def load_48(src: str) -> Image.Image:
    """读图 → 48×48 灰度 → 圆形裁切（圆外纯黑）。"""
    im = Image.open(src).convert("RGBA")
    # 压到 48×48 短边 cover
    w, h = im.size
    scale = max(W / w, H / h)
    im = im.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    left = (im.width - W) // 2
    top = (im.height - H) // 2
    im = im.crop((left, top, left + W, top + H))
    # 灰度（在透明底上先铺黑，再按 alpha 混合）
    bg = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    im = Image.alpha_composite(bg, im).convert("L")
    # 圆形蒙版：圆外变纯黑
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, W - 1, H - 1), fill=255)
    black = Image.new("L", (W, H), 0)
    im = Image.composite(im, black, mask)
    return im


def to_blob(im: Image.Image) -> bytes:
    head = struct.pack("<BBBBHHHH", 0x19, 0x09, 0, W, H, (W * 4 + 7) // 8, 0, 0)
    # 字段序: magic(1B) cf(1B) flags(u16) w(u16) h(u16) stride(u16) rsv(u16) => 12B
    head = struct.pack("<BBHHHHH", 0x19, 0x09, 0, W, H, (W * 4 + 7) // 8, 0)
    assert len(head) == 12
    pal = b"".join(struct.pack("<BBBB", i * 17, i * 17, i * 17, 255) for i in range(16))
    px = bytearray()
    for y in range(H):
        row = [min(15, im.getpixel((x, y)) * 16 // 256 if im.getpixel((x, y)) < 256 else 15)
               for x in range(W)]
        # 量化：v//17 取整，255→15
        row = [min(15, (im.getpixel((x, y)) + 8) // 17) for x in range(W)]
        for x in range(0, W, 2):
            px.append((row[x] << 4) | row[x + 1])
    blob = head + pal + bytes(px)
    assert len(blob) == AV, len(blob)
    return blob


def main():
    import os
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, src in (("av_dienuo", DIEFUO_SRC), ("av_qq", QQ_SRC)):
        im = load_48(src)
        blob = to_blob(im)
        with open(f"{OUT_DIR}/{name}.i4", "wb") as f:
            f.write(blob)
        # 预览：按调色板还原放大
        prev = Image.new("L", (W, H))
        for y in range(H):
            for x in range(W):
                b = blob[12 + 64 + y * 24 + x // 2]
                idx = (b >> 4) if x % 2 == 0 else (b & 0x0F)
                prev.putpixel((x, y), min(255, idx * 17))
        prev.resize((192, 192), Image.NEAREST).save(f"{OUT_DIR}/{name}_preview.png")
        print(name, len(blob), "bytes; 序列含 ]] =", b"]]" in blob, "]=]" , b"]-]" in blob)


if __name__ == "__main__":
    main()
