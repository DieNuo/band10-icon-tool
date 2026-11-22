#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把手环端新 UI Lua（官方深色风格 + 建删副本一体化）等长替换进 App 资产里的模板：
  - tool.face（换装工具，16758B Lua 区 @3336784）
  - cc.face （控制中心，10871B Lua 区 @110896）
容器头/图标资源段一个字节不动；App 侧生成流程无需任何改动——生成的每个
.face 自带新界面与建/删副本功能。可重复运行（幂等）。
"""
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
UI_DIR = Path(__file__).resolve().parent / "watch_ui"

TARGETS = [
    # (face 文件, Lua 区偏移, Lua 区长度)
    ("tool.face", 3336784, 16758),
    ("cc.face",   110896,  10871),
]


def patch(face_name: str, off: int, budget: int) -> None:
    path = ASSETS / face_name
    data = bytearray(path.read_bytes())
    orig_len = len(data)

    lua_path = UI_DIR / (face_name.replace(".face", "_main.lua"))
    body = lua_path.read_bytes()
    if len(body) > budget:
        raise SystemExit(f"{face_name}: 新 Lua 超长 {len(body)} > {budget}")
    pad = budget - len(body)
    new_text = body + b" " * pad
    data[off:off + budget] = new_text

    assert len(data) == orig_len, (face_name, len(data), orig_len)
    path.write_bytes(bytes(data))
    print(f"{face_name}: Lua {len(body)}B + 填充 {pad}B = {budget}B 已替换")

    # 结构自检：FPK1 段应保持完好
    for base in (367, 1668579) if face_name == "tool.face" else ():
        if base < len(data) and data[base:base+4] == b"FPK1":
            cnt = struct.unpack_from("<I", data, base+4)[0]
            print(f"  FPK1@{base} 条目数 {cnt} OK")
        else:
            raise SystemExit(f"{face_name}: FPK1@{base} 自检失败")


def main():
    for name, off, budget in TARGETS:
        patch(name, off, budget)
    print("全部替换完成，可重新构建 APK")


if __name__ == "__main__":
    main()
