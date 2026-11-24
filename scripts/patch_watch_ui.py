#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把手环端 UI Lua + 关于页头像 blob 等长替换进模板 face 的 Lua 区。
  - tool.face: Lua 区 @3336784, 预算 16758B
  - cc.face:   Lua 区 @110896,  预算 10871B
布局（tool）：[Lua 代码][空格填充][--[==[ + 头像blob×2 + ]==]
头像紧贴文件末尾，关于页代码从 sz-556 读回两个 276B blob。
容器头/图标资源/预览块一个字节不动。幂等。
"""
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
UI_DIR = Path(__file__).resolve().parent / "watch_ui"
ASSETS_DIR = Path(__file__).resolve().parent / "about_assets"

TARGETS = [
    ("tool.face", 3336784, 16758),
    ("cc.face",   110896,  10871),
]


def patch(face_name: str, off: int, budget: int) -> None:
    path = ASSETS / face_name
    data = bytearray(path.read_bytes())
    orig_len = len(data)

    body = (UI_DIR / face_name.replace(".face", "_main.lua")).read_bytes()

    if face_name == "tool.face":
        b1 = (ASSETS_DIR / "av_dienuo.i4").read_bytes()
        b2 = (ASSETS_DIR / "av_qq.i4").read_bytes()
        assert len(b1) == 276 and len(b2) == 276
        assert b"]==]" not in b1 + b2
        tail = b"--[==[" + b1 + b2 + b"]==]"   # 6 + 552 + 4 = 562B
        # 读回偏移: blob 数据起点 = sz - len(b1+b2) - len("]==]") = sz - 556
        assert len(tail) - 6 == 556
    else:
        tail = b""

    total = len(body) + len(tail)
    if total > budget:
        raise SystemExit(f"{face_name}: Lua {len(body)}+blob {len(tail)} > {budget}")

    pad = b" " * (budget - total)
    new_text = body + pad + tail
    assert len(new_text) == budget
    data[off:off + budget] = new_text
    assert len(data) == orig_len
    path.write_bytes(bytes(data))
    print(f"{face_name}: 代码 {len(body)}B + 填充 {len(pad)}B + blob {len(tail)}B = {budget}B")

    if face_name == "tool.face":
        for base in (367, 1668579):
            assert data[base:base + 4] == b"FPK1"
        print("  FPK1 段完好")


def main():
    for name, off, budget in TARGETS:
        patch(name, off, budget)
    print("全部替换完成")


if __name__ == "__main__":
    main()
